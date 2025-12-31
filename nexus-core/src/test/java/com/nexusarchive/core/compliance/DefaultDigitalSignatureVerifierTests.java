// Input: JUnit + PDFBox + BouncyCastle
// Output: DefaultDigitalSignatureVerifier 验签测试
// Pos: NexusCore tests/compliance
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenGenerator;
import org.bouncycastle.util.Store;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDigitalSignatureVerifierTests {
    private static final ASN1ObjectIdentifier OID_TSA_TIMESTAMP_TOKEN =
            new ASN1ObjectIdentifier("1.2.840.113549.1.9.16.2.14");
    @TempDir
    Path tempDir;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    void shouldVerifySignedPdfWithTsaTokenAndTruststore() throws Exception {
        DefaultDigitalSignatureVerifier verifier = new DefaultDigitalSignatureVerifier();
        KeyPair rootKeyPair = generateKeyPair();
        X509Certificate rootCert = createRootCertificate(rootKeyPair, "NexusArchive-Root");

        KeyPair signerKeyPair = generateKeyPair();
        X509Certificate signerCert = createLeafCertificate(
                signerKeyPair, rootCert, rootKeyPair.getPrivate(), "NexusArchive-Signer", false);

        KeyPair tsaKeyPair = generateKeyPair();
        X509Certificate tsaCert = createLeafCertificate(
                tsaKeyPair, rootCert, rootKeyPair.getPrivate(), "NexusArchive-TSA", true);

        Path truststorePath = createTrustStore(
                tempDir.resolve("truststore.p12"),
                "changeit",
                rootCert);

        verifier.setStrictModeConfig(true);
        verifier.setTruststorePathConfig(truststorePath.toString());
        verifier.setTruststorePasswordConfig("changeit");

        Path unsignedPdf = createSamplePdf(tempDir.resolve("unsigned.pdf"));
        Path signedPdf = tempDir.resolve("signed.pdf");

        signPdf(unsignedPdf, signedPdf,
                signerKeyPair.getPrivate(),
                signerCert,
                List.of(signerCert, rootCert),
                new TsaMaterial(tsaKeyPair.getPrivate(), tsaCert, rootCert));

        SignatureVerifyResult result = verifier.verify(signedPdf);

        assertTrue(result.signaturePresent());
        assertTrue(result.valid());
        assertTrue(result.timestampPresent());
        assertTrue(result.timestampValid());
        assertTrue(result.complianceValid());
        assertTrue(result.signTime() != null);
    }

    @Test
    void shouldFailWhenTimestampMissingInStrictMode() throws Exception {
        DefaultDigitalSignatureVerifier verifier = new DefaultDigitalSignatureVerifier();
        KeyPair rootKeyPair = generateKeyPair();
        X509Certificate rootCert = createRootCertificate(rootKeyPair, "NexusArchive-Root");

        KeyPair signerKeyPair = generateKeyPair();
        X509Certificate signerCert = createLeafCertificate(
                signerKeyPair, rootCert, rootKeyPair.getPrivate(), "NexusArchive-Signer", false);

        Path truststorePath = createTrustStore(
                tempDir.resolve("truststore-no-tsa.p12"),
                "changeit",
                rootCert);

        verifier.setStrictModeConfig(true);
        verifier.setTruststorePathConfig(truststorePath.toString());
        verifier.setTruststorePasswordConfig("changeit");

        Path unsignedPdf = createSamplePdf(tempDir.resolve("unsigned-no-tsa.pdf"));
        Path signedPdf = tempDir.resolve("signed-no-tsa.pdf");

        signPdf(unsignedPdf, signedPdf,
                signerKeyPair.getPrivate(),
                signerCert,
                List.of(signerCert, rootCert),
                null);

        SignatureVerifyResult result = verifier.verify(signedPdf);

        assertFalse(result.valid());
        assertFalse(result.timestampPresent());
        assertFalse(result.timestampValid());
    }

    @Test
    void shouldVerifySignedPdfInNonStrictMode() throws Exception {
        DefaultDigitalSignatureVerifier verifier = new DefaultDigitalSignatureVerifier();
        verifier.setStrictModeConfig(false);

        KeyPair keyPair = generateKeyPair();
        X509Certificate certificate = createRootCertificate(keyPair, "NexusArchive-Self");

        Path unsignedPdf = createSamplePdf(tempDir.resolve("unsigned-nonstrict.pdf"));
        Path signedPdf = tempDir.resolve("signed-nonstrict.pdf");

        signPdf(unsignedPdf, signedPdf,
                keyPair.getPrivate(),
                certificate,
                List.of(certificate),
                null);

        SignatureVerifyResult result = verifier.verify(signedPdf);

        assertTrue(result.signaturePresent());
        assertTrue(result.valid());
        assertFalse(result.timestampPresent());
        assertFalse(result.timestampValid());
    }

    private Path createSamplePdf(Path target) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(target.toFile());
        }
        return target;
    }

    private void signPdf(Path source,
                         Path target,
                         PrivateKey privateKey,
                         X509Certificate certificate,
                         List<X509Certificate> certificateChain,
                         TsaMaterial tsaMaterial) throws Exception {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("Unit Test");
            signature.setLocation("Local");
            signature.setReason("Compliance test");
            signature.setSignDate(Calendar.getInstance());

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);

            document.addSignature(signature,
                    new CmsSigner(privateKey, certificate, certificateChain, tsaMaterial),
                    options);
            try (OutputStream outputStream = Files.newOutputStream(target)) {
                document.saveIncremental(outputStream);
            }
        }
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private X509Certificate createRootCertificate(KeyPair keyPair, String commonName) throws Exception {
        X500Name issuer = new X500Name("CN=" + commonName);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = Date.from(Instant.now().minusSeconds(3600));
        Date notAfter = Date.from(Instant.now().plusSeconds(86400));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                issuer,
                keyPair.getPublic());

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);
    }

    private X509Certificate createLeafCertificate(KeyPair keyPair,
                                                   X509Certificate issuerCert,
                                                   PrivateKey issuerKey,
                                                   String commonName,
                                                   boolean timestamping) throws Exception {
        X500Name issuer = new X500Name(issuerCert.getSubjectX500Principal().getName());
        X500Name subject = new X500Name("CN=" + commonName);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis() + commonName.hashCode());
        Date notBefore = Date.from(Instant.now().minusSeconds(3600));
        Date notAfter = Date.from(Instant.now().plusSeconds(86400));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(issuerKey);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic());

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        if (timestamping) {
            builder.addExtension(Extension.extendedKeyUsage,
                    true,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
        }

        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);
    }

    private Path createTrustStore(Path target,
                                  String password,
                                  X509Certificate... certificates) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        for (int i = 0; i < certificates.length; i++) {
            keyStore.setCertificateEntry("trusted-" + i, certificates[i]);
        }
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            keyStore.store(outputStream, password.toCharArray());
        }
        return target;
    }

    private static TimeStampToken createTimeStampToken(byte[] signature, TsaMaterial tsaMaterial) throws Exception {
        DigestCalculatorProvider calculatorProvider = new JcaDigestCalculatorProviderBuilder()
                .setProvider("BC")
                .build();
        DigestCalculator digestCalculator = calculatorProvider.get(
                new AlgorithmIdentifier(TSPAlgorithms.SHA256));

        TimeStampTokenGenerator tokenGenerator = new TimeStampTokenGenerator(
                new JcaSimpleSignerInfoGeneratorBuilder()
                        .setProvider("BC")
                        .build("SHA256withRSA", tsaMaterial.privateKey(), tsaMaterial.certificate()),
                digestCalculator,
                new ASN1ObjectIdentifier("1.2.3.4.1"));

        tokenGenerator.addCertificates(new JcaCertStore(
                List.of(tsaMaterial.certificate(), tsaMaterial.rootCertificate())));

        byte[] messageImprint = java.security.MessageDigest
                .getInstance("SHA-256")
                .digest(signature);

        TimeStampRequestGenerator requestGenerator = new TimeStampRequestGenerator();
        requestGenerator.setCertReq(true);
        TimeStampRequest request = requestGenerator.generate(
                TSPAlgorithms.SHA256,
                messageImprint,
                BigInteger.valueOf(System.nanoTime()));

        return tokenGenerator.generate(
                request,
                BigInteger.valueOf(System.currentTimeMillis()),
                new Date());
    }

    private record TsaMaterial(PrivateKey privateKey, X509Certificate certificate, X509Certificate rootCertificate) {
    }

    private static final class CmsSigner implements SignatureInterface {
        private final PrivateKey privateKey;
        private final X509Certificate certificate;
        private final List<X509Certificate> certificateChain;
        private final TsaMaterial tsaMaterial;

        private CmsSigner(PrivateKey privateKey,
                          X509Certificate certificate,
                          List<X509Certificate> certificateChain,
                          TsaMaterial tsaMaterial) {
            this.privateKey = privateKey;
            this.certificate = certificate;
            this.certificateChain = certificateChain;
            this.tsaMaterial = tsaMaterial;
        }

        @Override
        public byte[] sign(InputStream content) throws IOException {
            try {
                byte[] data = content.readAllBytes();
                CMSSignedDataGenerator generator = new CMSSignedDataGenerator();

                ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                        .setProvider("BC")
                        .build(privateKey);

                generator.addSignerInfoGenerator(
                        new JcaSignerInfoGeneratorBuilder(
                                new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                                .build(signer, certificate));

                Store<X509CertificateHolder> certStore = new JcaCertStore(certificateChain);
                generator.addCertificates(certStore);

                CMSSignedData signedData = generator.generate(new CMSProcessableByteArray(data), false);
                if (tsaMaterial != null) {
                    signedData = attachTimestamp(signedData, tsaMaterial);
                }
                return signedData.getEncoded();
            } catch (Exception ex) {
                throw new IOException("签名生成失败: " + ex.getMessage(), ex);
            }
        }

        private CMSSignedData attachTimestamp(CMSSignedData signedData, TsaMaterial tsaMaterial) throws Exception {
            SignerInformation signerInformation = signedData.getSignerInfos().getSigners().iterator().next();
            TimeStampToken token = createTimeStampToken(signerInformation.getSignature(), tsaMaterial);

            AttributeTable unsignedAttrs = signerInformation.getUnsignedAttributes();
            ASN1EncodableVector vector = unsignedAttrs == null
                    ? new ASN1EncodableVector()
                    : unsignedAttrs.toASN1EncodableVector();
            vector.add(new Attribute(
                    OID_TSA_TIMESTAMP_TOKEN,
                    new DERSet(token.toCMSSignedData().toASN1Structure())));

            SignerInformation updatedSigner = SignerInformation.replaceUnsignedAttributes(
                    signerInformation,
                    new AttributeTable(vector));

            SignerInformationStore signerStore = new SignerInformationStore(List.of(updatedSigner));
            return CMSSignedData.replaceSigners(signedData, signerStore);
        }
    }
}
