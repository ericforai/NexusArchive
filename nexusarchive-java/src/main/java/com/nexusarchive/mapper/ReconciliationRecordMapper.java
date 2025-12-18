package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ReconciliationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 三位一体核对记录 Mapper
 */
@Mapper
public interface ReconciliationRecordMapper extends BaseMapper<ReconciliationRecord> {
}
