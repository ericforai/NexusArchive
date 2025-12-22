// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: ArchiveAttachmentMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ArchiveAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 档案附件关联 Mapper
 */
@Mapper
public interface ArchiveAttachmentMapper extends BaseMapper<ArchiveAttachment> {

    /**
     * 按档案ID查询关联的附件
     */
    @Select("SELECT * FROM acc_archive_attachment WHERE archive_id = #{archiveId}")
    List<ArchiveAttachment> selectByArchiveId(String archiveId);

    /**
     * 按文件ID查询关联的档案
     */
    @Select("SELECT * FROM acc_archive_attachment WHERE file_id = #{fileId}")
    List<ArchiveAttachment> selectByFileId(String fileId);
}
