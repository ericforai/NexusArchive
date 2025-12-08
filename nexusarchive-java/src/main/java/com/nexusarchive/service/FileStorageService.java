package com.nexusarchive.service;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文件存储服务
 * 处理文件的物理存储路径解析、读写操作
 */
public interface FileStorageService {

    /**
     * 解析文件的绝对路径
     * @param relativePath 相对路径
     * @return 绝对路径对象
     */
    Path resolvePath(String relativePath);

    /**
     * 保存文件
     * @param inputStream 输入流
     * @param relativePath 相对路径
     * @return 保存后的相对路径
     */
    String saveFile(InputStream inputStream, String relativePath);

    /**
     * 检查文件是否存在
     * @param relativePath 相对路径
     * @return true if exists
     */
    boolean exists(String relativePath);
}
