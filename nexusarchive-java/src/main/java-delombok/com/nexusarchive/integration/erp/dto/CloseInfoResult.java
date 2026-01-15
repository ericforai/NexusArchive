// Input: Lombok、Java 标准库
// Output: CloseInfoResult 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 关账状态检查结果
 * 统一的关账信息返回，适配不同 ERP 系统
 */
@Data
@Builder
public class CloseInfoResult {

    /**
     * 是否已关账
     */
    private boolean closed;

    /**
     * 关账时间，格式 "yyyy-MM-dd HH:mm:ss"，未关账时为 null
     */
    private String closeTime;

    /**
     * 期间，格式 "yyyy-MM"
     */
    private String period;

    /**
     * 账套代码
     */
    private String accbookCode;

    /**
     * 检查是否成功
     */
    private boolean checkSuccess;

    /**
     * 错误信息，检查失败时非空
     */
    private String errorMessage;
}
