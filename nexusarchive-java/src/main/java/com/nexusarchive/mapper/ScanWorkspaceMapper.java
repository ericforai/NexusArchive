// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: ScanWorkspaceMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ScanWorkspace;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 扫描工作区 Mapper
 */
@Mapper
public interface ScanWorkspaceMapper extends BaseMapper<ScanWorkspace> {

    /**
     * 查询用户的草稿列表（按创建时间倒序）
     */
    @Select("SELECT * FROM scan_workspace WHERE user_id = #{userId} AND submit_status = 'draft' ORDER BY created_at DESC")
    List<ScanWorkspace> findDraftsByUserId(@Param("userId") String userId);

    /**
     * 查询指定会话的所有扫描记录
     */
    @Select("SELECT * FROM scan_workspace WHERE session_id = #{sessionId} ORDER BY created_at DESC")
    List<ScanWorkspace> findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 更新OCR识别结果
     */
    @Update("UPDATE scan_workspace SET ocr_status = #{status}, ocr_result = #{result}, overall_score = #{score}, updated_at = NOW() WHERE id = #{id}")
    int updateOcrResult(@Param("id") Long id, @Param("status") String status,
                       @Param("result") String result, @Param("score") Integer score);

    /**
     * 标记为已提交
     */
    @Update("UPDATE scan_workspace SET submit_status = 'submitted', archive_id = #{archiveId}, submitted_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int markAsSubmitted(@Param("id") Long id, @Param("archiveId") String archiveId);

    /**
     * 查询待OCR识别的任务列表
     */
    @Select("SELECT * FROM scan_workspace WHERE ocr_status = 'pending' ORDER BY created_at ASC LIMIT #{limit}")
    List<ScanWorkspace> findPendingOcrTasks(@Param("limit") int limit);

    /**
     * 查询待审核的列表
     */
    @Select("SELECT * FROM scan_workspace WHERE ocr_status = 'review' AND user_id = #{userId} ORDER BY created_at DESC")
    List<ScanWorkspace> findReviewByUserId(@Param("userId") String userId);

    /**
     * 统计用户草稿数量
     */
    @Select("SELECT COUNT(*) FROM scan_workspace WHERE user_id = #{userId} AND submit_status = 'draft'")
    int countDraftsByUserId(@Param("userId") String userId);

    /**
     * 删除指定会话的所有草稿
     */
    @Delete("DELETE FROM scan_workspace WHERE session_id = #{sessionId} AND submit_status = 'draft'")
    int deleteDraftsBySessionId(@Param("sessionId") String sessionId);
}
