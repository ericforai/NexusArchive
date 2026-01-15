// Input: MyBatis-Plus、Spring Framework
// Output: SysSqlAuditRuleMapper 接口
// Pos: 数据访问层 Mapper
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.SysSqlAuditRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysSqlAuditRuleMapper extends BaseMapper<SysSqlAuditRule> {
}
