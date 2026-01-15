package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;

/**
 * Interface for writing (modifying) Archive data.
 * Segregated from the concrete implementation to support Dependency Inversion.
 */
public interface ArchiveWriteService {

    /**
     * 创建档案
     *
     * @param archive 档案实体
     * @param userId  创建人ID
     * @return 创建后的档案
     */
    Archive createArchive(Archive archive, String userId);

    /**
     * 更新档案
     *
     * @param id      档案ID
     * @param archive 更新的数据
     */
    void updateArchive(String id, Archive archive);

    /**
     * 删除档案
     *
     * @param id 档案ID
     */
    void deleteArchive(String id);
}
