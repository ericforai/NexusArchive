// Input: Lombok、Java 标准库、Jakarta Validation
// Output: GlobalSearchRequest DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 全局搜索请求 DTO
 * <p>
 * 支持分页和多条件筛选的全局搜索
 * </p>
 */
@Data
public class GlobalSearchRequest {

    /**
     * 搜索关键字（必填）
     */
    @NotBlank(message = "搜索关键字不能为空")
    private String query;

    /**
     * 匹配类型筛选（可选）
     * <ul>
     *   <li>ARCHIVE - 档案基本信息匹配</li>
     *   <li>METADATA - 元数据匹配</li>
     * </ul>
     */
    private String matchType;

    /**
     * 日期范围筛选 - 开始日期（可选）
     */
    private LocalDate startDate;

    /**
     * 日期范围筛选 - 结束日期（可选）
     */
    private LocalDate endDate;

    /**
     * 页码（从1开始，默认1）
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /**
     * 每页大小（默认20，最大100）
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 20;
}
