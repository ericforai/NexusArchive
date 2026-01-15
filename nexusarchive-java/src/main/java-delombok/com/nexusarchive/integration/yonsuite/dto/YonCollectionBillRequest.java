// Input: Lombok、Java 标准库
// Output: YonCollectionBillRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 收款单列表查询请求
 */
@Data
public class YonCollectionBillRequest {

    private Integer pageIndex;
    private Integer pageSize;

    private String open_billDate_begin;
    private String open_billDate_end;

    // 状态: 0:开立; 1:审批中; 2:已审批
    private List<String> verifyState;

    // 必须参数: 组织ID (或者通过 simple.financeOrg.code 传编码)
    private List<String> financeOrg;

    // 扩展查询参数 (用于传编码而不是ID)
    private Map<String, Object> simple;

    private Boolean isSum; // false: 返回表头+表体
}
