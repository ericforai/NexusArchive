// Input: MyBatis-Plus、Java 标准库、Lombok
// Output: ArcFileContent 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 电子文件存储记录
 * 对应表: arc_file_content
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_file_content")
public class ArcFileContent {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 关联的档案号 (Item Level)
     */
    private String archivalCode;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String fileHash;

    private String hashAlgorithm;

    /**
     * 物理存储路径 (相对路径或绝对路径)
     */
    private String storagePath;

    /**
     * 关联单据ID
     */
    private String itemId;

    /**
     * 原始哈希值 (接收时)
     */
    private String originalHash;

    /**
     * 当前哈希值 (巡检时)
     */
    private String currentHash;

    /**
     * 时间戳Token
     */
    private byte[] timestampToken;

    /**
     * 电子签名值
     */
    private byte[] signValue;

    /**
     * 数字证书 (Base64)
     */
    private String certificate;

    // ===== 预归档状态管理 (第一阶段新增) =====

    /**
     * 预归档状态: PENDING_CHECK/CHECK_FAILED/PENDING_METADATA/PENDING_ARCHIVE/ARCHIVED
     */
    private String preArchiveStatus;

    /**
     * 四性检测结果 (JSON格式)
     */
    private String checkResult;

    /**
     * 检测时间
     */
    private LocalDateTime checkedTime;

    /**
     * 归档时间
     */
    private LocalDateTime archivedTime;

    // ===== DA/T 94-2022 必填元数据 =====

    /**
     * 会计年度
     */
    private String fiscalYear;

    /**
     * 会计期间
     */
    private String period;

    /**
     * 凭证类型
     */
    private String voucherType;

    /**
     * 责任者/创建人
     */
    private String creator;

    /**
     * 全宗号
     */
    private String fondsCode;

    /**
     * 来源系统
     */
    private String sourceSystem;

    /**
     * 来源唯一标识（幂等性控制，如 YonSuite_xxx）
     */
    private String businessDocNo;

    /**
     * ERP原始凭证号（用户可读，如 记-3）
     */
    private String erpVoucherNo;

    /**
     * 原始业务数据(JSON)
     * 用于按需生成文件或审计追溯
     */
    private String sourceData;

    /**
     * 归档批次 ID
     */
    private Long batchId;

    /**
     * 批次内序号
     */
    private Integer sequenceInBatch;

    private String summary;

    private String voucherWord;

    private LocalDate docDate;

    /**
     * 文件高亮元数据(坐标信息)
     */
    @TableField(value = "highlight_meta", typeHandler = com.nexusarchive.config.PostgresJsonTypeHandler.class)
    private String highlightMeta;

    @TableField(exist = false)
    private Map<String, Object> highlightMetaMap;

    public Map<String, Object> getHighlightMetaMap() {
        if (highlightMetaMap == null && highlightMeta != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                highlightMetaMap = mapper.readValue(highlightMeta, Map.class);
            } catch (Exception e) {
                // Ignore parse error
            }
        }
        return highlightMetaMap;
    }

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
