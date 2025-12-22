// Input: Lombok、Java 标准库
// Output: YonCollectionFileResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;

@Data
public class YonCollectionFileResponse {
    private String code;
    private String message;
    private List<FileItem> data;

    @Data
    public static class FileItem {
        private String downLoadUrl;
        private String fileName;
        private String id;
    }
}
