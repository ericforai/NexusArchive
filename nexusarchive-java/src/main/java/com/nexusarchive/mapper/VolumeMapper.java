// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: VolumeMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.Volume;
import org.apache.ibatis.annotations.Mapper;

/**
 * 案卷 Mapper
 */
@Mapper
public interface VolumeMapper extends BaseMapper<Volume> {
}
