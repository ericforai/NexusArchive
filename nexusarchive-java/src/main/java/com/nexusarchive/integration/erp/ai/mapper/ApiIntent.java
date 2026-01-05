// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/ApiIntent.java
// Input: OpenAPI definition
// Output: ApiIntent representing business purpose
// Pos: AI 模块 - 业务语义映射器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.mapper;

import lombok.Builder;
import lombok.Data;

/**
 * API 接口意图
 * <p>
 * 描述 API 的业务目的和特征
 * </p>
 */
@Data
@Builder
public class ApiIntent {

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 业务对象
     */
    private BusinessObject businessObject;

    /**
     * 触发时机
     */
    private TriggerTiming triggerTiming;

    /**
     * 数据流向
     */
    private DataFlowDirection dataFlowDirection;

    /**
     * 操作类型
     */
    public enum OperationType {
        QUERY,      // 查询
        SYNC,       // 同步
        SUBMIT,     // 提交
        CALLBACK,   // 回调
        NOTIFY      // 通知
    }

    /**
     * 业务对象类型
     */
    public enum BusinessObject {
        ACCOUNTING_VOUCHER,  // 记账凭证
        INVOICE,             // 发票
        RECEIPT,             // 收据
        CONTRACT,            // 合同
        ATTACHMENT,          // 附件
        ACCOUNT_BALANCE,     // 科目余额
        SALES_OUT,           // 销售出库
        REFUND,              // 退款单
        UNKNOWN
    }

    /**
     * 触发时机
     */
    public enum TriggerTiming {
        SCHEDULED,   // 定时触发
        EVENT_BASED, // 事件触发
        REALTIME,    // 实时查询
        ON_DEMAND    // 按需
    }

    /**
     * 数据流向
     */
    public enum DataFlowDirection {
        ERP_TO_SYSTEM,  // ERP → 系统
        SYSTEM_TO_ERP,  // 系统 → ERP
        BIDIRECTIONAL   // 双向
    }
}
