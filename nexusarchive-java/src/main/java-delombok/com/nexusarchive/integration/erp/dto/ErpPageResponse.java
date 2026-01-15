// Input: Lombok、Java 标准库
// Output: ErpPageResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import lombok.Data;
import java.util.List;

@Data
public class ErpPageResponse<T> {
    private List<T> items;
    private long total;
    private int pageIndex;
    private int pageSize;
    
    public static <T> ErpPageResponse<T> of(List<T> items, long total, int pageIndex, int pageSize) {
        ErpPageResponse<T> response = new ErpPageResponse<>();
        response.setItems(items);
        response.setTotal(total);
        response.setPageIndex(pageIndex);
        response.setPageSize(pageSize);
        return response;
    }
}
