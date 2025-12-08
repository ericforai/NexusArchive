package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ArcFileContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArcFileContentMapper extends BaseMapper<ArcFileContent> {
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM arc_file_content")
    Long sumFileSize();
}
