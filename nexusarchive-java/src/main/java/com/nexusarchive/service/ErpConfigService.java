// nexusarchive-java/src/main/java/com/nexusarchive/service/ErpConfigService.java
// Input: ERP type
// Output: List of ERP configs
// Pos: AI 模块 - ERP 配置服务接口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.ErpConfig;

import java.util.List;

/**
 * ERP 配置服务接口
 */
public interface ErpConfigService {

    /**
     * 获取所有配置
     *
     * @return 所有配置列表
     */
    List<ErpConfig> getAllConfigs();

    /**
     * 根据 ERP 类型查询配置列表
     *
     * @param erpType ERP 类型（YONSUITE, KINGDEE, WEAVER, GENERIC）
     * @return 配置列表
     */
    List<ErpConfig> findConfigsByErpType(String erpType);

    /**
     * 根据 ID 查询配置
     *
     * @param configId 配置 ID
     * @return 配置对象，不存在返回 null
     */
    ErpConfig findById(Long configId);

    /**
     * 保存或更新配置（包含敏感信息加密处理）
     *
     * @param config 配置对象
     */
    void saveConfig(ErpConfig config);

    /**
     * 删除配置
     *
     * @param id 配置 ID
     */
    void deleteConfig(Long id);

    /**
     * 统计配置总数
     *
     * @return 配置总数
     */
    Long countConfigs();

    /**
     * 统计活跃配置数量
     *
     * @return 活跃配置数量
     */
    Long countActiveConfigs();
}
