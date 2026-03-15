// Input: JUnit 5、Spring Test、PathSecurityUtils
// Output: PathSecurityUtilsTest 类
// Pos: 测试模块

package com.nexusarchive.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PathSecurityUtils 单元测试
 *
 * 测试路径安全工具类对路径遍历攻击的防护能力
 */
@Tag("unit")
class PathSecurityUtilsTest {

    private PathSecurityUtils pathSecurityUtils;

    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        this.pathSecurityUtils = new PathSecurityUtils();
        
        // 使用反射设置配置
        ReflectionTestUtils.setField(pathSecurityUtils, "archiveRootPath", tempDir.resolve("archives").toString());
        ReflectionTestUtils.setField(pathSecurityUtils, "tempPath", tempDir.resolve("temp").toString());
    }

    @Test
    void shouldValidateNormalRelativePath() {
        // 正常相对路径应该通过验证
        Path result = pathSecurityUtils.validateAndNormalize("BR01/2025/001.pdf", tempDir.toString());
        assertTrue(result.toString().contains("BR01"));
        assertTrue(result.startsWith(tempDir.toAbsolutePath().normalize()));
    }

    @Test
    void shouldBlockPathTraversalWithDoubleDot() {
        // 路径遍历攻击: ../..
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateAndNormalize("../../../etc/passwd", tempDir.toString())
        );
    }

    @Test
    void shouldBlockUrlEncodedPathTraversal() {
        // URL 编码的路径遍历: %2e%2e
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateAndNormalize("%2e%2e/%2e%2e/etc/passwd", tempDir.toString())
        );
    }

    @Test
    void shouldBlockMixedEncodingPathTraversal() {
        // 混合编码: ..%2f
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateAndNormalize("..%2f..%2fetc/passwd", tempDir.toString())
        );
    }

    @Test
    void shouldBlockNullPath() {
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateAndNormalize(null, tempDir.toString())
        );
    }

    @Test
    void shouldBlockEmptyPath() {
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateAndNormalize("", tempDir.toString())
        );
    }

    @Test
    void shouldGetSafeFileName() {
        // 正常文件名应该通过
        String safeName = pathSecurityUtils.getSafeFileName("document.pdf");
        assertEquals("document.pdf", safeName);
    }

    @Test
    void shouldRemovePathFromFileName() {
        // 带路径的文件名应该只返回文件名
        String safeName = pathSecurityUtils.getSafeFileName("/path/to/document.pdf");
        assertEquals("document.pdf", safeName);
    }

    @Test
    void shouldBlockDangerousFileName() {
        // 路径遍历会被自动处理 - getSafeFileName 提取文件名部分
        // ../../../etc/passwd 的文件名部分是 "passwd"，这是安全的
        String safeName = pathSecurityUtils.getSafeFileName("../../../etc/passwd");
        assertEquals("passwd", safeName);

        // URL 编码的空字符注入会被 containsDangerousChars 检测到
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.getSafeFileName("file%00name.txt")
        );
    }

    @Test
    void shouldBlockDangerousFileExtension() {
        // 危险文件扩展名应该被阻止
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.getSafeFileName("malware.exe")
        );
    }

    @Test
    void shouldValidateScanPath_AbsolutePathOutsideTemp() throws IOException {
        // 创建临时目录结构
        Path scanDir = tempDir.resolve("temp").resolve("scan");
        Files.createDirectories(scanDir);

        // 绝对路径在临时目录内应该通过
        Path fileInTemp = scanDir.resolve("test.pdf");
        Files.createFile(fileInTemp);

        Path result = pathSecurityUtils.validateScanPath(fileInTemp.toString());
        assertTrue(result.startsWith(tempDir.toAbsolutePath().normalize()));
    }

    @Test
    void shouldBlockScanPath_AbsolutePathOutsideTempDirectory() {
        // 绝对路径在临时目录外应该被阻止
        Path outsidePath = Paths.get("/etc/passwd");
        
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateScanPath(outsidePath.toString())
        );
    }

    @Test
    void shouldBlockScanPath_RelativePathTraversal() {
        // 相对路径遍历攻击应该被阻止
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateScanPath("../../../etc/passwd")
        );
    }

    @Test
    void shouldValidateScanPath_SafeRelativePath() throws IOException {
        // 创建扫描目录
        Path scanDir = tempDir.resolve("temp").resolve("scan");
        Files.createDirectories(scanDir);

        // 相对路径应该被验证为临时目录下的路径
        Path result = pathSecurityUtils.validateScanPath("scan/test.pdf");
        assertTrue(result.toString().contains("temp"));
    }

    @Test
    void shouldBlockScanPath_NullPath() {
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateScanPath(null)
        );
    }

    @Test
    void shouldBlockScanPath_EmptyPath() {
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateScanPath("")
        );
    }

    @Test
    void shouldBlockScanPath_UrlEncodedTraversal() {
        // URL 编码的路径遍历攻击应该被阻止
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateScanPath("..%2f..%2fetc/passwd")
        );
    }

    @Test
    void shouldValidateArchivePath() throws IOException {
        // 创建档案目录
        Path archiveDir = tempDir.resolve("archives").resolve("BR01");
        Files.createDirectories(archiveDir);

        Path result = pathSecurityUtils.validateArchivePath("BR01/2025/001.pdf");
        assertTrue(result.toString().contains("BR01"));
        assertTrue(result.startsWith(tempDir.toAbsolutePath().normalize()));
    }

    @Test
    void shouldBlockArchivePath_Traversal() {
        assertThrows(SecurityException.class, () ->
            pathSecurityUtils.validateArchivePath("../../../etc/passwd")
        );
    }
}
