package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface for reading Archive data.
 * Segregated from the concrete implementation to support Dependency Inversion.
 */
public interface ArchiveReadService {

    /**
     * 分页查询档案
     *
     * @param page         页码
     * @param limit        每页条数
     * @param search       搜索关键词
     * @param status       状态
     * @param categoryCode 类别号
     * @param orgId        部门ID
     * @param uniqueBizId  唯一业务ID
     * @param subType      子类型
     * @param fondsNo      全宗号(显式过滤，可选)
     * @return 分页结果
     */
    Page<Archive> getArchives(int page, int limit, String search, String status, String categoryCode,
                              String orgId, String uniqueBizId, String subType, String fondsNo);

    /**
     * 根据ID获取档案
     *
     * @param id 档案ID
     * @return 档案详情
     */
    Archive getArchiveById(String id);

    /**
     * 根据唯一业务ID获取档案
     *
     * @param uniqueBizId 唯一业务ID
     * @return 档案详情
     */
    Archive getByUniqueBizId(String uniqueBizId);

    /**
     * 获取最近创建的档案
     *
     * @param limit 数量限制
     * @return 档案列表
     */
    List<Archive> getRecentArchives(int limit);

    /**
     * 批量获取档案 (受控)
     *
     * @param ids ID集合
     * @return 档案列表
     */
    List<Archive> getArchivesByIds(Set<String> ids);

    /**
     * 根据部门ID列表获取档案ID列表
     *
     * @param departmentIds 部门ID集合
     * @return 档案ID列表
     */
    List<String> getArchiveIdsByDepartmentIds(Collection<String> departmentIds);

    /**
     * 获取档案关联的文件列表
     *
     * @param archiveId 档案ID
     * @return 文件列表
     */
    List<ArcFileContent> getFilesByArchiveId(String archiveId);

    /**
     * 获取到期档案列表
     */
    com.baomidou.mybatisplus.core.metadata.IPage<Archive> getExpiredArchives(int page, int limit, String fondsNo);
}
