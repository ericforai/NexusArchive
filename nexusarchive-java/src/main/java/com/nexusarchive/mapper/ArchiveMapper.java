package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.Archive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 档案Mapper
 */
@Mapper
public interface ArchiveMapper extends BaseMapper<Archive> {
}
