// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: BasFondsServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.mapper.ArchivalCodeSequenceMapper;
import com.nexusarchive.mapper.BasFondsMapper;
import com.nexusarchive.service.BasFondsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BasFondsServiceImpl extends ServiceImpl<BasFondsMapper, BasFonds> implements BasFondsService {

    private final ArchivalCodeSequenceMapper sequenceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initSequence(String fondsCode, String year, String category) {
        sequenceMapper.initSequence(fondsCode, year, category);
    }

    @Override
    public boolean canModifyFondsCode(String fondsCode) {
        // 如果全宗号下没有归档档案，则可以修改
        return !baseMapper.hasArchivedRecords(fondsCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFonds(BasFonds fonds) {
        if (fonds.getId() == null) {
            throw new BusinessException("全宗ID不能为空");
        }

        // 获取原有记录
        BasFonds existing = getById(fonds.getId());
        if (existing == null) {
            throw new BusinessException("全宗不存在");
        }

        // 检查全宗号是否被修改
        String oldFondsCode = existing.getFondsCode();
        String newFondsCode = fonds.getFondsCode();

        if (newFondsCode != null && !newFondsCode.equals(oldFondsCode)) {
            // 全宗号被修改，检查是否存在归档档案
            if (baseMapper.hasArchivedRecords(oldFondsCode)) {
                throw new BusinessException("全宗号已有归档档案，不可修改");
            }
        }

        return updateById(fonds);
    }
}

