// Input: MyBatis-Plus、Java 标准库、本地模块
// Output: IArchiveRelationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexusarchive.entity.ArchiveRelation;

/**
 * 档案关联关系服务接口
 */
public interface IArchiveRelationService extends IService<ArchiveRelation> {
}
