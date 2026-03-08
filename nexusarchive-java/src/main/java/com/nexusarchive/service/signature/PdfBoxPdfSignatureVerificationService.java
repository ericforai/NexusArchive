// Input: BouncyCastle、Lombok、PDFBox、Spring Framework、Java 标准库、本地模块
// Output: PdfBoxPdfSignatureVerificationService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.signature;

import com.nexusarchive.dto.signature.PdfSignatureVerificationResult;
import com.nexusarchive.dto.signature.PdfSignatureVerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 基于 PDFBox + BouncyCastle 的 PDF 签名验证服务
 */
@Slf4j
@Service
public class PdfBoxPdfSignatureVerificationService implements PdfSignatureVerificationService {

    private static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    static {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public PdfSignatureVerificationResult verify(InputStream pdfStream) {
        if (pdfStream == null) {
            return PdfSignatureVerificationResult.unknown(null, 0, "PDF 输入流为空");
        }

        try {
            return verify(pdfStream.readAllBytes());
        } catch (IOException e) {
            log.warn("读取 PDF 输入流失败: {}", e.getMessage());
            return PdfSignatureVerificationResult.unknown(null, 0, "PDF 读取失败: " + safeMessage(e));
        }
    }

    @Override
    public PdfSignatureVerificationResult verify(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return PdfSignatureVerificationResult.unknown(null, 0, "PDF 内容为空");
        }

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            if (signatures == null || signatures.isEmpty()) {
                return PdfSignatureVerificationResult.unknown(Boolean.FALSE, 0, "未检测到 PDF 数字签名");
            }

            SignatureInspection firstValid = null;
            SignatureInspection firstUnknown = null;
            int latestCoveredLength = signatures.stream()
                    .map(PDSignature::getByteRange)
                    .filter(byteRange -> byteRange != null && byteRange.length >= 4)
                    .mapToInt(this::coveredLength)
                    .max()
                    .orElse(pdfBytes.length);
            for (PDSignature signature : signatures) {
                boolean requiresWholeDocumentCoverage = coveredLength(signature.getByteRange()) == latestCoveredLength;
                SignatureInspection inspection = inspect(signature, pdfBytes, requiresWholeDocumentCoverage);
                if (inspection.status() == PdfSignatureVerificationStatus.INVALID) {
                    return toInvalidResult(inspection, signatures.size());
                }
                if (inspection.status() == PdfSignatureVerificationStatus.UNKNOWN && firstUnknown == null) {
                    firstUnknown = inspection;
                }
                if (inspection.status() == PdfSignatureVerificationStatus.VALID && firstValid == null) {
                    firstValid = inspection;
                }
            }

            if (firstUnknown != null) {
                return toUnknownResult(firstUnknown, signatures.size());
            }

            if (firstValid != null) {
                return toValidResult(firstValid, signatures.size());
            }

            return PdfSignatureVerificationResult.unknown(Boolean.TRUE, signatures.size(), "PDF 包含签名，但无法得出稳定结论");
        } catch (IOException e) {
            log.info("解析 PDF 失败: {}", e.getMessage());
            return PdfSignatureVerificationResult.unknown(null, 0, "PDF 解析失败: " + safeMessage(e));
        }
    }

    private SignatureInspection inspect(PDSignature signature, byte[] pdfBytes, boolean requiresWholeDocumentCoverage) {
        LocalDateTime signTime = toLocalDateTime(signature.getSignDate());
        String fallbackSignerName = signature.getName();
        String fallbackAlgorithm = signature.getSubFilter();

        int[] byteRange = signature.getByteRange();
        if (byteRange == null || byteRange.length < 4) {
            return SignatureInspection.unknown(
                    "PDF 签名缺少有效的 ByteRange",
                    fallbackSignerName,
                    null,
                    null,
                    fallbackAlgorithm,
                    signTime);
        }

        if (requiresWholeDocumentCoverage && !coversWholeDocument(byteRange, pdfBytes.length)) {
            return SignatureInspection.invalid(
                    "PDF 签名未覆盖整个文档",
                    fallbackSignerName,
                    null,
                    null,
                    fallbackAlgorithm,
                    signTime);
        }

        byte[] signatureContents;
        byte[] signedContent;
        try {
            signatureContents = signature.getContents(pdfBytes);
            signedContent = signature.getSignedContent(pdfBytes);
        } catch (IOException e) {
            return SignatureInspection.unknown(
                    "读取 PDF 签名内容失败: " + safeMessage(e),
                    fallbackSignerName,
                    null,
                    null,
                    fallbackAlgorithm,
                    signTime);
        }

        if (signatureContents == null || signatureContents.length == 0
                || signedContent == null || signedContent.length == 0) {
            return SignatureInspection.unknown(
                    "PDF 包含签名字段，但缺少可验证内容",
                    fallbackSignerName,
                    null,
                    null,
                    fallbackAlgorithm,
                    signTime);
        }

        try {
            CMSSignedData cmsSignedData = new CMSSignedData(
                    new CMSProcessableByteArray(signedContent),
                    signatureContents);
            SignerInformationStore signerStore = cmsSignedData.getSignerInfos();
            Collection<SignerInformation> signers = signerStore.getSigners();
            if (signers.isEmpty()) {
                return SignatureInspection.unknown(
                        "PDF 签名中未发现签名者信息",
                        fallbackSignerName,
                        null,
                        null,
                        fallbackAlgorithm,
                        signTime);
            }

            Store<X509CertificateHolder> certificateStore = cmsSignedData.getCertificates();
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider(PROVIDER);

            SignatureInspection firstValid = null;
            for (SignerInformation signer : signers) {
                Collection<X509CertificateHolder> matches = certificateStore.getMatches(signer.getSID());
                if (matches.isEmpty()) {
                    return SignatureInspection.unknown(
                            "PDF 签名缺少匹配的签名证书",
                            fallbackSignerName,
                            null,
                            null,
                            fallbackAlgorithm,
                            signTime);
                }

                X509Certificate certificate = converter.getCertificate(matches.iterator().next());
                Date certificateValidationTime = signTime != null
                        ? toDate(signTime)
                        : new Date();
                try {
                    certificate.checkValidity(certificateValidationTime);
                } catch (CertificateExpiredException e) {
                    return SignatureInspection.invalid(
                            "签名证书已过期",
                            extractSignerName(certificate, fallbackSignerName),
                            certificate.getSubjectX500Principal().getName(),
                            certificate.getSerialNumber().toString(16),
                            certificate.getSigAlgName(),
                            signTime);
                } catch (CertificateNotYetValidException e) {
                    return SignatureInspection.invalid(
                            "签名证书尚未生效",
                            extractSignerName(certificate, fallbackSignerName),
                            certificate.getSubjectX500Principal().getName(),
                            certificate.getSerialNumber().toString(16),
                            certificate.getSigAlgName(),
                            signTime);
                }

                boolean verified = signer.verify(
                        new JcaSimpleSignerInfoVerifierBuilder()
                                .setProvider(PROVIDER)
                                .build(certificate));
                if (!verified) {
                    return SignatureInspection.invalid(
                            "PDF 签名校验失败",
                            extractSignerName(certificate, fallbackSignerName),
                            certificate.getSubjectX500Principal().getName(),
                            certificate.getSerialNumber().toString(16),
                            certificate.getSigAlgName(),
                            signTime);
                }

                if (firstValid == null) {
                    firstValid = SignatureInspection.valid(
                            "PDF 签名验证通过",
                            extractSignerName(certificate, fallbackSignerName),
                            certificate.getSubjectX500Principal().getName(),
                            certificate.getSerialNumber().toString(16),
                            certificate.getSigAlgName(),
                            signTime);
                }
            }

            return firstValid != null
                    ? firstValid
                    : SignatureInspection.unknown(
                    "PDF 包含签名，但无法完成验证",
                    fallbackSignerName,
                    null,
                    null,
                    fallbackAlgorithm,
                    signTime);
        } catch (CMSException | OperatorCreationException | CertificateException e) {
            return SignatureInspection.unknown(
                    "PDF 签名解析失败: " + safeMessage(e),
                    fallbackSignerName,
                    null,
                    null,
                    fallbackAlgorithm,
                    signTime);
        }
    }

    private int coveredLength(int[] byteRange) {
        if (byteRange == null || byteRange.length < 4) {
            return -1;
        }
        long coveredLength = (long) byteRange[2] + byteRange[3];
        if (coveredLength > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) coveredLength;
    }

    private boolean coversWholeDocument(int[] byteRange, int pdfLength) {
        long firstOffset = byteRange[0];
        long firstLength = byteRange[1];
        long secondOffset = byteRange[2];
        long secondLength = byteRange[3];

        if (firstOffset != 0 || firstLength < 0 || secondOffset < 0 || secondLength < 0) {
            return false;
        }

        if (firstOffset + firstLength > secondOffset) {
            return false;
        }

        return secondOffset + secondLength == pdfLength;
    }

    private PdfSignatureVerificationResult toValidResult(SignatureInspection inspection, int signatureCount) {
        return PdfSignatureVerificationResult.valid(
                signatureCount,
                inspection.message(),
                inspection.signerName(),
                inspection.certificateSubject(),
                inspection.certSerialNumber(),
                inspection.algorithm(),
                inspection.signTime());
    }

    private PdfSignatureVerificationResult toInvalidResult(SignatureInspection inspection, int signatureCount) {
        return PdfSignatureVerificationResult.invalid(
                signatureCount,
                inspection.message(),
                inspection.signerName(),
                inspection.certificateSubject(),
                inspection.certSerialNumber(),
                inspection.algorithm(),
                inspection.signTime());
    }

    private PdfSignatureVerificationResult toUnknownResult(SignatureInspection inspection, int signatureCount) {
        return PdfSignatureVerificationResult.builder()
                .status(PdfSignatureVerificationStatus.UNKNOWN)
                .signed(Boolean.TRUE)
                .message(inspection.message())
                .signerName(inspection.signerName())
                .certificateSubject(inspection.certificateSubject())
                .certSerialNumber(inspection.certSerialNumber())
                .algorithm(inspection.algorithm())
                .signTime(inspection.signTime())
                .verifiedAt(LocalDateTime.now())
                .signatureCount(signatureCount)
                .build();
    }

    private LocalDateTime toLocalDateTime(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        return LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String extractSignerName(X509Certificate certificate, String fallback) {
        if (certificate == null) {
            return fallback;
        }

        String subject = certificate.getSubjectX500Principal().getName();
        for (String part : subject.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }
        return fallback != null ? fallback : subject;
    }

    private String safeMessage(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }

    private record SignatureInspection(
            PdfSignatureVerificationStatus status,
            String message,
            String signerName,
            String certificateSubject,
            String certSerialNumber,
            String algorithm,
            LocalDateTime signTime) {

        private static SignatureInspection valid(
                String message,
                String signerName,
                String certificateSubject,
                String certSerialNumber,
                String algorithm,
                LocalDateTime signTime) {
            return new SignatureInspection(
                    PdfSignatureVerificationStatus.VALID,
                    message,
                    signerName,
                    certificateSubject,
                    certSerialNumber,
                    algorithm,
                    signTime);
        }

        private static SignatureInspection invalid(
                String message,
                String signerName,
                String certificateSubject,
                String certSerialNumber,
                String algorithm,
                LocalDateTime signTime) {
            return new SignatureInspection(
                    PdfSignatureVerificationStatus.INVALID,
                    message,
                    signerName,
                    certificateSubject,
                    certSerialNumber,
                    algorithm,
                    signTime);
        }

        private static SignatureInspection unknown(
                String message,
                String signerName,
                String certificateSubject,
                String certSerialNumber,
                String algorithm,
                LocalDateTime signTime) {
            return new SignatureInspection(
                    PdfSignatureVerificationStatus.UNKNOWN,
                    message,
                    signerName,
                    certificateSubject,
                    certSerialNumber,
                    algorithm,
                    signTime);
        }
    }
}
