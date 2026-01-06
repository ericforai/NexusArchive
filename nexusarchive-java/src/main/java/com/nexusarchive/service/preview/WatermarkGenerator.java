// Input: Spring Security, Java Standard Library
// Output: WatermarkGenerator
// Pos: Service Layer

package com.nexusarchive.service.preview;

import com.nexusarchive.dto.PreviewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 水印生成器
 *
 * 负责生成预览水印的文本和元数据
 */
@Service
@Slf4j
public class WatermarkGenerator {

    private static final double WATERMARK_OPACITY = 0.3;
    private static final int WATERMARK_ROTATE = -45;

    /**
     * 生成完整的水印元数据
     *
     * @param traceId 追踪ID
     * @param fondsNo 全宗号
     * @return 水印元数据
     */
    public PreviewResponse.WatermarkMetadata generateWatermarkMetadata(String traceId, String fondsNo) {
        PreviewResponse.WatermarkMetadata watermark = new PreviewResponse.WatermarkMetadata();
        watermark.setText(generateWatermarkText(traceId));
        watermark.setSubtext(generateWatermarkSubtext(traceId, fondsNo));
        watermark.setOpacity(WATERMARK_OPACITY);
        watermark.setRotate(WATERMARK_ROTATE);
        return watermark;
    }

    /**
     * 生成水印主文本（用户名 + 时间戳 + TraceID）
     *
     * @param traceId 追踪ID
     * @return 水印文本
     */
    public String generateWatermarkText(String traceId) {
        String username = getCurrentUsername();
        String timestamp = LocalDateTime.now().toString();
        return String.format("%s %s %s", username, timestamp, traceId);
    }

    /**
     * 生成水印副文本（TraceID + FondsNo）
     *
     * @param traceId 追踪ID
     * @param fondsNo 全宗号
     * @return 水印副文本
     */
    public String generateWatermarkSubtext(String traceId, String fondsNo) {
        return String.format("%s %s", traceId, fondsNo);
    }

    /**
     * 生成追踪ID
     *
     * @return UUID格式的追踪ID
     */
    public String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "Unknown";
    }
}
