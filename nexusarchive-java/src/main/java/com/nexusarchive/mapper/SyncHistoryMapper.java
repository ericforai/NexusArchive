package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.SyncHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步历史记录 Mapper
 */
@Mapper
public interface SyncHistoryMapper extends BaseMapper<SyncHistory> {
}
