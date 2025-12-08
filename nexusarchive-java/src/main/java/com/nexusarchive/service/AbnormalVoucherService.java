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
}
