// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: SystemPerformanceMetrics 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 系统性能指标实体
 * 对应表: system_performance_metrics
 */
@Data
@TableName("system_performance_metrics")
public class SystemPerformanceMetrics {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 指标类型
     */
    private String metricType;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 指标值
     */
    private BigDecimal metricValue;

    /**
     * 指标单位
     */
    private String metricUnit;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 记录时间
     */
    @TableField(value = "recorded_at", fill = FieldFill.INSERT)
    private LocalDateTime recordedAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

