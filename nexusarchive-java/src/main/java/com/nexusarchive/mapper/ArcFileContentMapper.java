// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: ArcFileContentMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ArcFileContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArcFileContentMapper extends BaseMapper<ArcFileContent> {
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM arc_file_content")
    Long sumFileSize();

    @Select("SELECT afc.id, afc.file_name, afc.storage_path, afc.file_size, afc.file_type, aa.attachment_type as voucher_type, afc.fonds_code " +
            "FROM acc_archive_attachment aa " +
            "JOIN arc_file_content afc ON aa.file_id = afc.id " +
            "WHERE aa.archive_id = #{archiveId}")
    List<ArcFileContent> selectAttachmentsByArchiveId(@Param("archiveId") String archiveId);
}
