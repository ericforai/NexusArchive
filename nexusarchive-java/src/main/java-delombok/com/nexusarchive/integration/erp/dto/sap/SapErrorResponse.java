// Input: Jackson、Lombok、Java 标准库
// Output: SapErrorResponse 类
// Pos: 数据传输对象 - SAP OData 错误响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto.sap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * SAP OData 错误响应 DTO
 * 代表 SAP S/4HANA OData API 返回的错误信息
 *
 * @author Agent D (基础设施工程师)
 * @see <a href="https://docs.oasis-open.org/odata/odata/v4.0/">OData V4 Error Response</a>
 */
@Data
public class SapErrorResponse {

    /**
     * 错误对象
     */
    @JsonProperty("error")
    private ErrorDetail error;

    /**
     * 错误详情
     */
    @Data
    public static class ErrorDetail {

        /**
         * 错误代码
         */
        @JsonProperty("code")
        private String code;

        /**
         * 错误消息
         */
        @JsonProperty("message")
        private ErrorMessage message;

        /**
         * 内部错误信息
         */
        @JsonProperty("innererror")
        private InnerError innerError;
    }

    /**
     * 错误消息
     */
    @Data
    public static class ErrorMessage {

        /**
         * 语言
         */
        @JsonProperty("lang")
        private String lang;

        /**
         * 消息内容
         */
        @JsonProperty("value")
        private String value;
    }

    /**
     * 内部错误
     */
    @Data
    public static class InnerError {

        /**
         * 应用程序错误代码
         */
        @JsonProperty("application")
        private String application;

        /**
         * 事务 ID
         */
        @JsonProperty("transactionId")
        private String transactionId;

        /**
         * 时间戳
         */
        @JsonProperty("timestamp")
        private String timestamp;

        /**
         * 错误详情
         */
        @JsonProperty("Error_Resolution")
        private ErrorResolution errorResolution;
    }

    /**
     * 错误解决方案
     */
    @Data
    public static class ErrorResolution {

        /**
         * 错误描述
         */
        @JsonProperty("errordetails")
        private java.util.List<ErrorDetailItem> errorDetails;
    }

    /**
     * 错误详情项
     */
    @Data
    public static class ErrorDetailItem {

        /**
         * 代码
         */
        @JsonProperty("code")
        private String code;

        /**
         * 消息
         */
        @JsonProperty("message")
        private String message;

        /**
         * 属性路径
         */
        @JsonProperty("propertyref")
        private String propertyRef;
    }

    /**
     * 获取错误消息 (简化访问)
     */
    public String getMessage() {
        return error != null && error.getMessage() != null
            ? error.getMessage().getValue()
            : "Unknown SAP error";
    }

    /**
     * 获取错误代码
     */
    public String getCode() {
        return error != null ? error.getCode() : "UNKNOWN";
    }
}
