// Input: Spring Framework, JDK
// Output: PoolService 接口
// Pos: 业务逻辑接口层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.PoolItemDto;
import com.nexusarchive.dto.search.CandidateSearchRequest;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;

import java.util.List;
import java.util.Map;

/**
 * 电子凭证池服务接口
 */
public interface PoolService {

    /**
     * 搜索可关联的候选凭证
     *
     * @param request 搜索条件
     * @return 候选凭证列表
     */
    List<PoolItemDto> searchCandidates(CandidateSearchRequest request);

    /**
     * 根据 ID 获取文件
     */
    ArcFileContent getFileById(String id);

    /**
     * 查询电子凭证池列表
     * @param category 门类过滤器 (可选)
     * @return 凭证池列表
     */
    List<PoolItemDto> listPoolItems(String category);

    /**
     * 按状态查询预归档文件
     * @param status 状态
     * @param category 门类过滤器 (可选)
     * @return 文件列表
     */
    List<PoolItemDto> listByStatus(String status, String category);

    /**
     * 统计各状态数量
     * @param category 门类过滤 (DA/T 94 标准码: VOUCHER/AC01/AC02/AC03/AC04), 可为 null
     */
    Map<String, Long> getStatusStats(String category);

    /**
     * 更新预归档状态
     */
    void updateStatus(String id, String status);

    /**
     * 查询待检测文件
     */
    List<ArcFileContent> listPendingCheckFiles();

    /**
     * 根据业务单据号查询旧附件
     */
    List<ArcFileContent> getLegacyAttachments(String businessDocNo);

    /**
     * 根据文件 ID 获取元数据
     */
    ArcFileMetadataIndex getMetadataByFileId(String fileId);

    /**
     * 转换实体为 DTO
     */
    PoolItemDto convertToPoolItemDto(ArcFileContent fileContent);

    /**
     * 清理演示数据（通过文件哈希前缀识别）
     * @return 删除的数量
     */
    int cleanupDemoData();

    /**
     * 插入演示文件
     */
    void insertDemoFile(ArcFileContent content);

    /**
     * 插入演示元数据
     */
    void insertDemoMetadata(ArcFileMetadataIndex metadata);
}
