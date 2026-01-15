// Input: MyBatis-Plus BaseMapper
// Output: FileHashDedupScopeMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.FileHashDedupScope;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件哈希去重范围配置 Mapper
 */
@Mapper
public interface FileHashDedupScopeMapper extends BaseMapper<FileHashDedupScope> {
}





