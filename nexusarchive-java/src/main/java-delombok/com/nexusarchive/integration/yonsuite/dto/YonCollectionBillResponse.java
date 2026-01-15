// Input: Lombok、Java 标准库
// Output: YonCollectionBillResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;
import java.util.List;

/**
 * 收款单列表查询响应
 */
@Data
public class YonCollectionBillResponse {

    private String code;
    private String message;
    private PageData data;

    @Data
    public static class PageData {
        private Integer pageIndex;
        private Integer pageSize;
        private Integer recordCount;
        private List<Record> recordList;
    }

    @Data
    public static class Record {
        private String id;
        private String code; // 单据编号
        private String billDate; // 单据日期
        private Integer verifyState;
        private String billTypeName;
        // 其他字段按需添加
    }
}
