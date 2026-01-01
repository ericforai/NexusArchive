// Input: Lombok, Java 标准库
// Output: ErpPluginResult 类
// Pos: 服务层 - ERP 插件结果

package com.nexusarchive.service.erp.plugin;

import com.nexusarchive.integration.erp.dto.VoucherDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * ERP 插件执行结果
 * <p>
 * 封装插件执行的返回结果
 * </p>
 */
@Data
@Builder
public class ErpPluginResult {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 获取的凭证数量
     */
    private int totalCount;

    /**
     * 成功处理的凭证数量
     */
    private int successCount;

    /**
     * 失败的凭证数量
     */
    private int failCount;

    /**
     * 凭证列表
     */
    private List<VoucherDTO> vouchers;

    /**
     * 额外信息
     */
    private String message;

    /**
     * 创建成功结果
     */
    public static ErpPluginResult success(int count, List<VoucherDTO> vouchers) {
        return ErpPluginResult.builder()
                .success(true)
                .totalCount(count)
                .vouchers(vouchers)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ErpPluginResult failure(String errorMessage) {
        return ErpPluginResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
