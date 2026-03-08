// Input: MyBatis-Plus、Archive DTO、Archive Entity
// Output: ArchiveFacade 接口
// Pos: archivecore/app
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.archivecore.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveCreateRequest;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveUpdateRequest;

import java.util.List;

public interface ArchiveFacade {
    Page<Archive> getArchives(int page, int limit, String search, String status, String categoryCode,
                              String orgId, String uniqueBizId, String subType, String fondsNo);

    Archive getArchiveById(String id);

    List<ArcFileContent> getArchiveFiles(String archiveId);

    List<Archive> getRecentArchives(int limit);

    Archive createArchive(ArchiveCreateRequest request, String userId);

    void updateArchive(String id, ArchiveUpdateRequest request);

    void deleteArchive(String id);
}
