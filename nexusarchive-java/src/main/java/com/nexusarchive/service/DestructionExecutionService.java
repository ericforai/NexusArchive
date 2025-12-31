// Input: Destruction Entity, Archive Entity, File Storage
// Output: DestructionExecutionService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

/**
 * 销毁执行服务
 * 
 * 功能：
 * 1. 执行逻辑销毁（权限校验、事务一致性）
 * 2. 支持软删除/硬删除模式
 * 3. 记录文件删除审计日志
 * 4. 生成销毁清册记录
 * 
 * PRD 要求：
 * - 权限要求：Archivist 角色 + DESTRUCTION_APPROVED 状态
 * - 事务边界：清册写入 → 元数据更新 → 物理文件删除
 * - 默认软删除，硬删除需额外审批
 */
public interface DestructionExecutionService {
    
    /**
     * 执行逻辑销毁
     * 
     * @param destructionId 销毁申请ID
     * @param executorId 执行人ID（需校验 Archivist 角色）
     * @param mode 销毁模式：SOFT_DELETE（默认）或 HARD_DELETE（需额外审批）
     * @return 销毁结果（包含销毁数量、TraceID等）
     */
    DestructionExecutionResult executeDestruction(String destructionId, String executorId, DestructionMode mode);
    
    /**
     * 销毁模式枚举
     */
    enum DestructionMode {
        /**
         * 软删除（默认）：标记删除，物理文件保留在隔离存储区
         */
        SOFT_DELETE,
        
        /**
         * 硬删除：物理删除，需额外审批+备份验证
         */
        HARD_DELETE
    }
    
    /**
     * 销毁执行结果
     */
    class DestructionExecutionResult {
        private int destroyedCount;
        private String traceId;
        private DestructionMode mode;
        
        // Getters and Setters
        public int getDestroyedCount() { return destroyedCount; }
        public void setDestroyedCount(int destroyedCount) { this.destroyedCount = destroyedCount; }
        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }
        public DestructionMode getMode() { return mode; }
        public void setMode(DestructionMode mode) { this.mode = mode; }
    }
}

