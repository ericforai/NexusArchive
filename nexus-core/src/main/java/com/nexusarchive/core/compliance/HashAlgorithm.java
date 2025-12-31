// Input: 哈希算法枚举
// Output: 支持的哈希算法类型
// Pos: NexusCore compliance/hash
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

public enum HashAlgorithm {
    SM3("SM3", "国密 SM3"),
    SHA256("SHA-256", "SHA-256");

    private final String code;
    private final String displayName;

    HashAlgorithm(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
