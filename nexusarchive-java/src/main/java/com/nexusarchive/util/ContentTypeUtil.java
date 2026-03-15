// Input: Spring Framework, Java 标准库
// Output: ContentTypeUtil 类
// Pos: 工具模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.util;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import com.nexusarchive.common.constants.HttpConstants;

/**
 * 文件内容类型工具类
 *
 * 根据文件类型或扩展名确定 HTTP Content-Type
 */
public class ContentTypeUtil {

    private ContentTypeUtil() {
        // Utility class
    }

    /**
     * 根据文件类型和文件名确定内容类型
     *
     * @param fileType 文件类型 (如: PDF, OFD, JPG)
     * @param fileName 文件名 (用于从扩展名推断)
     * @return Content-Type 字符串
     */
    public static String determineContentType(String fileType, String fileName) {
        // 优先使用文件类型
        if (StringUtils.hasText(fileType)) {
            return switch (fileType.toLowerCase()) {
                case "ofd" -> HttpConstants.APPLICATION_OFD;
                case "pdf" -> HttpConstants.APPLICATION_PDF;
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "xml" -> "application/xml";
                case "tiff", "tif" -> "image/tiff";
                default -> "application/octet-stream";
            };
        }

        // 从文件名扩展名推断
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".ofd")) return HttpConstants.APPLICATION_OFD;
            if (lowerName.endsWith(".pdf")) return HttpConstants.APPLICATION_PDF;
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
            if (lowerName.endsWith(".png")) return "image/png";
            if (lowerName.endsWith(".tif") || lowerName.endsWith(".tiff")) return "image/tiff";
            if (lowerName.endsWith(".xml")) return "application/xml";
        }

        return "application/octet-stream";
    }

    /**
     * 判断是否为有效的图片类型
     */
    public static boolean isImageType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return false;
        }
        return switch (fileType.toUpperCase()) {
            case "JPG", "JPEG", "PNG", "GIF", "BMP", "TIFF", "TIF" -> true;
            default -> false;
        };
    }

    /**
     * 判断是否为支持的文档类型
     */
    public static boolean isDocumentType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return false;
        }
        return switch (fileType.toUpperCase()) {
            case "PDF", "OFD", "XML", "DOC", "DOCX", "XLS", "XLSX" -> true;
            default -> false;
        };
    }
}
