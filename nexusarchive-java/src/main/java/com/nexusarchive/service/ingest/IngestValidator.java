// Input: Spring Framework, Lombok
// Output: IngestValidator 接口
// Pos: 服务层 - SIP 接收验证器接口

package com.nexusarchive.service.ingest;

import com.nexusarchive.dto.sip.AccountingSipDto;

/**
 * SIP 接收验证器接口
 * <p>
 * 负责验证 SIP 请求的业务规则完整性
 * </p>
 */
public interface IngestValidator {

    /**
     * 验证 SIP 请求的业务规则
     *
     * @param sipDto 待验证的 SIP 请求
     * @throws com.nexusarchive.common.exception.BusinessException 验证失败时抛出
     */
    void validate(AccountingSipDto sipDto);

    /**
     * 获取验证器名称
     */
    String getName();

    /**
     * 获取验证优先级（数字越小优先级越高）
     */
    int getPriority();
}
