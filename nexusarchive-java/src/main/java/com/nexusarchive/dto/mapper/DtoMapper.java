// Input: Entity classes、Lombok
// Output: DtoMapper 类
// Pos: 数据传输对象映射器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.mapper;

import com.nexusarchive.dto.response.*;
import com.nexusarchive.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO 映射器
 * <p>
 * 负责将 Entity 转换为 Response DTO，避免 Controller 直接返回 Entity
 * </p>
 * <p>
 * 重要原则：
 * <ul>
 *   <li>不包含敏感字段（passwordHash, salt, secret 等）</li>
 *   <li>不包含大字段（二进制内容、长 JSON）</li>
 *   <li>只包含必要的业务字段</li>
 * </ul>
 * </p>
 */
@Component
public class DtoMapper {

    // ===== Archive =====

    /**
     * Archive -> ArchiveResponse
     * 使用 ArchiveResponse.fromEntity 静态方法
     */
    public ArchiveResponse toArchiveResponse(Archive entity) {
        if (entity == null) {
            return null;
        }
        return ArchiveResponse.fromEntity(entity);
    }

    /**
     * List<Archive> -> List<ArchiveResponse>
     */
    public List<ArchiveResponse> toArchiveResponseList(List<Archive> entities) {
        return entities.stream()
                .map(this::toArchiveResponse)
                .collect(Collectors.toList());
    }

    /**
     * Page<Archive> -> Page<ArchiveResponse>
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<ArchiveResponse> toArchiveResponsePage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Archive> entityPage) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ArchiveResponse> dtoPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal()
            );
        dtoPage.setRecords(toArchiveResponseList(entityPage.getRecords()));
        return dtoPage;
    }

    // ===== ArcFileContent =====

    /**
     * ArcFileContent -> ArchiveFileResponse
     * 注意：不包含二进制大字段 timestampToken 和 signValue
     */
    public ArchiveFileResponse toArchiveFileResponse(ArcFileContent entity) {
        if (entity == null) {
            return null;
        }
        ArchiveFileResponse dto = new ArchiveFileResponse();
        dto.setId(entity.getId());
        dto.setArchivalCode(entity.getArchivalCode());
        dto.setFileName(entity.getFileName());
        dto.setFileType(entity.getFileType());
        dto.setFileSize(entity.getFileSize());
        dto.setFileHash(entity.getFileHash());
        dto.setHashAlgorithm(entity.getHashAlgorithm());
        dto.setItemId(entity.getItemId());
        dto.setOriginalHash(entity.getOriginalHash());
        dto.setCurrentHash(entity.getCurrentHash());
        dto.setPreArchiveStatus(entity.getPreArchiveStatus());
        dto.setFiscalYear(entity.getFiscalYear());
        dto.setVoucherType(entity.getVoucherType());
        dto.setCreator(entity.getCreator());
        dto.setFondsCode(entity.getFondsCode());
        dto.setSourceSystem(entity.getSourceSystem());
        dto.setBusinessDocNo(entity.getBusinessDocNo());
        dto.setErpVoucherNo(entity.getErpVoucherNo());
        dto.setBatchId(entity.getBatchId());
        dto.setSequenceInBatch(entity.getSequenceInBatch());
        dto.setSummary(entity.getSummary());
        dto.setVoucherWord(entity.getVoucherWord());
        dto.setDocDate(entity.getDocDate() != null ? entity.getDocDate().toString() : null);
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setCheckedTime(entity.getCheckedTime());
        dto.setArchivedTime(entity.getArchivedTime());
        return dto;
    }

    /**
     * List<ArcFileContent> -> List<ArchiveFileResponse>
     */
    public List<ArchiveFileResponse> toArchiveFileResponseList(List<ArcFileContent> entities) {
        return entities.stream()
                .map(this::toArchiveFileResponse)
                .collect(Collectors.toList());
    }

    // ===== SysAuditLog =====

    /**
     * SysAuditLog -> AuditLogResponse
     * 使用 AuditLogResponse.fromEntity 静态方法
     */
    public AuditLogResponse toAuditLogResponse(SysAuditLog entity) {
        if (entity == null) {
            return null;
        }
        return AuditLogResponse.fromEntity(entity);
    }

    /**
     * Page<SysAuditLog> -> Page<AuditLogResponse>
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<AuditLogResponse> toAuditLogResponsePage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysAuditLog> entityPage) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AuditLogResponse> dtoPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal()
            );
        dtoPage.setRecords(entityPage.getRecords().stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    // ===== Volume =====

    /**
     * Volume -> VolumeResponse
     */
    public VolumeResponse toVolumeResponse(Volume entity) {
        if (entity == null) {
            return null;
        }
        VolumeResponse dto = new VolumeResponse();
        dto.setId(entity.getId());
        dto.setVolumeCode(entity.getVolumeCode());
        dto.setTitle(entity.getTitle());
        dto.setFondsNo(entity.getFondsNo());
        dto.setFiscalYear(entity.getFiscalYear());
        dto.setFiscalPeriod(entity.getFiscalPeriod());
        dto.setRetentionPeriod(entity.getRetentionPeriod());
        dto.setStatus(entity.getStatus());
        dto.setArchiveCount(entity.getFileCount());
        dto.setReviewerId(entity.getReviewedBy());
        dto.setReviewTime(entity.getReviewedAt());
        dto.setArchivedTime(entity.getArchivedAt());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedTime(entity.getLastModifiedTime());
        return dto;
    }

    /**
     * Page<Volume> -> Page<VolumeResponse>
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<VolumeResponse> toVolumeResponsePage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Volume> entityPage) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<VolumeResponse> dtoPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal()
            );
        dtoPage.setRecords(entityPage.getRecords().stream()
                .map(this::toVolumeResponse)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    // ===== Destruction =====

    /**
     * Destruction -> DestructionResponse
     */
    public DestructionResponse toDestructionResponse(Destruction entity) {
        if (entity == null) {
            return null;
        }
        DestructionResponse dto = new DestructionResponse();
        dto.setId(entity.getId());
        dto.setArchiveCount(entity.getArchiveCount());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        dto.setApplicantId(entity.getApplicantId());
        dto.setApplicantName(entity.getApplicantName());
        dto.setApplyTime(entity.getCreatedTime());
        dto.setApproverId(entity.getApproverId());
        dto.setApproverName(entity.getApproverName());
        dto.setApprovalComment(entity.getApprovalComment());
        dto.setApproveTime(entity.getApprovalTime());
        dto.setExecuteTime(entity.getExecutionTime());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedTime(entity.getLastModifiedTime());
        return dto;
    }

    /**
     * Page<Destruction> -> Page<DestructionResponse>
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<DestructionResponse> toDestructionResponsePage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Destruction> entityPage) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DestructionResponse> dtoPage =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal()
            );
        dtoPage.setRecords(entityPage.getRecords().stream()
                .map(this::toDestructionResponse)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    // ===== User =====

    /**
     * User -> UserResponse
     * 注意：不包含 passwordHash 字段
     */
    public UserResponse toUserResponse(User entity) {
        if (entity == null) {
            return null;
        }
        UserResponse dto = new UserResponse();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setFullName(entity.getFullName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setAvatar(entity.getAvatar());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    /**
     * List<User> -> List<UserResponse>
     */
    public List<UserResponse> toUserResponseList(List<User> entities) {
        return entities.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    // ===== SysEntity (法人实体) =====

    /**
     * SysEntity -> EntityResponse
     * 注意：不包含 deleted 等内部字段
     * 字段映射：SysEntity.taxId -> EntityResponse.creditCode
     */
    public EntityResponse toEntityResponse(SysEntity entity) {
        if (entity == null) {
            return null;
        }
        EntityResponse dto = new EntityResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCreditCode(entity.getTaxId());
        dto.setRegisteredAddress(entity.getAddress());
        dto.setContactPerson(entity.getContactPerson());
        dto.setContactPhone(entity.getContactPhone());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getDescription());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedTime(entity.getUpdatedTime());
        return dto;
    }

    /**
     * List<SysEntity> -> List<EntityResponse>
     */
    public List<EntityResponse> toEntityResponseList(List<SysEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(this::toEntityResponse)
                .collect(Collectors.toList());
    }

    /**
     * Page<SysEntity> -> Page<EntityResponse>
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<EntityResponse> toEntityResponsePage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysEntity> entityPage) {
        if (entityPage == null) {
            return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<EntityResponse> dtoPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        entityPage.getCurrent(),
                        entityPage.getSize(),
                        entityPage.getTotal());
        dtoPage.setRecords(toEntityResponseList(entityPage.getRecords()));
        return dtoPage;
    }

    // ===== IngestRequestStatus =====

    /**
     * IngestRequestStatus -> IngestRequestStatusResponse
     * 注意：只映射存在的字段
     */
    public IngestRequestStatusResponse toIngestRequestStatusResponse(IngestRequestStatus entity) {
        if (entity == null) {
            return null;
        }
        IngestRequestStatusResponse dto = new IngestRequestStatusResponse();
        dto.setId(entity.getRequestId());
        dto.setStatus(entity.getStatus());
        dto.setErrorMessage(entity.getMessage());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedTime(entity.getUpdatedTime());
        return dto;
    }

    // ===== ArchiveAttachment =====

    /**
     * ArchiveAttachment -> ArchiveAttachmentResponse
     * 注意：不包含内部审计字段
     */
    public ArchiveAttachmentResponse toArchiveAttachmentResponse(ArchiveAttachment entity) {
        if (entity == null) {
            return null;
        }
        ArchiveAttachmentResponse dto = new ArchiveAttachmentResponse();
        dto.setId(entity.getId());
        dto.setArchiveId(entity.getArchiveId());
        dto.setFileId(entity.getFileId());
        dto.setAttachmentType(entity.getAttachmentType());
        dto.setRemarks(entity.getRelationDesc());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedTime(entity.getCreatedTime());
        return dto;
    }

    /**
     * List<ArchiveAttachment> -> List<ArchiveAttachmentResponse>
     */
    public List<ArchiveAttachmentResponse> toArchiveAttachmentResponseList(List<ArchiveAttachment> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(this::toArchiveAttachmentResponse)
                .collect(Collectors.toList());
    }
}
