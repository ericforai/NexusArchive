// Input: PDF 签名文件（CMS + 时间戳/证书链/信任根）
// Output: 数字签名验证实现
// Pos: NexusCore compliance/signature
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 数字签名验证实现
 * 
 * 支持 PDF 文件的 CMS/PKCS7 签名验证
 */
@Service
public class DefaultDigitalSignatureVerifier implements DigitalSignatureVerifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDigitalSignatureVerifier.class);
    private static final String OID_SM2 = "1.2.156.10197.1.301";
    private static final String OID_SM3 = "1.2.156.10197.1.401";
    private static final String OID_RSA = "1.2.840.113549.1.1.1";
    private static final String OID_SHA256 = "2.16.840.1.101.3.4.2.1";
    private static final ASN1ObjectIdentifier OID_TSA_TIMESTAMP_TOKEN =
            new ASN1ObjectIdentifier("1.2.840.113549.1.9.16.2.14");
    private static final String KEYSTORE_TYPE = "PKCS12";

    private String truststorePathConfig;
    private String truststorePasswordConfig;
    private Boolean strictModeConfig;
    private volatile TrustStoreBundle trustStoreBundle;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${signature.truststore.path:}")
    void setTruststorePathConfig(String truststorePathConfig) {
        this.truststorePathConfig = truststorePathConfig;
    }

    @Value("${signature.truststore.password:}")
    void setTruststorePasswordConfig(String truststorePasswordConfig) {
        this.truststorePasswordConfig = truststorePasswordConfig;
    }

    @Value("${compliance.strict-mode:true}")
    void setStrictModeConfig(boolean strictModeConfig) {
        this.strictModeConfig = strictModeConfig;
    }

    // [P2-FIX] CRL/OCSP 吐销检查配置
    private Boolean revocationCheckEnabled;

    @Value("${signature.revocation.enabled:false}")
    void setRevocationCheckEnabled(boolean enabled) {
        this.revocationCheckEnabled = enabled;
    }

    @Override
    public SignatureVerifyResult verify(Path filePath) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        
        String fileName = filePath.getFileName().toString().toLowerCase();
        try {
            if (fileName.endsWith(".ofd")) {
                return verifyOfdSignature(filePath);
            } else if (fileName.endsWith(".pdf")) {
                return verifyPdfSignature(filePath);
            }
            return SignatureVerifyResult.failure("不支持的文件格式: " + fileName);
        } catch (Exception ex) {
            LOGGER.error("签名验证失败: {}", filePath, ex);
            return SignatureVerifyResult.failure("签名验证异常: " + ex.getMessage());
        }
    }

    private SignatureVerifyResult verifyOfdSignature(Path ofdPath) {
        return SignatureVerifyResult.failure("OFD 验签暂停（当前仅支持 PDF）");
    }

    private SignatureVerifyResult verifyPdfSignature(Path pdfPath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            List<PDSignature> signatures = doc.getSignatureDictionaries();
            
            if (signatures.isEmpty()) {
                return SignatureVerifyResult.noSignature();
            }

            SignatureVerifyResult firstValid = null;

            for (PDSignature signature : signatures) {
                SignatureVerifyResult result = verifyPdfSignatureEntry(pdfPath, signature);
                if (!result.valid()) {
                    return result;
                }
                if (firstValid == null) {
                    firstValid = result;
                }
            }

            return firstValid == null ? SignatureVerifyResult.failure("签名校验失败") : firstValid;
        }
    }

    private SignatureVerifyResult verifyPdfSignatureEntry(Path pdfPath, PDSignature signature) throws IOException {
        byte[] signatureBytes = readSignatureContents(pdfPath, signature);
        byte[] signedContent = readSignedContent(pdfPath, signature);

        if (signatureBytes.length == 0 || signedContent.length == 0) {
            return SignatureVerifyResult.failure("签名内容缺失");
        }

        boolean strictMode = resolveStrictMode();
        TrustStoreBundle trustStoreBundle = resolveTrustStore(strictMode);

        try {
            CMSSignedData signedData = new CMSSignedData(
                    new CMSProcessableByteArray(signedContent),
                    signatureBytes);

            Store<X509CertificateHolder> certStore = signedData.getCertificates();
            SignerInformationStore signerStore = signedData.getSignerInfos();
            Collection<SignerInformation> signers = signerStore.getSigners();

            if (signers.isEmpty()) {
                return SignatureVerifyResult.failure("签名人为空");
            }

            SignerInformation signer = signers.iterator().next();
            X509Certificate certificate = resolveCertificate(certStore, signer.getSID());

            TimestampValidationResult timestampResult = validateTimestampToken(
                    signer, trustStoreBundle, strictMode);
            if (strictMode && !timestampResult.valid()) {
                return SignatureVerifyResult.failureWithTimestamp(
                        "时间戳验证失败: " + timestampResult.message(),
                        true,
                        false,
                        timestampResult.present(),
                        timestampResult.message());
            }

            LocalDateTime signTime = timestampResult.timestampTime() != null
                    ? timestampResult.timestampTime()
                    : resolveSigningTime(signature, signer);
            if (signTime == null) {
                if (strictMode) {
                    return SignatureVerifyResult.failureWithTimestamp(
                            "签名时间戳缺失",
                            true,
                            timestampResult.valid(),
                            timestampResult.present(),
                            timestampResult.message());
                }
                LOGGER.warn("签名时间戳缺失，使用当前时间校验证书有效期");
                signTime = LocalDateTime.now();
            }

            certificate.checkValidity(Date.from(signTime.atZone(ZoneId.systemDefault()).toInstant()));
            boolean chainValid = validateCertificateChain(certStore, certificate, trustStoreBundle, strictMode);
            if (strictMode && !chainValid) {
                return SignatureVerifyResult.failureWithTimestamp(
                        "证书链校验失败",
                        true,
                        timestampResult.valid(),
                        timestampResult.present(),
                        timestampResult.message());
            }
            if (!chainValid) {
                LOGGER.warn("证书链校验失败，进入非严格模式");
            }

            boolean verified = signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider("BC")
                    .build(certificate));

            if (!verified) {
                return SignatureVerifyResult.failureWithTimestamp(
                        "签名验证失败",
                        true,
                        timestampResult.valid(),
                        timestampResult.present(),
                        timestampResult.message());
            }

            String algorithm = resolveAlgorithm(signature, signer);
            String signerName = resolveSignerName(certificate);
            String certSerialNo = certificate.getSerialNumber().toString(16).toUpperCase();

            LOGGER.info("PDF 签名验证通过: signer={}, algorithm={}, serial={}",
                    signerName, algorithm, certSerialNo);

            return SignatureVerifyResult.successWithTimestamp(
                    algorithm,
                    signerName,
                    signTime,
                    certSerialNo,
                    timestampResult.valid(),
                    timestampResult.present(),
                    timestampResult.message());
        } catch (CertificateExpiredException ex) {
            return SignatureVerifyResult.failure("证书过期");
        } catch (CertificateNotYetValidException ex) {
            return SignatureVerifyResult.failure("证书未生效");
        } catch (Exception ex) {
            LOGGER.error("PDF 签名验证异常: {}", pdfPath.getFileName(), ex);
            return SignatureVerifyResult.failure("签名验证异常: " + ex.getMessage());
        }
    }

    private byte[] readSignatureContents(Path pdfPath, PDSignature signature) throws IOException {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(pdfPath)) {
            return signature.getContents(inputStream);
        }
    }

    private byte[] readSignedContent(Path pdfPath, PDSignature signature) throws IOException {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(pdfPath)) {
            return signature.getSignedContent(inputStream);
        }
    }

    private boolean resolveStrictMode() {
        return strictModeConfig == null || strictModeConfig;
    }

    private TrustStoreBundle resolveTrustStore(boolean strictMode) {
        String truststorePath = truststorePathConfig;
        if (truststorePath == null || truststorePath.isBlank()) {
            if (strictMode) {
                throw new IllegalStateException("信任根未配置");
            }
            LOGGER.warn("信任根未配置，进入非严格模式");
            return null;
        }

        String password = truststorePasswordConfig;
        TrustStoreBundle cached = trustStoreBundle;
        if (cached != null && cached.matches(truststorePath, password)) {
            return cached;
        }

        TrustStoreBundle loaded = loadTrustStore(Path.of(truststorePath), password);
        trustStoreBundle = loaded;
        return loaded;
    }

    private TrustStoreBundle loadTrustStore(Path truststorePath, String password) {
        if (!Files.exists(truststorePath)) {
            throw new IllegalStateException("信任根文件不存在: " + truststorePath);
        }
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            char[] pwd = (password == null || password.isBlank()) ? null : password.toCharArray();
            try (InputStream inputStream = Files.newInputStream(truststorePath)) {
                keyStore.load(inputStream, pwd);
            }

            List<X509Certificate> certificates = new ArrayList<>();
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate x509) {
                    certificates.add(x509);
                }
            }

            Set<TrustAnchor> anchors = certificates.stream()
                    .map(cert -> new TrustAnchor(cert, null))
                    .collect(Collectors.toSet());

            if (anchors.isEmpty()) {
                throw new IllegalStateException("信任根为空: " + truststorePath);
            }

            return new TrustStoreBundle(truststorePath.toString(), password, keyStore, certificates, anchors);
        } catch (Exception ex) {
            throw new IllegalStateException("加载信任根失败: " + ex.getMessage(), ex);
        }
    }

    private X509Certificate resolveCertificate(Store<X509CertificateHolder> certStore,
                                               SignerId signerId) throws Exception {
        Collection<X509CertificateHolder> matches = certStore.getMatches(signerId);
        if (matches.isEmpty()) {
            throw new IllegalStateException("签名证书不存在");
        }
        X509CertificateHolder holder = matches.iterator().next();
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
    }

    private LocalDateTime resolveSigningTime(PDSignature signature, SignerInformation signer) {
        Date signingTime = resolveCmsSigningTime(signer);
        if (signingTime != null) {
            return LocalDateTime.ofInstant(signingTime.toInstant(), ZoneId.systemDefault());
        }
        Calendar signDate = signature.getSignDate();
        if (signDate == null) {
            return null;
        }
        return LocalDateTime.ofInstant(signDate.toInstant(), ZoneId.systemDefault());
    }

    private String resolveAlgorithm(PDSignature signature, SignerInformation signer) {
        String encryptionOid = String.valueOf(signer.getEncryptionAlgOID());
        String digestOid = String.valueOf(signer.getDigestAlgOID());
        if (OID_SM2.equals(encryptionOid) || OID_SM3.equals(digestOid)) {
            return "SM2-SM3";
        }
        if (OID_RSA.equals(encryptionOid)) {
            return OID_SHA256.equals(digestOid) ? "RSA-SHA256" : "RSA";
        }
        String subFilter = signature.getSubFilter();
        if (subFilter != null) {
            return subFilter;
        }
        return encryptionOid;
    }

    private String resolveSignerName(X509Certificate certificate) {
        try {
            LdapName ldapName = new LdapName(certificate.getSubjectX500Principal().getName());
            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return String.valueOf(rdn.getValue());
                }
            }
        } catch (Exception ex) {
            LOGGER.debug("解析签名人失败", ex);
        }
        return certificate.getSubjectX500Principal().getName();
    }

    private Date resolveCmsSigningTime(SignerInformation signer) {
        AttributeTable attrs = signer.getSignedAttributes();
        if (attrs == null) {
            return null;
        }
        Attribute attr = attrs.get(CMSAttributes.signingTime);
        if (attr == null || attr.getAttrValues().size() == 0) {
            return null;
        }
        ASN1Encodable value = attr.getAttrValues().getObjectAt(0);
        Time time = Time.getInstance(value);
        return time.getDate();
    }

    private TimestampValidationResult validateTimestampToken(SignerInformation signer,
                                                             TrustStoreBundle trustStoreBundle,
                                                             boolean strictMode) {
        AttributeTable unsignedAttributes = signer.getUnsignedAttributes();
        if (unsignedAttributes == null) {
            return TimestampValidationResult.missing("未包含 TSA 时间戳");
        }
        Attribute timestampAttr = unsignedAttributes.get(OID_TSA_TIMESTAMP_TOKEN);
        if (timestampAttr == null || timestampAttr.getAttrValues().size() == 0) {
            return TimestampValidationResult.missing("未包含 TSA 时间戳");
        }
        ASN1Encodable value = timestampAttr.getAttrValues().getObjectAt(0);
        try {
            CMSSignedData timestampData = new CMSSignedData(value.toASN1Primitive().getEncoded());
            TimeStampToken token = new TimeStampToken(timestampData);
            TimeStampTokenInfo info = token.getTimeStampInfo();

            X509Certificate tsaCertificate = resolveCertificate(token.getCertificates(), token.getSID());
            token.validate(new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider("BC")
                    .build(tsaCertificate));

            boolean tsaChainValid = validateCertificateChain(token.getCertificates(), tsaCertificate,
                    trustStoreBundle, strictMode);
            if (!tsaChainValid) {
                if (!strictMode) {
                    LOGGER.warn("TSA 证书链校验失败，进入非严格模式");
                }
                return TimestampValidationResult.invalid("TSA 证书链校验失败");
            }

            if (!verifyMessageImprint(info, signer.getSignature())) {
                return TimestampValidationResult.invalid("时间戳摘要不匹配");
            }

            Date genTime = info.getGenTime();
            tsaCertificate.checkValidity(genTime);
            LocalDateTime timestampTime = LocalDateTime.ofInstant(genTime.toInstant(), ZoneId.systemDefault());
            return TimestampValidationResult.valid(timestampTime, "TSA 时间戳已校验");
        } catch (TSPException ex) {
            return TimestampValidationResult.invalid("TSA 令牌解析失败: " + ex.getMessage());
        } catch (Exception ex) {
            return TimestampValidationResult.invalid("TSA 时间戳验证失败: " + ex.getMessage());
        }
    }

    private boolean verifyMessageImprint(TimeStampTokenInfo info, byte[] signature)
            throws IOException, OperatorCreationException {
        DigestCalculator calculator = buildDigestCalculator(info.getMessageImprintAlgOID().getId());
        try (OutputStream outputStream = calculator.getOutputStream()) {
            outputStream.write(signature);
        }
        byte[] digest = calculator.getDigest();
        return MessageDigest.isEqual(digest, info.getMessageImprintDigest());
    }

    private DigestCalculator buildDigestCalculator(String oid) throws OperatorCreationException {
        DigestCalculatorProvider provider = new JcaDigestCalculatorProviderBuilder()
                .setProvider("BC")
                .build();
        return provider.get(new AlgorithmIdentifier(new ASN1ObjectIdentifier(oid)));
    }

    private boolean validateCertificateChain(Store<X509CertificateHolder> certStore,
                                             X509Certificate signerCert,
                                             TrustStoreBundle trustStoreBundle,
                                             boolean strictMode) {
        List<X509Certificate> certificates = new ArrayList<>(certStore.getMatches(null)
                .stream()
                .map(this::convertCertificate)
                .toList());

        if (trustStoreBundle != null) {
            certificates.addAll(trustStoreBundle.certificates());
        }

        Set<TrustAnchor> anchors;
        if (trustStoreBundle != null) {
            anchors = trustStoreBundle.trustAnchors();
        } else if (!strictMode) {
            anchors = certificates.stream()
                    .filter(this::isSelfSigned)
                    .map(cert -> new TrustAnchor(cert, null))
                    .collect(Collectors.toSet());
        } else {
            return false;
        }

        if (anchors.isEmpty()) {
            return false;
        }

        try {
            PKIXBuilderParameters params = new PKIXBuilderParameters(anchors, buildSelector(signerCert));
            // [P2-FIX] CRL/OCSP 吐销检查：通过配置启用
            boolean enableRevocation = Boolean.TRUE.equals(revocationCheckEnabled);
            params.setRevocationEnabled(enableRevocation);
            if (enableRevocation) {
                LOGGER.debug("启用证书吐销检查 (CRL/OCSP)");
            }
            params.addCertStore(CertStore.getInstance("Collection",
                    new CollectionCertStoreParameters(certificates)));

            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
            builder.build(params);
            return true;
        } catch (CertPathBuilderException ex) {
            LOGGER.warn("证书链校验失败: {}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            LOGGER.warn("证书链校验异常: {}", ex.getMessage());
            return false;
        }
    }

    private X509Certificate convertCertificate(X509CertificateHolder holder) {
        try {
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        } catch (Exception ex) {
            throw new IllegalStateException("证书转换失败: " + ex.getMessage(), ex);
        }
    }

    private java.security.cert.X509CertSelector buildSelector(X509Certificate certificate) {
        java.security.cert.X509CertSelector selector = new java.security.cert.X509CertSelector();
        selector.setCertificate(certificate);
        return selector;
    }

    private boolean isSelfSigned(X509Certificate certificate) {
        try {
            certificate.verify(certificate.getPublicKey());
            return certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal());
        } catch (Exception ex) {
            return false;
        }
    }

    private record TimestampValidationResult(boolean valid,
                                             boolean present,
                                             LocalDateTime timestampTime,
                                             String message) {
        private static TimestampValidationResult missing(String message) {
            return new TimestampValidationResult(false, false, null, message);
        }

        private static TimestampValidationResult invalid(String message) {
            return new TimestampValidationResult(false, true, null, message);
        }

        private static TimestampValidationResult valid(LocalDateTime timestampTime, String message) {
            return new TimestampValidationResult(true, true, timestampTime, message);
        }
    }

    private record TrustStoreBundle(String path,
                                    String password,
                                    KeyStore keyStore,
                                    List<X509Certificate> certificates,
                                    Set<TrustAnchor> trustAnchors) {
        private boolean matches(String path, String password) {
            return Objects.equals(this.path, path) && Objects.equals(this.password, password);
        }
    }
}
