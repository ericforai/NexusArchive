package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ArchiveBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArchiveBatchMapper extends BaseMapper<ArchiveBatch> {
    
    /**
     * 获取最后一条有效批次的哈希值
     */
    @Select("SELECT chained_hash FROM arc_archive_batch ORDER BY id DESC LIMIT 1")
    String getLastChainedHash();
}
