// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: NotificationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.constants.DateFormat;
import com.nexusarchive.dto.notification.NotificationDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import com.nexusarchive.service.NotificationService;
import com.nexusarchive.security.FondsContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final IngestRequestStatusMapper ingestRequestStatusMapper;
    private final ArchiveMapper archiveMapper;
    private final DataScopeService dataScopeService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern(DateFormat.DATETIME);

    @Override
    public List<NotificationDto> listLatest() {
        List<NotificationDto> items = new ArrayList<>();
        String currentFondsNo = FondsContext.getCurrentFondsNo();

        // 1) 最新处理任务（待处理/处理中/失败）- 添加全宗过滤
        LambdaQueryWrapper<IngestRequestStatus> taskWrapper = new LambdaQueryWrapper<>();
        if (currentFondsNo != null && !currentFondsNo.isEmpty()) {
            taskWrapper.eq(IngestRequestStatus::getFondsNo, currentFondsNo);
        }
        taskWrapper.orderByDesc(IngestRequestStatus::getUpdatedTime)
                .last("LIMIT 5");
        List<IngestRequestStatus> tasks = ingestRequestStatusMapper.selectList(taskWrapper);
        for (IngestRequestStatus task : tasks) {
            String type = "info";
            if ("FAILED".equalsIgnoreCase(task.getStatus())) {
                type = "warning";
            } else if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
                type = "success";
            }
            items.add(NotificationDto.builder()
                    .id(task.getRequestId())
                    .title("接收任务 " + task.getRequestId() + " 状态: " + task.getStatus())
                    .time(formatDateTime(task.getUpdatedTime(), task.getCreatedTime()))
                    .type(type)
                    .build());
        }

        // 2) 最新归档记录 - 使用 DataScopeService 进行全宗过滤
        DataScopeContext scope = dataScopeService.resolve();
        LambdaQueryWrapper<Archive> archiveWrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyArchiveScope(archiveWrapper, scope);
        archiveWrapper.orderByDesc(Archive::getCreatedTime)
                .last("LIMIT 3");
        List<Archive> archives = archiveMapper.selectList(archiveWrapper);
        for (Archive archive : archives) {
            items.add(NotificationDto.builder()
                    .id(archive.getId() != null ? archive.getId().toString() : UUID.randomUUID().toString())
                    .title("新归档: " + archive.getArchiveCode() + " - " + archive.getTitle())
                    .time(formatDateTime(archive.getLastModifiedTime(), archive.getCreatedTime()))
                    .type("success")
                    .build());
        }

        return items;
    }

    private String formatDateTime(java.time.LocalDateTime primary, java.time.LocalDateTime fallback) {
        java.time.LocalDateTime dt = primary != null ? primary : fallback;
        return dt != null ? dt.format(TIME_FMT) : "";
    }
}
