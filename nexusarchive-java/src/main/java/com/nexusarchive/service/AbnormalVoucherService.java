// Input: Java 标准库、本地模块
// Output: AbnormalVoucherService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.entity.AbnormalVoucher;
import java.util.List;

public interface AbnormalVoucherService {
    
    /**
     * 保存异常凭证
     * @param sip 原始 SIP 数据
     * @param reason 失败原因
     */
    void saveAbnormal(AccountingSipDto sip, String reason);
    
    /**
     * 重试异常凭证
     * @param id 异常记录 ID
     */
    void retry(String id);
    
    /**
     * 获取所有待处理异常
     */
    List<AbnormalVoucher> getPendingAbnormals();
    
    /**
     * 更新 SIP 数据
     */
    void updateSipData(String id, AccountingSipDto newSipDto);
    
    /**
     * 通过 RequestId 标记重试成功的异常记录为 RESOLVED
     * 适用于异步归档成功后的回调
     * @param originalRequestId 原始请求ID（支持带 -R1, -R2 等后缀）
     */
    void markResolvedByRequestId(String originalRequestId);
}
