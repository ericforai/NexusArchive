// Input: Jackson、Lombok、Java 标准库
// Output: KingdeeAuthResponse 类
// Pos: 数据传输对象 - 金蝶认证响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto.kingdee;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 金蝶云星空认证响应 DTO
 * Reference: Kingdee K3Cloud ValidateUser API
 *
 * @author Agent D (基础设施工程师)
 */
@Data
public class KingdeeAuthResponse {

    /**
     * 登录结果类型
     * 1-成功, 其他-失败
     */
    @JsonProperty("LoginResultType")
    private Integer loginResultType;

    /**
     * 会话ID
     */
    @JsonProperty("SessionId")
    private String sessionId;

    /**
     * 响应消息
     */
    @JsonProperty("Message")
    private String message;

    /**
     * 响应码
     */
    @JsonProperty("Code")
    private String code;

    /**
     * 上下文ID
     */
    @JsonProperty("Context")
    private String context;

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return loginResultType != null && loginResultType == 1;
    }
}
