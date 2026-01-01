// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: VolumeUtils 类
// Pos: 案卷服务 - 工具方法层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.volume;

import com.nexusarchive.entity.Archive;
import lombok.experimental.UtilityClass;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 案卷工具类
 * <p>
 * 提供案卷相关的通用工具方法
 * </p>
 */
@UtilityClass
public class VolumeUtils {

    /**
     * 生成案卷号
     * 格式: 全宗号-分类号-期间序号 (如 BR01-AC01-202508)
     */
    public String generateVolumeCode(String fondsNo, String categoryCode, String fiscalPeriod) {
        String periodCode = fiscalPeriod.replace("-", "");
        return String.format("%s-%s-%s", fondsNo, categoryCode, periodCode);
    }

    /**
     * 计算最长保管期限
     * 规范: "保管期限按卷内文件的最长保管期限填写"
     */
    public String calculateMaxRetention(List<Archive> archives) {
        Map<String, Integer> priorityMap = Map.of(
                "PERMANENT", 3,
                "30Y", 2,
                "10Y", 1
        );

        return archives.stream()
                .map(Archive::getRetentionPeriod)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(r -> priorityMap.getOrDefault(r, 0)))
                .orElse("10Y");
    }

    /**
     * XML 特殊字符转义
     */
    public String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * 递归打包目录到 ZIP
     */
    public void zipDirectory(Path sourceDir, String basePath, ZipOutputStream zos) throws IOException {
        try (var stream = Files.walk(sourceDir)) {
            stream.filter(path -> !Files.isDirectory(path))
                  .forEach(path -> {
                      String zipEntryName = basePath.isEmpty()
                          ? sourceDir.relativize(path).toString()
                          : basePath + "/" + sourceDir.relativize(path).toString();
                      try {
                          zos.putNextEntry(new ZipEntry(zipEntryName));
                          Files.copy(path, zos);
                          zos.closeEntry();
                      } catch (IOException e) {
                          throw new RuntimeException("打包文件失败: " + path, e);
                      }
                  });
        }
    }

    /**
     * 递归删除临时目录
     */
    public void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder())
                      .forEach(p -> {
                          try {
                              Files.deleteIfExists(p);
                          } catch (IOException e) {
                              // 静默删除失败
                          }
                      });
            }
        }
    }

    /**
     * 截断文本到指定长度
     */
    public String truncateText(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    /**
     * 安全文本处理
     * 如果支持中文字体，返回原文本
     * 如果不支持，替换非ASCII字符为?
     */
    public String safeText(String text, boolean supportChinese) {
        if (text == null) return "";
        if (supportChinese) return text;

        // 回退到 ASCII 模式
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c > 127) {
                sb.append("?");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
