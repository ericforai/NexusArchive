// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库
// Output: ArchiveFileContentService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.nexusarchive.security.FondsContext;

import java.util.List;

/**
 * 档案文件内容服务
 * 处理档案文件内容的查询业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveFileContentService {

    private final ArcFileContentMapper fileContentMapper;
    private final AuditLogService auditLogService;

    /**
     * 根据 item_id 查询单个档案文件内容（取最新）
     *
     * @param itemId 项目 ID
     * @param operatorId 操作人 ID
     * @return 档案文件内容
     */
    public ArcFileContent getFileContentByItemId(String itemId, String operatorId) {
        ArcFileContent content = fileContentMapper.selectOne(
            new LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getItemId, itemId)
                .orderByDesc(ArcFileContent::getCreatedTime)
                .last("LIMIT 1")
        );

        if (content != null) {
            // 数据范围过滤：检查全宗权限
            String currentFonds = FondsContext.getCurrentFondsNo();
            if (currentFonds != null && !currentFonds.isEmpty()) {
                String fondsCode = content.getFondsCode();
                if (fondsCode != null && !currentFonds.equals(fondsCode)) {
                    log.warn("全宗权限不匹配: itemId={}, fondsCode={}, currentFonds={}",
                        itemId, fondsCode, currentFonds);
                    return null;
                }
            }
        }

        // 审计日志
        auditLogService.log(
            operatorId != null ? operatorId : "SYSTEM",
            "USER_" + operatorId,
            "ARCHIVE_FILE_VIEWED",
            "ARCHIVE_FILE",
            content != null ? content.getId() : itemId,
            "SUCCESS",
            "查询档案文件内容: itemId=" + itemId,
            null
        );

        return content;
    }

    /**
     * 根据 ID 查询档案文件内容（应用全宗数据范围过滤）
     *
     * @param fileId 文件 ID
     * @param operatorId 操作人 ID
     * @return 档案文件内容
     */
    public ArcFileContent getFileContentById(String fileId, String operatorId) {
        ArcFileContent content = fileContentMapper.selectById(fileId);

        if (content == null) {
            log.warn("档案文件不存在: fileId={}", fileId);
            return null;
        }

        // 数据范围过滤：检查全宗权限
        String currentFonds = FondsContext.getCurrentFondsNo();
        if (currentFonds != null && !currentFonds.isEmpty()) {
            String fondsCode = content.getFondsCode();
            // 如果文件的 fonds_code 为 null，则允许访问（用于种子数据或未分配全宗的文件）
            // 只有当明确设置了不同的 fonds_code 时才拒绝访问
            if (fondsCode != null && !currentFonds.equals(fondsCode)) {
                log.warn("全宗权限不匹配: fileId={}, fondsCode={}, currentFonds={}",
                    fileId, fondsCode, currentFonds);
                return null;
            }
        }

        // 审计日志
        auditLogService.log(
            operatorId != null ? operatorId : "SYSTEM",
            "USER_" + operatorId,
            "ARCHIVE_FILE_VIEWED",
            "ARCHIVE_FILE",
            fileId,
            "SUCCESS",
            "查询档案文件内容: fileId=" + fileId,
            null
        );

        return content;
    }

    /**
     * 根据 item_id 查询关联的所有档案文件
     *
     * @param itemId 项目 ID
     * @param operatorId 操作人 ID
     * @return 档案文件内容列表
     */
    public List<ArcFileContent> getFilesByItemId(String itemId, String operatorId) {
        List<ArcFileContent> files = fileContentMapper.selectList(
            new LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getItemId, itemId)
        );

        // 审计日志
        auditLogService.log(
            operatorId != null ? operatorId : "SYSTEM",
            "USER_" + operatorId,
            "ARCHIVE_FILES_LISTED",
            "ARCHIVE_FILE",
            itemId,
            "SUCCESS",
            "查询档案关联文件: itemId=" + itemId + ", count=" + files.size(),
            null
        );

        return files;
    }
}
