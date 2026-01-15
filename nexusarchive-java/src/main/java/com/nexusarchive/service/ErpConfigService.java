// nexusarchive-java/src/main/java/com/nexusarchive/service/ErpConfigService.java
// Input: ERP type
// Output: List of ERP configs / DTOs
// Pos: AI 模块 - ERP 配置服务接口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.ErpConfigDto;
import com.nexusarchive.entity.ErpConfig;

import java.util.List;

/**
 * ERP 配置服务接口
 *
 * <p><strong>方法命名规则：</strong></p>
 * <ul>
 *   <li><code>getXxx()</code> - 返回 DTO，用于 API 响应（已清理敏感信息）</li>
 *   <li><code>getXxxForInternalUse()</code> - 返回 Entity，用于内部调用（包含敏感信息）</li>
 * </ul>
 */
public interface ErpConfigService {

    // ========== API 方法（返回 DTO，已清理敏感信息） ==========

    /**
     * 获取所有配置（API 响应，已清理敏感信息）
     *
     * @return 所有配置 DTO 列表
     */
    List<ErpConfigDto> getConfigs();

    /**
     * 根据 ERP 类型查询配置列表（API 响应，已清理敏感信息）
     *
     * @param erpType ERP 类型（YONSUITE, KINGDEE, WEAVER, GENERIC）
     * @return 配置 DTO 列表
     */
    List<ErpConfigDto> getConfigsByErpType(String erpType);

    /**
     * 根据 ID 查询配置（API 响应，已清理敏感信息）
     *
     * @param configId 配置 ID
     * @return 配置 DTO，不存在返回 null
     */
    ErpConfigDto getConfig(Long configId);

    // ========== 内部方法（返回 Entity，包含敏感信息） ==========

    /**
     * 获取所有配置（内部使用，包含敏感信息）
     *
     * @return 所有配置实体列表
     * @deprecated 使用 {@link #getConfigs()} 获取清理后的 DTO
     */
    @Deprecated
    List<ErpConfig> getAllConfigs();

    /**
     * 根据 ERP 类型查询配置列表（内部使用，包含敏感信息）
     *
     * @param erpType ERP 类型（YONSUITE, KINGDEE, WEAVER, GENERIC）
     * @return 配置实体列表
     * @deprecated 使用 {@link #getConfigsByErpType(String)} 获取清理后的 DTO
     */
    @Deprecated
    List<ErpConfig> findConfigsByErpType(String erpType);

    /**
     * 根据 ID 查询配置
     *
     * @param configId 配置 ID
     * @return 配置对象，不存在返回 null
     */
    ErpConfig findById(Long configId);

    /**
     * 根据 ID 查询完整配置（包含敏感信息）
     * 用于内部调用（如适配器连接测试），不清除敏感字段
     *
     * @param configId 配置 ID
     * @return 完整配置对象，不存在返回 null
     */
    ErpConfig getByIdForInternalUse(Long configId);

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

    /**
     * 根据账套编码获取对应的全宗编码
     *
     * @param accbookCode ERP 账套编码（如 BR01）
     * @return 对应的全宗编码（如 BR-GROUP），如果未找到返回账套编码本身
     */
    String getFondsCodeByAccbook(String accbookCode);

    /**
     * 获取所有账套-全宗映射
     *
     * @return 映射关系 Map，Key=账套编码，Value=全宗编码
     */
    java.util.Map<String, String> getAccbookFondsMapping();
}
