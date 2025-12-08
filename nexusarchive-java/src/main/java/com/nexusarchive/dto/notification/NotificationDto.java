package com.nexusarchive.dto.notification;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDto {
    private String id;
    private String title;
    private String time;
    private String type; // info / warning / success
}
