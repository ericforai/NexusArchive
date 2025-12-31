// Input: SysEntity、BasFonds、Archive
// Output: EnterpriseArchitectureTree DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;
import java.util.List;

/**
 * 集团架构树 DTO
 * 
 * 用于展示"法人 -> 全宗 -> 档案"的层级关系
 */
@Data
public class EnterpriseArchitectureTree {
    
    /**
     * 法人节点列表
     */
    private List<EntityNode> entities;
    
    /**
     * 法人节点
     */
    @Data
    public static class EntityNode {
        private String id;
        private String name;
        private String taxId;
        private String status;
        private Integer fondsCount; // 全宗数量
        private Long archiveCount; // 档案数量
        private Double totalSizeGB; // 总容量（GB）
        private List<FondsNode> fonds;
    }
    
    /**
     * 全宗节点
     */
    @Data
    public static class FondsNode {
        private String id;
        private String fondsCode;
        private String fondsName;
        private Long archiveCount; // 档案数量
        private Double sizeGB; // 容量（GB）
        private Integer archiveYearCount; // 归档年度数量
    }
}

