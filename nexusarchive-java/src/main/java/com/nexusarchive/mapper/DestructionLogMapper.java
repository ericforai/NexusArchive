// Input: MyBatis-Plus BaseMapper
// Output: DestructionLogMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.DestructionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 销毁清册 Mapper
 */
@Mapper
public interface DestructionLogMapper extends BaseMapper<DestructionLog> {
    
    /**
     * 获取最后一条记录的哈希值（用于哈希链）
     */
    @Select("SELECT curr_hash FROM destruction_log " +
            "WHERE fonds_no = #{fondsNo} AND archive_year = #{archiveYear} " +
            "ORDER BY created_at DESC LIMIT 1")
    String getLastHash(String fondsNo, Integer archiveYear);
    
    /**
     * 检查记录是否已存在（防止重复记录）
     */
    @Select("SELECT COUNT(*) FROM destruction_log " +
            "WHERE archive_object_id = #{archiveObjectId} " +
            "AND fonds_no = #{fondsNo} AND archive_year = #{archiveYear}")
    int countByArchive(String archiveObjectId, String fondsNo, Integer archiveYear);
}

