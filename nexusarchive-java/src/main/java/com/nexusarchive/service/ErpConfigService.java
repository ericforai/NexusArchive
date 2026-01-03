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
}
