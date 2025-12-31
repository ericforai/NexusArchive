// Input: 四性检测参数（含完整性/签名/病毒策略）
// Output: 检测请求 DTO
// Pos: NexusCore compliance/fournature
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.nio.file.Path;
import java.util.Objects;

public final class FourNatureCheckRequest {
    private final String expectedHash;
    private final HashAlgorithm hashAlgorithm;
    private final String expectedExtension;
    private final Path metadataXmlPath;
    private final boolean integrityRequired;
    private final boolean signatureRequired;
    private final boolean virusScanRequired;

    private FourNatureCheckRequest(Builder builder) {
        this.expectedHash = builder.expectedHash;
        this.hashAlgorithm = Objects.requireNonNullElse(builder.hashAlgorithm, HashAlgorithm.SM3);
        this.expectedExtension = builder.expectedExtension;
        this.metadataXmlPath = builder.metadataXmlPath;
        this.integrityRequired = builder.integrityRequired;
        this.signatureRequired = builder.signatureRequired;
        this.virusScanRequired = builder.virusScanRequired;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getExpectedHash() {
        return expectedHash;
    }

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public String getExpectedExtension() {
        return expectedExtension;
    }

    public Path getMetadataXmlPath() {
        return metadataXmlPath;
    }

    public boolean isIntegrityRequired() {
        return integrityRequired;
    }

    public boolean isSignatureRequired() {
        return signatureRequired;
    }

    public boolean isVirusScanRequired() {
        return virusScanRequired;
    }

    public static final class Builder {
        private String expectedHash;
        private HashAlgorithm hashAlgorithm;
        private String expectedExtension;
        private Path metadataXmlPath;
        private boolean integrityRequired = true;
        private boolean signatureRequired = false;
        private boolean virusScanRequired = true;

        private Builder() {
        }

        public Builder expectedHash(String expectedHash) {
            this.expectedHash = expectedHash;
            return this;
        }

        public Builder hashAlgorithm(HashAlgorithm hashAlgorithm) {
            this.hashAlgorithm = hashAlgorithm;
            return this;
        }

        public Builder expectedExtension(String expectedExtension) {
            this.expectedExtension = expectedExtension;
            return this;
        }

        public Builder metadataXmlPath(Path metadataXmlPath) {
            this.metadataXmlPath = metadataXmlPath;
            return this;
        }

        public Builder integrityRequired(boolean integrityRequired) {
            this.integrityRequired = integrityRequired;
            return this;
        }

        public Builder signatureRequired(boolean signatureRequired) {
            this.signatureRequired = signatureRequired;
            return this;
        }

        public Builder virusScanRequired(boolean virusScanRequired) {
            this.virusScanRequired = virusScanRequired;
            return this;
        }

        public FourNatureCheckRequest build() {
            return new FourNatureCheckRequest(this);
        }
    }
}
