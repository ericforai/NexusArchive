// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库
// Output: ErpSubInterfaceService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.entity.ErpSubInterface;
import com.nexusarchive.mapper.ErpScenarioMapper;
import com.nexusarchive.mapper.ErpSubInterfaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 子接口管理服务
 * <p>
 * 负责管理 ERP 场景的子接口配置，包括查询、更新和状态切换。
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErpSubInterfaceService {

    private final ErpSubInterfaceMapper erpSubInterfaceMapper;
    private final ErpScenarioMapper erpScenarioMapper;
    private final AuditLogService auditLogService;

    /**
     * 获取场景的子接口列表
     *
     * @param scenarioId 场景 ID
     * @return 子接口列表（按排序字段升序排列）
     */
    public List<ErpSubInterface> listSubInterfaces(Long scenarioId) {
        return erpSubInterfaceMapper.selectList(
            new LambdaQueryWrapper<ErpSubInterface>()
                .eq(ErpSubInterface::getScenarioId, scenarioId)
                .orderByAsc(ErpSubInterface::getSortOrder)
        );
    }

    /**
     * 更新子接口配置
     * 包含：数据验证、审计日志
     *
     * @param subInterface 子接口实体
     * @param operatorId 操作人 ID
     * @param clientIp 客户端 IP
     */
    @Transactional
    public void updateSubInterface(ErpSubInterface subInterface, String operatorId, String clientIp) {
        // 1. 数据验证
        if (subInterface.getId() == null) {
            throw new IllegalArgumentException("子接口 ID 不能为空");
        }
        if (subInterface.getScenarioId() == null) {
            throw new IllegalArgumentException("场景 ID 不能为空");
        }

        // 2. 检查场景是否存在
        ErpScenario scenario = erpScenarioMapper.selectById(subInterface.getScenarioId());
        if (scenario == null) {
            throw new RuntimeException("关联场景不存在: " + subInterface.getScenarioId());
        }

        // 3. 执行更新
        subInterface.setLastModifiedTime(LocalDateTime.now());
        erpSubInterfaceMapper.updateById(subInterface);

        // 4. 审计日志
        auditLogService.log(
            operatorId != null ? operatorId : "SYSTEM",
            "USER_" + operatorId,
            "ERP_SUBINTERFACE_UPDATED",
            "ERP_SUBINTERFACE",
            String.valueOf(subInterface.getId()),
            "SUCCESS",
            "更新子接口: " + subInterface.getInterfaceName(),
            clientIp
        );

        log.info("子接口已更新: id={}, name={}", subInterface.getId(), subInterface.getInterfaceName());
    }

    /**
     * 切换子接口启用状态
     *
     * @param id 子接口 ID
     * @param operatorId 操作人 ID
     * @param clientIp 客户端 IP
     */
    @Transactional
    public void toggleSubInterface(Long id, String operatorId, String clientIp) {
        ErpSubInterface sub = erpSubInterfaceMapper.selectById(id);
        if (sub == null) {
            throw new RuntimeException("子接口不存在: " + id);
        }

        boolean oldStatus = sub.getIsActive();
        sub.setIsActive(!sub.getIsActive());
        sub.setLastModifiedTime(LocalDateTime.now());
        erpSubInterfaceMapper.updateById(sub);

        // 审计日志
        auditLogService.log(
            operatorId != null ? operatorId : "SYSTEM",
            "USER_" + operatorId,
            "ERP_SUBINTERFACE_TOGGLED",
            "ERP_SUBINTERFACE",
            String.valueOf(id),
            "SUCCESS",
            "切换子接口状态: " + sub.getInterfaceName() + " [" + oldStatus + " -> " + sub.getIsActive() + "]",
            clientIp
        );

        log.info("子接口状态已切换: id={}, name={}, isActive={}", id, sub.getInterfaceName(), sub.getIsActive());
    }
}
