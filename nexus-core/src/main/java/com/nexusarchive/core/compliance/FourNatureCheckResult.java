// Input: 四性检测结果（含签名/完整性/病毒详情）
// Output: 检测结果 DTO
// Pos: NexusCore compliance/fournature
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

public final class FourNatureCheckResult {
    private final boolean authenticityPassed;
    private final String computedHash;
    private final String authenticityMessage;
    private final SignatureVerifyResult signatureVerifyResult;
    private final boolean integrityPassed;
    private final String integrityMessage;
    private final IntegrityCheckResult integrityCheckResult;
    private final boolean usabilityPassed;
    private final String detectedFileType;
    private final boolean safetyPassed;
    private final String safetyMessage;
    private final VirusScanResult virusScanResult;

    private FourNatureCheckResult(Builder builder) {
        this.authenticityPassed = builder.authenticityPassed;
        this.computedHash = builder.computedHash;
        this.authenticityMessage = builder.authenticityMessage;
        this.signatureVerifyResult = builder.signatureVerifyResult;
        this.integrityPassed = builder.integrityPassed;
        this.integrityMessage = builder.integrityMessage;
        this.integrityCheckResult = builder.integrityCheckResult;
        this.usabilityPassed = builder.usabilityPassed;
        this.detectedFileType = builder.detectedFileType;
        this.safetyPassed = builder.safetyPassed;
        this.safetyMessage = builder.safetyMessage;
        this.virusScanResult = builder.virusScanResult;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isAllPassed() {
        return authenticityPassed && integrityPassed && usabilityPassed && safetyPassed;
    }

    public boolean isAuthenticityPassed() {
        return authenticityPassed;
    }

    public String getComputedHash() {
        return computedHash;
    }

    public String getAuthenticityMessage() {
        return authenticityMessage;
    }

    public SignatureVerifyResult getSignatureVerifyResult() {
        return signatureVerifyResult;
    }

    public boolean isIntegrityPassed() {
        return integrityPassed;
    }

    public String getIntegrityMessage() {
        return integrityMessage;
    }

    public IntegrityCheckResult getIntegrityCheckResult() {
        return integrityCheckResult;
    }

    public boolean isUsabilityPassed() {
        return usabilityPassed;
    }

    public String getDetectedFileType() {
        return detectedFileType;
    }

    public boolean isSafetyPassed() {
        return safetyPassed;
    }

    public String getSafetyMessage() {
        return safetyMessage;
    }

    public VirusScanResult getVirusScanResult() {
        return virusScanResult;
    }

    public static final class Builder {
        private boolean authenticityPassed;
        private String computedHash;
        private String authenticityMessage;
        private SignatureVerifyResult signatureVerifyResult;
        private boolean integrityPassed;
        private String integrityMessage;
        private IntegrityCheckResult integrityCheckResult;
        private boolean usabilityPassed;
        private String detectedFileType;
        private boolean safetyPassed;
        private String safetyMessage;
        private VirusScanResult virusScanResult;

        private Builder() {
        }

        public Builder authenticity(boolean passed, String computedHash) {
            this.authenticityPassed = passed;
            this.computedHash = computedHash;
            return this;
        }

        public Builder authenticityDetail(String message, SignatureVerifyResult signatureVerifyResult) {
            this.authenticityMessage = message;
            this.signatureVerifyResult = signatureVerifyResult;
            return this;
        }

        public Builder integrity(boolean passed, String message) {
            this.integrityPassed = passed;
            this.integrityMessage = message;
            return this;
        }

        public Builder integrityDetail(IntegrityCheckResult integrityCheckResult) {
            this.integrityCheckResult = integrityCheckResult;
            return this;
        }

        public Builder usability(boolean passed, String detectedFileType) {
            this.usabilityPassed = passed;
            this.detectedFileType = detectedFileType;
            return this;
        }

        public Builder safety(boolean passed, String message) {
            this.safetyPassed = passed;
            this.safetyMessage = message;
            return this;
        }

        public Builder safetyDetail(VirusScanResult virusScanResult) {
            this.virusScanResult = virusScanResult;
            return this;
        }

        public FourNatureCheckResult build() {
            return new FourNatureCheckResult(this);
        }
    }
}
