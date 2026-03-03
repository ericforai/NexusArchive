// Input: ArchiveCabinetMapper 接口
// Output: ArchiveCabinetService 业务逻辑层
// Pos: src/main/java/com/nexusarchive/service/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.nexusarchive.entity.warehouse.ArchiveCabinet;
import com.nexusarchive.mapper.warehouse.ArchiveCabinetMapper;

import java.util.List;
import java.util.Objects;

/**
 * 档案柜 Service 层
 *
 * 业务逻辑：
 * 1. 柜号生成规则：C-{全宗代码}-{2位序号}
 * 2. 跨全宗柜号唯一性
 * 3. 状态校验与转换
 * 4. 容量统计与使用率计算
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ArchiveCabinetService extends ServiceImpl<ArchiveCabinetMapper, ArchiveCabinet> {

    private final ArchiveCabinetMapper cabinetMapper;

    /**
     * 生成柜号
     *
     * 规则：C-{全宗代码}-{2位序号}
     * 示例：C-01, C-02
     *
     * @param fondsId 全宗ID
     * @return 柜号
     */
    public String generateCode(String fondsId) {
        // 前缀
        String prefix = "C";

        // 查询当前全宗下的最大柜号
        String maxCode = cabinetMapper.selectMaxCode(fondsId);

        // 提取序号部分
        int nextNum = 1;
        if (maxCode != null && maxCode.startsWith(prefix)) {
            try {
                String numStr = maxCode.substring(2);
                nextNum = Integer.parseInt(numStr) + 1;
            } catch (NumberFormatException e) {
                // 忽略解析错误，使用默认值
            }
        }

        // 格式化为2位数字
        return String.format("%s-%02d", prefix, nextNum);
    }

    /**
     * 检查柜号是否已存在
     *
     * @param code 柜号
     * @param fondsId 全宗ID
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    public boolean checkCodeExists(String code, String fondsId, Long excludeId) {
        return cabinetMapper.existsByCode(code, fondsId, excludeId);
    }

    /**
     * 创建档案柜（使用自定义业务逻辑）
     *
     * @param entity 档案柜实体
     * @return 创建后的实体
     */
    public ArchiveCabinet createWithValidation(ArchiveCabinet entity) {
        // 生成柜号
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            entity.setCode(generateCode(String.valueOf(entity.getFondsId())));
        } else {
            // 验证柜号格式和唯一性
            if (!checkCodeFormat(entity.getCode())) {
                throw new IllegalArgumentException("柜号格式不正确，应为 C-XX 格式");
            }
            if (checkCodeExists(entity.getCode(), String.valueOf(entity.getFondsId()), null)) {
                throw new IllegalArgumentException("柜号已存在");
            }
        }

        // 计算总容量
        if (entity.getTotalCapacity() == null) {
            entity.setTotalCapacity(entity.getRows() * entity.getColumns() * entity.getRowCapacity());
        }

        // 设置默认值
        if (entity.getStatus() == null) {
            entity.setStatus("normal");
        }
        if (entity.getCurrentCount() == null) {
            entity.setCurrentCount(0);
        }

        // 创建人
        Long createdBy = entity.getCreatedBy();
        if (createdBy == null) {
            // 从当前上下文获取用户ID
            // TODO: 从 SecurityContext 获取当前用户
            createdBy = 0L;
        }
        entity.setCreatedBy(createdBy);

        baseMapper.insert(entity);
        return entity;
    }

    /**
     * 更新档案柜（使用自定义业务逻辑）
     *
     * @param entity 档案柜实体
     * @return 更新后的实体
     */
    public ArchiveCabinet updateWithValidation(ArchiveCabinet entity) {
        Objects.requireNonNull(entity.getId(), "ID不能为空");

        // 检查权限
        // TODO: 验证用户是否有操作该档案柜的权限

        // 状态校验
        if ("disabled".equals(entity.getStatus())) {
            throw new IllegalArgumentException("已停用的档案柜不能修改");
        }

        // 计算使用率
        Integer currentCount = entity.getCurrentCount();
        Integer totalCapacity = entity.getTotalCapacity();
        if (currentCount != null && totalCapacity != null) {
            entity.setUsageRate(currentCount * 100 / totalCapacity);
        }

        baseMapper.updateById(entity);
        return entity;
    }

    /**
     * 根据柜号查询档案柜
     *
     * @param code 柜号
     * @param fondsId 全宗ID
     * @return 档案柜实体
     */
    public ArchiveCabinet getByCode(String code, String fondsId) {
        ArchiveCabinet entity = cabinetMapper.selectByCodeAndFondsId(code, fondsId);
        if (entity == null) {
            throw new RuntimeException("档案柜不存在: " + code);
        }
        return entity;
    }

    /**
     * 根据柜号查询档案柜（仅柜号）
     *
     * @param code 柜号
     * @return 档案柜实体
     */
    public ArchiveCabinet getByCode(String code) {
        LambdaQueryWrapper<ArchiveCabinet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArchiveCabinet::getCode, code);
        wrapper.last("LIMIT 1");

        ArchiveCabinet entity = baseMapper.selectOne(wrapper);
        if (entity == null) {
            throw new RuntimeException("档案柜不存在: " + code);
        }
        return entity;
    }

    /**
     * 分页查询档案柜列表
     *
     * @param fondsId 全宗ID
     * @param status 状态
     * @param keyword 关键字（柜号/名称搜索）
     * @return 档案柜列表
     */
    public List<ArchiveCabinet> listByCondition(String fondsId, String status, String keyword) {
        LambdaQueryWrapper<ArchiveCabinet> queryWrapper = new LambdaQueryWrapper<>();

        // 全宗过滤
        queryWrapper.eq(ArchiveCabinet::getFondsId, Long.valueOf(fondsId));

        // 状态过滤
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(ArchiveCabinet::getStatus, status);
        }

        // 关键字搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like(ArchiveCabinet::getCode, keyword)
                .or()
                .like(ArchiveCabinet::getName, keyword)
                .or()
                .like(ArchiveCabinet::getLocation, keyword)
            );
        }

        // 排序：按柜号排序
        queryWrapper.orderByAsc(ArchiveCabinet::getCode);

        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 统计档案柜数量
     *
     * @param fondsId 全宗ID
     * @return 档案柜总数
     */
    public int countByFondsId(String fondsId) {
        return cabinetMapper.countByFondsId(fondsId);
    }

    /**
     * 校验柜号格式
     *
     * @param code 柜号
     * @return 是否有效
     */
    private boolean checkCodeFormat(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        // 格式：C-XX（XX 为2位数字）
        return code.matches("^C-\\d{2}$");
    }

    /**
     * 增加当前数量
     *
     * @param cabinetId 档案柜ID
     */
    public void incrementCurrentCount(Long cabinetId) {
        ArchiveCabinet cabinet = baseMapper.selectById(cabinetId);
        if (cabinet != null) {
            Integer currentCount = cabinet.getCurrentCount();
            cabinet.setCurrentCount(currentCount == null ? 1 : currentCount + 1);
            baseMapper.updateById(cabinet);
        }
    }

    /**
     * 按数量增加当前数量
     *
     * @param cabinetId 档案柜ID
     * @param delta 增量（可为负）
     */
    public void incrementCurrentCountByCount(Long cabinetId, int delta) {
        ArchiveCabinet cabinet = baseMapper.selectById(cabinetId);
        if (cabinet != null) {
            Integer currentCount = cabinet.getCurrentCount();
            cabinet.setCurrentCount(currentCount == null ? delta : currentCount + delta);
            baseMapper.updateById(cabinet);
        }
    }
}
