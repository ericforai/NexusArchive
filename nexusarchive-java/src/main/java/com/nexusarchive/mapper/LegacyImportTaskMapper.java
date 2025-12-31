// Input: MyBatis-Plus BaseMapper
// Output: LegacyImportTaskMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.LegacyImportTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 历史数据导入任务 Mapper
 * 对应表: legacy_import_task
 */
@Mapper
public interface LegacyImportTaskMapper extends BaseMapper<LegacyImportTask> {
}

