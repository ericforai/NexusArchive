// Input: MyBatis-Plus BaseMapper
// Output: EmployeeLifecycleEventMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.EmployeeLifecycleEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工生命周期事件 Mapper
 */
@Mapper
public interface EmployeeLifecycleEventMapper extends BaseMapper<EmployeeLifecycleEvent> {
}

