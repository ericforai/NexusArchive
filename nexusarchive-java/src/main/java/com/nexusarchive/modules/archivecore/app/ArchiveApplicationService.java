// Input: Spring Framework、Archive DTO、Archive Service
// Output: ArchiveApplicationService 类
// Pos: archivecore/app
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.archivecore.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveCreateRequest;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveUpdateRequest;
import com.nexusarchive.service.ArchiveReadService;
import com.nexusarchive.service.ArchiveWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveApplicationService implements ArchiveFacade {

    private final ArchiveReadService archiveReadService;
    private final ArchiveWriteService archiveWriteService;
    private final ArchiveFileQueryService archiveFileQueryService;

    @Override
    public Page<Archive> getArchives(int page, int limit, String search, String status, String categoryCode,
                                     String orgId, String uniqueBizId, String subType, String fondsNo) {
        return archiveReadService.getArchives(page, limit, search, status, categoryCode, orgId, uniqueBizId, subType, fondsNo);
    }

    @Override
    public Archive getArchiveById(String id) {
        return archiveReadService.getArchiveById(id);
    }

    @Override
    public List<ArcFileContent> getArchiveFiles(String archiveId) {
        return archiveFileQueryService.getFilesByArchiveId(archiveId);
    }

    @Override
    public List<Archive> getRecentArchives(int limit) {
        return archiveReadService.getRecentArchives(limit);
    }

    @Override
    public Archive createArchive(ArchiveCreateRequest request, String userId) {
        return archiveWriteService.createArchive(toArchive(request), userId);
    }

    @Override
    public void updateArchive(String id, ArchiveUpdateRequest request) {
        archiveWriteService.updateArchive(id, toArchive(request));
    }

    @Override
    public void deleteArchive(String id) {
        archiveWriteService.deleteArchive(id);
    }

    private Archive toArchive(ArchiveCreateRequest request) {
        Archive archive = new Archive();
        BeanUtils.copyProperties(request, archive);
        return archive;
    }

    private Archive toArchive(ArchiveUpdateRequest request) {
        Archive archive = new Archive();
        BeanUtils.copyProperties(request, archive);
        return archive;
    }
}
