// Input: 归档提交结果
// Output: 提交结果 DTO
// Pos: NexusCore compliance
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

public final class ArchiveSubmitResult {
    private final String fondsNo;
    private final int archiveYear;
    private final String computedHash;
    private final String detectedFileType;
    private final boolean success;

    private ArchiveSubmitResult(Builder builder) {
        this.fondsNo = builder.fondsNo;
        this.archiveYear = builder.archiveYear;
        this.computedHash = builder.computedHash;
        this.detectedFileType = builder.detectedFileType;
        this.success = builder.success;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFondsNo() {
        return fondsNo;
    }

    public int getArchiveYear() {
        return archiveYear;
    }

    public String getComputedHash() {
        return computedHash;
    }

    public String getDetectedFileType() {
        return detectedFileType;
    }

    public boolean isSuccess() {
        return success;
    }

    public static final class Builder {
        private String fondsNo;
        private int archiveYear;
        private String computedHash;
        private String detectedFileType;
        private boolean success;

        private Builder() {
        }

        public Builder fondsNo(String fondsNo) {
            this.fondsNo = fondsNo;
            return this;
        }

        public Builder archiveYear(int archiveYear) {
            this.archiveYear = archiveYear;
            return this;
        }

        public Builder computedHash(String computedHash) {
            this.computedHash = computedHash;
            return this;
        }

        public Builder detectedFileType(String detectedFileType) {
            this.detectedFileType = detectedFileType;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public ArchiveSubmitResult build() {
            return new ArchiveSubmitResult(this);
        }
    }
}
