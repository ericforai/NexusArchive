// Input: PreservationAudit entity
// Output: Mapper interface
// Pos: NexusCore mapper
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.core.domain.PreservationAudit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PreservationAuditMapper extends BaseMapper<PreservationAudit> {
}
