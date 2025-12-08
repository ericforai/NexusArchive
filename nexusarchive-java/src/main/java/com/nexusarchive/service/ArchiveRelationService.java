package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.mapper.ArchiveRelationMapper;
import org.springframework.stereotype.Service;

/**
 * 档案关联关系服务
 */
@Service
public class ArchiveRelationService extends ServiceImpl<ArchiveRelationMapper, ArchiveRelation> implements IArchiveRelationService {
}
