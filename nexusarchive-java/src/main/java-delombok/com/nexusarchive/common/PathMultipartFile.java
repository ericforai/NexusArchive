// Input: Spring MultipartFile, Java NIO Path
// Output: PathMultipartFile 适配器类
// Pos: 基础设施层

package com.nexusarchive.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Path 到 MultipartFile 的适配器
 *
 * 用于将文件系统路径转换为 MultipartFile 接口，
 * 使文件夹监控可以直接调用现有的文件上传逻辑。
 */
@Slf4j
public class PathMultipartFile implements MultipartFile {

    private final Path path;
    private final String filename;
    private final String contentType;

    public PathMultipartFile(Path path, String contentType) {
        this.path = path;
        this.filename = path.getFileName().toString();
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        try {
            return Files.size(path) == 0;
        } catch (IOException e) {
            log.error("无法读取文件大小: {}", path, e);
            return true;  // 出错时视为空文件
        }
    }

    @Override
    public long getSize() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            log.error("无法读取文件大小: {}", path, e);
            return 0;
        }
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        Files.copy(path, dest.toPath());
    }
}
