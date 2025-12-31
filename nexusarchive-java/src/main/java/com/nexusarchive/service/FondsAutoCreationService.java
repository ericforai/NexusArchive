// Input: BasFondsService、EntityService、Java 标准库
// Output: FondsAutoCreationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

/**
 * 全宗自动创建服务
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 * 功能: 根据导入数据自动创建全宗和实体
 */
public interface FondsAutoCreationService {
    
    /**
     * 自动创建或获取全宗
     * 
     * @param fondsNo 全宗号
     * @param fondsName 全宗名称
     * @param entityName 法人实体名称（可选）
     * @param entityTaxCode 统一社会信用代码（可选）
     * @param operatorId 操作人ID
     * @return 全宗ID（已存在则返回现有ID，不存在则创建后返回）
     */
    String ensureFondsExists(String fondsNo, 
                             String fondsName,
                             String entityName,
                             String entityTaxCode,
                             String operatorId);
    
    /**
     * 自动创建或获取法人实体
     * 
     * @param entityName 法人实体名称
     * @param entityTaxCode 统一社会信用代码
     * @return 实体ID
     */
    String ensureEntityExists(String entityName, String entityTaxCode);
    
    /**
     * 全宗与实体关联
     * 
     * @param fondsId 全宗ID
     * @param entityId 实体ID
     */
    void associateFondsWithEntity(String fondsId, String entityId);
}

