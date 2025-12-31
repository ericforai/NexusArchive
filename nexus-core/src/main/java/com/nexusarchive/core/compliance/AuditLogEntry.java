// Input: 审计日志数据
// Output: 审计日志条目
// Pos: NexusCore compliance/audit
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.time.LocalDateTime;

/**
 * 审计日志条目
 */
public class AuditLogEntry {
    private Long id;
    private String traceId;
    private String operator;
    private String operatorName;
    private String action;
    private String target;
    private String dataSnapshot;
    private String prevHash;
    private String currHash;
    private Long chainSeq;
    private LocalDateTime actionTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getDataSnapshot() { return dataSnapshot; }
    public void setDataSnapshot(String dataSnapshot) { this.dataSnapshot = dataSnapshot; }

    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }

    public String getCurrHash() { return currHash; }
    public void setCurrHash(String currHash) { this.currHash = currHash; }

    public Long getChainSeq() { return chainSeq; }
    public void setChainSeq(Long chainSeq) { this.chainSeq = chainSeq; }

    public LocalDateTime getActionTime() { return actionTime; }
    public void setActionTime(LocalDateTime actionTime) { this.actionTime = actionTime; }

    /**
     * 生成用于哈希计算的数据负载
     */
    public String toHashPayload() {
        return String.format("%s|%s|%s|%s|%s|%s",
                traceId, operator, action, target, dataSnapshot, actionTime);
    }
}
