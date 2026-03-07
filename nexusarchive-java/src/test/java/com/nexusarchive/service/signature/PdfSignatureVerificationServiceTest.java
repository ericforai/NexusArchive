// Input: AssertJ、BouncyCastle、JUnit 5、PDFBox、Java 标准库
// Output: PdfSignatureVerificationServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.signature;

import com.nexusarchive.dto.signature.PdfSignatureVerificationStatus;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class PdfSignatureVerificationServiceTest {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final PdfSignatureVerificationService service = new PdfBoxPdfSignatureVerificationService();

    @Test
    void verify_unsigned_pdf_returns_unknown() throws Exception {
        byte[] pdfBytes = createUnsignedPdf("unsigned-pdf");

        var result = service.verify(pdfBytes);

        assertThat(result.getStatus()).isEqualTo(PdfSignatureVerificationStatus.UNKNOWN);
        assertThat(result.getSigned()).isFalse();
        assertThat(result.getSignatureCount()).isZero();
        assertThat(result.getMessage()).contains("未检测到");
    }

    @Test
    void verify_malformed_pdf_returns_unknown() {
        byte[] malformedBytes = "not-a-valid-pdf".getBytes(StandardCharsets.UTF_8);

        var result = service.verify(malformedBytes);

        assertThat(result.getStatus()).isEqualTo(PdfSignatureVerificationStatus.UNKNOWN);
        assertThat(result.getSigned()).isNull();
        assertThat(result.getMessage()).contains("解析");
    }

    @Test
    void verify_signed_pdf_returns_valid() throws Exception {
        SignedPdfFixture fixture = createSignedPdf("valid-signed-pdf");

        var result = service.verify(fixture.pdfBytes());

        assertThat(result.getStatus()).isEqualTo(PdfSignatureVerificationStatus.VALID);
        assertThat(result.getSigned()).isTrue();
        assertThat(result.getSignatureCount()).isEqualTo(1);
        assertThat(result.getSignerName()).contains("Test Signer");
        assertThat(result.getCertificateSubject()).contains("CN=Test Signer");
        assertThat(result.getCertSerialNumber()).isNotBlank();
    }

    @Test
    void verify_signed_pdf_uses_sign_time_for_certificate_validity() throws Exception {
        Calendar signDate = Calendar.getInstance();
        signDate.add(Calendar.MINUTE, -10);
        Date notBefore = new Date(signDate.getTimeInMillis() - 60_000L);
        Date notAfter = new Date(signDate.getTimeInMillis() + 60_000L);
        SignedPdfFixture fixture = createSignedPdf(
                "expired-after-sign-time",
                signDate,
                notBefore,
                notAfter);

        var result = service.verify(fixture.pdfBytes());

        assertThat(result.getStatus()).isEqualTo(PdfSignatureVerificationStatus.VALID);
        assertThat(result.getSigned()).isTrue();
        assertThat(result.getSignatureCount()).isEqualTo(1);
        assertThat(result.getSignerName()).contains("Test Signer");
    }

    @Test
    void verify_tampered_signed_pdf_returns_invalid() throws Exception {
        SignedPdfFixture fixture = createSignedPdf("tampered-signed-pdf");
        byte[] tamperedBytes = appendTrailingBytes(fixture.pdfBytes(), "\n%tampered-after-signature\n");

        var result = service.verify(tamperedBytes);

        assertThat(result.getStatus()).isEqualTo(PdfSignatureVerificationStatus.INVALID);
        assertThat(result.getSigned()).isTrue();
        assertThat(result.getSignatureCount()).isEqualTo(1);
        assertThat(result.getMessage()).contains("覆盖");
    }

    private byte[] createUnsignedPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private SignedPdfFixture createSignedPdf(String text) throws Exception {
        Calendar signDate = Calendar.getInstance();
        Date notBefore = new Date(System.currentTimeMillis() - 60_000L);
        Date notAfter = new Date(System.currentTimeMillis() + 86_400_000L);
        return createSignedPdf(text, signDate, notBefore, notAfter);
    }

    private SignedPdfFixture createSignedPdf(String text, Calendar signDate, Date notBefore, Date notAfter)
            throws Exception {
        KeyPair keyPair = generateKeyPair();
        X509Certificate certificate = generateCertificate(keyPair, notBefore, notAfter);
        byte[] unsignedPdfBytes = createUnsignedPdf(text);

        try (PDDocument document = PDDocument.load(unsignedPdfBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Test Signer");
            signature.setReason("test-signature");
            signature.setSignDate(signDate);

            document.addSignature(signature,
                    content -> signCms(content, keyPair.getPrivate(), certificate, signDate.getTime()));
            document.saveIncremental(outputStream);
            return new SignedPdfFixture(outputStream.toByteArray(), certificate);
        }
    }

    private KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private X509Certificate generateCertificate(KeyPair keyPair, Date notBefore, Date notAfter) throws Exception {
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                new javax.security.auth.x500.X500Principal("CN=Test Signer, O=NexusArchive"),
                serialNumber,
                notBefore,
                notAfter,
                new javax.security.auth.x500.X500Principal("CN=Test Signer, O=NexusArchive"),
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyPair.getPrivate());

        X509CertificateHolder certificateHolder = certificateBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateHolder);
    }

    private byte[] signCms(
            InputStream content,
            PrivateKey privateKey,
            X509Certificate certificate,
            Date signingTime) throws IOException {
        try {
            byte[] signedContent = content.readAllBytes();
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addCertificates(new JcaCertStore(List.of(certificate)));
            JcaSignerInfoGeneratorBuilder signerInfoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build());
            signerInfoGeneratorBuilder.setSignedAttributeGenerator(
                    new DefaultSignedAttributeTableGenerator(createSignedAttributes(signingTime)));
            generator.addSignerInfoGenerator(signerInfoGeneratorBuilder.build(
                    new JcaContentSignerBuilder("SHA256withRSA")
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build(privateKey),
                    certificate));

            CMSSignedData signedData = generator.generate(new CMSProcessableByteArray(signedContent), false);
            return signedData.getEncoded();
        } catch (Exception e) {
            throw new IOException("生成 CMS 签名失败", e);
        }
    }

    private AttributeTable createSignedAttributes(Date signingTime) {
        ASN1EncodableVector signedAttributes = new ASN1EncodableVector();
        signedAttributes.add(new Attribute(
                CMSAttributes.signingTime,
                new DERSet(new Time(signingTime))));
        return new AttributeTable(signedAttributes);
    }

    private byte[] appendTrailingBytes(byte[] source, String suffix) {
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[source.length + suffixBytes.length];
        System.arraycopy(source, 0, result, 0, source.length);
        System.arraycopy(suffixBytes, 0, result, source.length, suffixBytes.length);
        return result;
    }

    private record SignedPdfFixture(byte[] pdfBytes, X509Certificate certificate) {
    }
}
