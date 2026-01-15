// Input: Lombok、Jakarta Validation、Java 标准库
// Output: PageRequest 类
// Pos: 数据传输对象 - 分页请求参数
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用分页请求 DTO
 * <p>
 * 用于所有列表查询接口的分页参数
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    /**
     * 页码（从 1 开始）
     */
    @Min(value = 1, message = "页码必须大于等于 1")
    private int pageNum = 1;

    /**
     * 每页大小（默认 20，最大 100）
     */
    @Min(value = 1, message = "每页大小必须大于等于 1")
    @Max(value = 100, message = "每页大小不能超过 100")
    private int pageSize = 20;
}
