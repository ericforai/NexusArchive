// Input: Lombok、Java 标准库
// Output: YonCloseInfoRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;

/**
 * 关账状态查询请求 DTO
 * Reference: YonSuite /yonbip/EFI/closeInfo API
 */
@Data
public class YonCloseInfoRequest {

    /**
     * 账簿 ID 数组
     */
    private List<String> books;

    /**
     * 期间，格式 "yyyy-MM"
     */
    private String period;
}
