// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: BasFondsServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}
