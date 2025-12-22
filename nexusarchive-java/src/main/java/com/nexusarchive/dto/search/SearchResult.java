// Input: Lombok、Java 标准库
// Output: SearchResult 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索结果封装
 * 
 * @author Agent D (基础设施工程师)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult<T> {

    /**
     * 搜索结果列表
     */
    private List<T> items;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;

    /**
     * 搜索耗时（毫秒）
     */
    private long durationMs;

    /**
     * 是否降级到数据库查询
     */
    private boolean fallbackToDb;

    public static <T> SearchResult<T> empty() {
        return new SearchResult<>(List.of(), 0, 0, 0, 0, false);
    }

    public static <T> SearchResult<T> of(List<T> items, long total, int page, int size, long duration) {
        return new SearchResult<>(items, total, page, size, duration, false);
    }

    public static <T> SearchResult<T> ofFallback(List<T> items, long total, int page, int size) {
        return new SearchResult<>(items, total, page, size, 0, true);
    }
}
