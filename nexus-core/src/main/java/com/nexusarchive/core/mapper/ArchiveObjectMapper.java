// Input: MyBatis-Plus
// Output: ArchiveObject Mapper
// Pos: NexusCore mapper
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.core.domain.ArchiveObject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 档案实体 Mapper (Core Module)
 */
@Mapper
public interface ArchiveObjectMapper extends BaseMapper<ArchiveObject> {
}
