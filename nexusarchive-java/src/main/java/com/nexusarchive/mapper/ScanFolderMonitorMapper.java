// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: ScanFolderMonitorMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ScanFolderMonitor;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 文件夹监控配置 Mapper
 */
@Mapper
public interface ScanFolderMonitorMapper extends BaseMapper<ScanFolderMonitor> {

    /**
     * 查询用户的所有监控配置
     */
    @Select("SELECT * FROM scan_folder_monitor WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<ScanFolderMonitor> findByUserId(@Param("userId") String userId);

    /**
     * 查询所有激活的监控配置
     */
    @Select("SELECT * FROM scan_folder_monitor WHERE is_active = true ORDER BY created_at DESC")
    List<ScanFolderMonitor> findAllActive();

    /**
     * 根据文件夹路径查询监控配置
     */
    @Select("SELECT * FROM scan_folder_monitor WHERE folder_path = #{folderPath}")
    ScanFolderMonitor findByFolderPath(@Param("folderPath") String folderPath);

    /**
     * 激活/停用监控配置
     */
    @Update("UPDATE scan_folder_monitor SET is_active = #{isActive}, updated_at = NOW() WHERE id = #{id}")
    int updateActiveStatus(@Param("id") Long id, @Param("isActive") Boolean isActive);

    /**
     * 查询监控配置数量
     */
    @Select("SELECT COUNT(*) FROM scan_folder_monitor WHERE user_id = #{userId} AND is_active = true")
    int countActiveByUserId(@Param("userId") String userId);
}
