// Input: Lombok、Java 标准库
// Output: ErpPageRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ErpPageRequest {
    private int pageIndex = 1;
    private int pageSize = 20;
    private LocalDate startDate;
    private LocalDate endDate;
    private String orgCode; // 会计主体/组织编码
}
