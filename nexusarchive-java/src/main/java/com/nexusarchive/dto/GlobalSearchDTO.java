// Input: Lombok、Java 标准库
// Output: GlobalSearchDTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

@Data
public class GlobalSearchDTO {
    private String id;
    private String archiveCode;
    private String title;
    private String matchType; // "ARCHIVE" or "METADATA"
    private String matchDetail; // e.g., "Invoice No: 12345"
    private Double score;

    public GlobalSearchDTO() {}

    public GlobalSearchDTO(String id, String archiveCode, String title, String matchType, String matchDetail) {
        this.id = id;
        this.archiveCode = archiveCode;
        this.title = title;
        this.matchType = matchType;
        this.matchDetail = matchDetail;
    }
}
