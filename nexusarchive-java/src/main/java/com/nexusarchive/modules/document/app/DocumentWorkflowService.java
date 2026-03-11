package com.nexusarchive.modules.document.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.modules.document.api.dto.DocumentAssignmentDto;
import com.nexusarchive.modules.document.api.dto.DocumentAssignmentRequest;
import com.nexusarchive.modules.document.api.dto.DocumentLockDto;
import com.nexusarchive.modules.document.api.dto.DocumentLockRequest;
import com.nexusarchive.modules.document.api.dto.DocumentReminderDto;
import com.nexusarchive.modules.document.api.dto.DocumentReminderRequest;
import com.nexusarchive.modules.document.api.dto.DocumentSectionDto;
import com.nexusarchive.modules.document.api.dto.DocumentSectionUpdateRequest;
import com.nexusarchive.modules.document.api.dto.DocumentVersionCreateRequest;
import com.nexusarchive.modules.document.api.dto.DocumentVersionDto;
import com.nexusarchive.modules.document.domain.DocumentAssignmentEntity;
import com.nexusarchive.modules.document.domain.DocumentLockEntity;
import com.nexusarchive.modules.document.domain.DocumentReminderEntity;
import com.nexusarchive.modules.document.domain.DocumentSectionEntity;
import com.nexusarchive.modules.document.domain.DocumentVersionEntity;
import com.nexusarchive.modules.document.infra.mapper.DocumentAssignmentMapper;
import com.nexusarchive.modules.document.infra.mapper.DocumentLockMapper;
import com.nexusarchive.modules.document.infra.mapper.DocumentReminderMapper;
import com.nexusarchive.modules.document.infra.mapper.DocumentSectionMapper;
import com.nexusarchive.modules.document.infra.mapper.DocumentVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentWorkflowService {

    private final DocumentSectionMapper sectionMapper;
    private final DocumentAssignmentMapper assignmentMapper;
    private final DocumentLockMapper lockMapper;
    private final DocumentReminderMapper reminderMapper;
    private final DocumentVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    public DocumentSectionDto getSection(String projectId, String sectionId) {
        DocumentSectionEntity section = getExistingSection(projectId, sectionId);
        return toSectionDto(section);
    }

    @Transactional
    public DocumentSectionDto upsertSection(String projectId,
                                           String sectionId,
                                           DocumentSectionUpdateRequest request,
                                           String userId) {
        DocumentSectionEntity existing = sectionMapper.selectOne(new LambdaQueryWrapper<DocumentSectionEntity>()
                .eq(DocumentSectionEntity::getProjectId, projectId)
                .eq(DocumentSectionEntity::getId, sectionId)
                .last("LIMIT 1"));

        if (existing == null) {
            existing = DocumentSectionEntity.builder()
                    .id(sectionId)
                    .projectId(projectId)
                    .title(request.getTitle())
                    .content(request.getContent())
                    .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build();
            sectionMapper.insert(existing);
        } else {
            existing.setTitle(request.getTitle());
            existing.setContent(request.getContent());
            existing.setSortOrder(request.getSortOrder() == null ? existing.getSortOrder() : request.getSortOrder());
            existing.setUpdatedBy(userId);
            existing.setUpdatedAt(LocalDateTime.now());
            sectionMapper.updateById(existing);
        }
        return toSectionDto(existing);
    }

    @Transactional
    public DocumentAssignmentDto createAssignment(String projectId,
                                                  DocumentAssignmentRequest request,
                                                  String userId) {
        ensureSectionExists(projectId, request.getSectionId());
        deactivateExistingAssignments(projectId, request.getSectionId());

        DocumentAssignmentEntity entity = DocumentAssignmentEntity.builder()
                .projectId(projectId)
                .sectionId(request.getSectionId())
                .assigneeId(request.getAssigneeId())
                .assigneeName(request.getAssigneeName())
                .assignedBy(userId)
                .note(request.getNote())
                .active(Boolean.TRUE.equals(request.getActive()))
                .build();
        assignmentMapper.insert(entity);
        return toAssignmentDto(entity);
    }

    @Transactional
    public DocumentLockDto createLock(String projectId,
                                      DocumentLockRequest request,
                                      String userId,
                                      String userName) {
        ensureSectionExists(projectId, request.getSectionId());
        deactivateExistingLocks(projectId, request.getSectionId());

        DocumentLockEntity entity = DocumentLockEntity.builder()
                .projectId(projectId)
                .sectionId(request.getSectionId())
                .lockedBy(userId)
                .lockedByName(userName)
                .reason(request.getReason())
                .active(Boolean.TRUE.equals(request.getActive()))
                .expiresAt(request.getExpiresAt())
                .build();
        lockMapper.insert(entity);
        return toLockDto(entity);
    }

    @Transactional
    public DocumentReminderDto createReminder(String projectId,
                                              DocumentReminderRequest request,
                                              String userId) {
        ensureSectionExists(projectId, request.getSectionId());

        DocumentReminderEntity entity = DocumentReminderEntity.builder()
                .projectId(projectId)
                .sectionId(request.getSectionId())
                .message(request.getMessage())
                .remindAt(request.getRemindAt())
                .recipientId(request.getRecipientId())
                .recipientName(request.getRecipientName())
                .createdBy(userId)
                .delivered(Boolean.FALSE)
                .build();
        reminderMapper.insert(entity);
        return toReminderDto(entity);
    }

    @Transactional
    public DocumentVersionDto createVersion(String projectId,
                                            DocumentVersionCreateRequest request,
                                            String userId) {
        DocumentVersionEntity entity = DocumentVersionEntity.builder()
                .projectId(projectId)
                .versionName(request.getVersionName())
                .description(request.getDescription())
                .createdBy(userId)
                .snapshotPayload(buildSnapshot(projectId))
                .build();
        versionMapper.insert(entity);
        return toVersionDto(entity);
    }

    @Transactional
    public DocumentVersionDto rollbackVersion(String projectId, String versionId, String userId) {
        DocumentVersionEntity version = versionMapper.selectById(versionId);
        if (version == null || !projectId.equals(version.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        restoreSnapshot(projectId, version.getSnapshotPayload());
        version.setRolledBackBy(userId);
        version.setRolledBackAt(LocalDateTime.now());
        versionMapper.updateById(version);
        return toVersionDto(version);
    }

    public List<DocumentVersionDto> listVersions(String projectId) {
        return versionMapper.findByProjectId(projectId).stream().map(this::toVersionDto).toList();
    }

    private void ensureSectionExists(String projectId, String sectionId) {
        sectionMapper.selectOne(new LambdaQueryWrapper<DocumentSectionEntity>()
                .eq(DocumentSectionEntity::getProjectId, projectId)
                .eq(DocumentSectionEntity::getId, sectionId)
                .last("LIMIT 1"));
        getExistingSection(projectId, sectionId);
    }

    private DocumentSectionEntity getExistingSection(String projectId, String sectionId) {
        DocumentSectionEntity section = sectionMapper.selectOne(new LambdaQueryWrapper<DocumentSectionEntity>()
                .eq(DocumentSectionEntity::getProjectId, projectId)
                .eq(DocumentSectionEntity::getId, sectionId)
                .last("LIMIT 1"));
        if (section == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return section;
    }

    private void deactivateExistingAssignments(String projectId, String sectionId) {
        List<DocumentAssignmentEntity> existing = assignmentMapper.findByProjectId(projectId).stream()
                .filter(item -> sectionId.equals(item.getSectionId()) && Boolean.TRUE.equals(item.getActive()))
                .toList();
        for (DocumentAssignmentEntity item : existing) {
            item.setActive(Boolean.FALSE);
            assignmentMapper.updateById(item);
        }
    }

    private void deactivateExistingLocks(String projectId, String sectionId) {
        List<DocumentLockEntity> existing = lockMapper.findByProjectId(projectId).stream()
                .filter(item -> sectionId.equals(item.getSectionId()) && Boolean.TRUE.equals(item.getActive()))
                .toList();
        for (DocumentLockEntity item : existing) {
            item.setActive(Boolean.FALSE);
            lockMapper.updateById(item);
        }
    }

    private String buildSnapshot(String projectId) {
        Map<String, Object> snapshot = Map.of(
                "sections", sectionMapper.findByProjectId(projectId),
                "assignments", assignmentMapper.findByProjectId(projectId),
                "locks", lockMapper.findByProjectId(projectId),
                "reminders", reminderMapper.findByProjectId(projectId)
        );
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new BusinessException("无法序列化文档版本快照", e);
        }
    }

    private void restoreSnapshot(String projectId, String snapshotPayload) {
        try {
            Map<String, Object> snapshot = objectMapper.readValue(snapshotPayload, new TypeReference<>() {});
            reminderMapper.deleteByProjectId(projectId);
            lockMapper.deleteByProjectId(projectId);
            assignmentMapper.deleteByProjectId(projectId);
            sectionMapper.deleteByProjectId(projectId);

            List<DocumentSectionEntity> sections = convert(snapshot.get("sections"), new TypeReference<>() {});
            List<DocumentAssignmentEntity> assignments = convert(snapshot.get("assignments"), new TypeReference<>() {});
            List<DocumentLockEntity> locks = convert(snapshot.get("locks"), new TypeReference<>() {});
            List<DocumentReminderEntity> reminders = convert(snapshot.get("reminders"), new TypeReference<>() {});

            sections.forEach(sectionMapper::insert);
            assignments.forEach(assignmentMapper::insert);
            locks.forEach(lockMapper::insert);
            reminders.forEach(reminderMapper::insert);
        } catch (JsonProcessingException e) {
            throw new BusinessException("无法恢复文档版本快照", e);
        }
    }

    private <T> List<T> convert(Object value, TypeReference<List<T>> typeReference) {
        if (value == null) {
            return Collections.emptyList();
        }
        return objectMapper.convertValue(value, typeReference);
    }

    private DocumentSectionDto toSectionDto(DocumentSectionEntity section) {
        DocumentAssignmentEntity assignment = assignmentMapper.findLatestBySectionId(section.getProjectId(), section.getId());
        DocumentLockEntity lock = lockMapper.findLatestBySectionId(section.getProjectId(), section.getId());
        List<DocumentReminderDto> reminders = reminderMapper.findBySectionId(section.getProjectId(), section.getId())
                .stream()
                .map(this::toReminderDto)
                .toList();

        return DocumentSectionDto.builder()
                .id(section.getId())
                .projectId(section.getProjectId())
                .title(section.getTitle())
                .content(section.getContent())
                .sortOrder(section.getSortOrder())
                .createdBy(section.getCreatedBy())
                .updatedBy(section.getUpdatedBy())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .assignment(assignment == null ? null : toAssignmentDto(assignment))
                .lock(lock == null ? null : toLockDto(lock))
                .reminders(reminders)
                .build();
    }

    private DocumentAssignmentDto toAssignmentDto(DocumentAssignmentEntity entity) {
        return DocumentAssignmentDto.builder()
                .id(entity.getId())
                .sectionId(entity.getSectionId())
                .assigneeId(entity.getAssigneeId())
                .assigneeName(entity.getAssigneeName())
                .assignedBy(entity.getAssignedBy())
                .note(entity.getNote())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private DocumentLockDto toLockDto(DocumentLockEntity entity) {
        return DocumentLockDto.builder()
                .id(entity.getId())
                .sectionId(entity.getSectionId())
                .lockedBy(entity.getLockedBy())
                .lockedByName(entity.getLockedByName())
                .reason(entity.getReason())
                .active(entity.getActive())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private DocumentReminderDto toReminderDto(DocumentReminderEntity entity) {
        return DocumentReminderDto.builder()
                .id(entity.getId())
                .sectionId(entity.getSectionId())
                .message(entity.getMessage())
                .remindAt(entity.getRemindAt())
                .recipientId(entity.getRecipientId())
                .recipientName(entity.getRecipientName())
                .createdBy(entity.getCreatedBy())
                .delivered(entity.getDelivered())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private DocumentVersionDto toVersionDto(DocumentVersionEntity entity) {
        return DocumentVersionDto.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .versionName(entity.getVersionName())
                .description(entity.getDescription())
                .createdBy(entity.getCreatedBy())
                .rolledBackBy(entity.getRolledBackBy())
                .rolledBackAt(entity.getRolledBackAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
