// Input: Spring, Java 标准库
// Output: ErpMapper 接口
// Pos: 集成模块 - ERP 统一映射接口

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.integration.erp.dto.ErpConfig;

import java.util.List;

/**
 * ERP 统一映射接口
 * 屏蔽不同 ERP 的数据结构差异
 *
 * <p>该接口定义了将不同 ERP 系统的响应数据转换为标准化 SIP DTO 的契约。</p>
 *
 * <p>实现类可以是：</p>
 * <ul>
 *   <li>基于配置的通用映射器 (DefaultErpMapper)</li>
 *   <li>特定 ERP 的专用映射器 (YonSuiteErpMapper, KingdeeErpMapper 等)</li>
 * </ul>
 */
public interface ErpMapper {

    /**
     * 将 ERP 响应转换为 AccountingSipDto
     *
     * @param erpResponse ERP 原始响应对象，可以是任何类型
     * @param sourceSystem ERP 标识 (如 "yonsuite", "kingdee", "nc")
     * @param config ERP 配置，包含连接信息和业务规则
     * @return 标准化的 SIP DTO
     * @throws MappingException 当映射失败时抛出
     */
    AccountingSipDto mapToSipDto(Object erpResponse, String sourceSystem, ErpConfig config);

    /**
     * 批量映射
     *
     * @param erpResponses ERP 原始响应对象列表
     * @param sourceSystem ERP 标识
     * @param config ERP 配置
     * @return 标准化的 SIP DTO 列表
     * @throws MappingException 当任一条映射失败时抛出
     */
    default List<AccountingSipDto> mapToSipDto(
            List<?> erpResponses,
            String sourceSystem,
            ErpConfig config) {
        return erpResponses.stream()
                .map(r -> mapToSipDto(r, sourceSystem, config))
                .toList();
    }
}
