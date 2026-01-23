// Input: Spring Framework, Local Entity
// Output: ScanWorkspaceService Interface
// Pos: Service Interface Layer

package com.nexusarchive.service;

import com.nexusarchive.entity.ScanWorkspace;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 扫描工作区服务接口
 *
 * <p>功能：</p>
 * <ol>
 *   <li>文件上传管理 (扫描/上传/文件夹)</li>
 *   <li>OCR 识别处理 (Tesseract/Paddle/百度)</li>
 *   <li>草稿数据查询</li>
 *   <li>提交预归档池</li>
 *   <li>工作区清理</li>
 * </ol>
 */
public interface ScanWorkspaceService {

    /**
     * 获取用户的草稿列表
     *
     * @param userId 用户ID
     * @return 草稿列表 (按创建时间倒序)
     */
    List<ScanWorkspace> getUserWorkspaceFiles(String userId);

    /**
     * 上传文件到工作区
     *
     * @param file 上传的文件
     * @param uploadSource 上传来源 (scan/upload/folder)
     * @param sessionId 会话ID (用于批量操作)
     * @param userId 用户ID
     * @return 创建的工作区记录
     */
    ScanWorkspace uploadFile(MultipartFile file, String uploadSource, String sessionId, String userId);

    /**
     * 触发 OCR 识别
     *
     * @param id 工作区记录ID
     * @param engine OCR 引擎 (tesseract/paddle/baidu)
     * @param userId 用户ID
     */
    void triggerOcr(Long id, String engine, String userId);

    /**
     * 更新工作区记录 (OCR 结果、人工编辑)
     *
     * @param workspace 工作区记录
     * @param userId 用户ID
     * @return 更新后的工作区记录
     */
    ScanWorkspace updateWorkspace(ScanWorkspace workspace, String userId);

    /**
     * 提交到预归档池
     *
     * @param id 工作区记录ID
     * @param userId 用户ID
     * @return 提交结果 (包含档案ID和消息)
     */
    SubmitResult submitToPreArchive(Long id, String userId);

    /**
     * 删除工作区记录
     *
     * @param id 工作区记录ID
     * @param userId 用户ID
     */
    void deleteWorkspace(Long id, String userId);

    /**
     * 创建会话ID（用于移动端扫码）
     *
     * @param userId 用户ID
     * @return 会话ID
     */
    String createSession(String userId);

    /**
     * 验证会话是否有效
     *
     * @param sessionId 会话ID
     * @return 是否有效
     */
    boolean validateSession(String sessionId);

    /**
     * 获取文件（物理文件）
     *
     * @param id 工作区记录ID
     * @param userId 用户ID
     * @return 物理文件对象
     */
    java.io.File getFile(Long id, String userId);

    /**
     * 提交结果
     *
     * @param archiveId 关联的档案ID
     * @param message 结果消息
     */
    record SubmitResult(String archiveId, String message) {}
}
