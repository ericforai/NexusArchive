// Input: Lombok、Java 标准库
// Output: PageResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应 DTO
 * <p>
 * 用于封装分页查询结果，包含数据列表、分页信息和总数
 * </p>
 *
 * @param <T> 数据项类型
 */
@Data
public class PageResponse<T> {

    /**
     * 当前页数据列表
     */
    private List<T> items;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码（从1开始）
     */
    private int page;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 是否为第一页
     */
    private boolean isFirst;

    /**
     * 是否为最后一页
     */
    private boolean isLast;

    /**
     * 创建空的分页响应
     */
    public PageResponse() {
        this.items = Collections.emptyList();
        this.total = 0;
        this.page = 1;
        this.pageSize = 20;
        this.totalPages = 0;
        this.hasNext = false;
        this.hasPrevious = false;
        this.isFirst = true;
        this.isLast = true;
    }

    /**
     * 创建分页响应
     *
     * @param items    数据列表
     * @param total    总记录数
     * @param page     当前页码
     * @param pageSize 每页大小
     */
    public PageResponse(List<T> items, long total, int page, int pageSize) {
        this.items = items != null ? items : Collections.emptyList();
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
        this.hasNext = this.page < this.totalPages;
        this.hasPrevious = this.page > 1;
        this.isFirst = this.page == 1;
        this.isLast = this.page >= this.totalPages || this.totalPages == 0;
    }

    /**
     * 静态工厂方法：创建空的分页响应
     *
     * @param <T> 数据项类型
     * @return 空的分页响应
     */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>();
    }

    /**
     * 静态工厂方法：创建分页响应
     *
     * @param items    数据列表
     * @param total    总记录数
     * @param page     当前页码
     * @param pageSize 每页大小
     * @param <T>      数据项类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(List<T> items, long total, int page, int pageSize) {
        return new PageResponse<>(items, total, page, pageSize);
    }
}
