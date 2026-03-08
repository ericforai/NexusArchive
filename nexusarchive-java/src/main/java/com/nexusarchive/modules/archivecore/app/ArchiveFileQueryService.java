// Input: MyBatis-Plus、Spring Framework、Archive/ArcFileContent Mapper
// Output: ArchiveFileQueryService 类
// Pos: archivecore/app
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.archivecore.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveFileQueryService {

    private final ArchiveMapper archiveMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final VoucherRelationMapper voucherRelationMapper;
    private final DataScopeService dataScopeService;

    public List<ArcFileContent> getFilesByArchiveId(String archiveId) {
        Archive archive = requireAccessibleArchive(archiveId);
        List<ArcFileContent> result = new ArrayList<>();

        LambdaQueryWrapper<ArcFileContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArcFileContent::getItemId, archive.getId());
        wrapper.orderByAsc(ArcFileContent::getCreatedTime);
        result.addAll(arcFileContentMapper.selectList(wrapper));

        try {
            result.addAll(arcFileContentMapper.selectAttachmentsByArchiveId(archive.getId()));
        } catch (Exception e) {
            log.warn("Failed to query attachments from acc_archive_attachment: {}", e.getMessage());
        }

        try {
            List<VoucherRelation> relations = voucherRelationMapper.selectList(
                    new LambdaQueryWrapper<VoucherRelation>()
                            .eq(VoucherRelation::getAccountingVoucherId, archive.getId())
                            .eq(VoucherRelation::getDeleted, 0)
            );

            if (!relations.isEmpty()) {
                List<String> originalVoucherIds = relations.stream()
                        .map(VoucherRelation::getOriginalVoucherId)
                        .collect(Collectors.toList());

                if (!originalVoucherIds.isEmpty()) {
                    result.addAll(arcFileContentMapper.selectList(
                            new LambdaQueryWrapper<ArcFileContent>()
                                    .in(ArcFileContent::getItemId, originalVoucherIds)
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to query related original voucher files: {}", e.getMessage());
        }

        return result;
    }

    private Archive requireAccessibleArchive(String idOrCode) {
        Archive archive = archiveMapper.selectById(idOrCode);
        if (archive == null) {
            LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Archive::getArchiveCode, idOrCode);
            archive = archiveMapper.selectOne(wrapper);
        }

        if (archive == null) {
            throw new BusinessException(404, ErrorCode.ARCHIVE_NOT_FOUND.getMessage(idOrCode));
        }

        DataScopeContext scope = dataScopeService.resolve();
        if (!dataScopeService.canAccessArchive(archive, scope)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION_TO_VIEW_ARCHIVE);
        }
        return archive;
    }
}
