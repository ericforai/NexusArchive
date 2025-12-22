// Input: MyBatis-Plus、Spring Framework、Java 标准库、本地模块
// Output: ArchiveRelationService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
