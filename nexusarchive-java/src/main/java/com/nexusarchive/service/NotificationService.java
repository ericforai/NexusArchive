package com.nexusarchive.service;

import com.nexusarchive.dto.notification.NotificationDto;

import java.util.List;

public interface NotificationService {
    List<NotificationDto> listLatest();
}
