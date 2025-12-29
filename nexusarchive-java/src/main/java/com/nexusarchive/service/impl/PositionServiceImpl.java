// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: PositionServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.CreatePositionRequest;
import com.nexusarchive.dto.request.UpdatePositionRequest;
import com.nexusarchive.entity.Position;
import com.nexusarchive.mapper.PositionMapper;
import com.nexusarchive.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {

    private final PositionMapper positionMapper;

    @Override
    @Transactional
    public Position create(CreatePositionRequest request) {
        if (existsByCode(request.getCode())) {
            throw new BusinessException("岗位编码已存在");
        }
        Position pos = new Position();
        pos.setId(UUID.randomUUID().toString().replace("-", ""));
        pos.setName(request.getName());
        pos.setCode(request.getCode());
        pos.setDepartmentId(request.getDepartmentId());
        pos.setDescription(request.getDescription());
        pos.setStatus(request.getStatus() == null ? "active" : request.getStatus());
        pos.setCreatedAt(LocalDateTime.now());
        pos.setUpdatedAt(LocalDateTime.now());
        pos.setDeleted(0);
        positionMapper.insert(pos);
        return pos;
    }

    @Override
    @Transactional
    public Position update(UpdatePositionRequest request) {
        Position existing = positionMapper.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException("岗位不存在");
        }
        if (!existing.getCode().equals(request.getCode()) && existsByCode(request.getCode())) {
            throw new BusinessException("岗位编码已存在");
        }
        existing.setName(request.getName());
        existing.setCode(request.getCode());
        existing.setDepartmentId(request.getDepartmentId());
        existing.setDescription(request.getDescription());
        existing.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        positionMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(String id) {
        Position existing = positionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("岗位不存在");
        }
        existing.setDeleted(1);
        existing.setUpdatedAt(LocalDateTime.now());
        positionMapper.updateById(existing);
    }

    @Override
    public Page<Position> list(int page, int limit, String search, String status) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getDeleted, 0);
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(Position::getName, search).or().like(Position::getCode, search));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Position::getStatus, status);
        }
        wrapper.orderByDesc(Position::getCreatedAt);
        Page<Position> pageObj = new Page<>(page, limit);
        return positionMapper.selectPage(pageObj, wrapper);
    }

    private boolean existsByCode(String code) {
        return positionMapper.selectCount(new LambdaQueryWrapper<Position>()
                .eq(Position::getCode, code)
                .eq(Position::getDeleted, 0)) > 0;
    }
}
