// Input: Java 标准库
// Output: FileStorageService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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

    /**
     * 获取文件对象
     * @param relativePath 相对路径
     * @return File对象，不存在时返回null
     */
    java.io.File getFile(String relativePath);

    /**
     * 获取文件信息
     * @param relativePath 相对路径
     * @return 文件信息，不存在时返回null
     */
    default FileInfo getFileInfo(String relativePath) {
        java.io.File file = getFile(relativePath);
        if (file == null || !file.exists()) {
            return null;
        }
        return new FileInfo(file.getName(), file.length(), file.lastModified());
    }

    /**
     * 软删除文件（移动到回收站或标记删除）
     * @param relativePath 相对路径
     * @return true if success
     */
    default boolean softDelete(String relativePath) {
        // 默认实现：移动到 .trash 目录
        java.io.File file = getFile(relativePath);
        if (file == null || !file.exists()) {
            return false;
        }
        java.io.File trashDir = new java.io.File(file.getParent(), ".trash");
        if (!trashDir.exists()) {
            trashDir.mkdirs();
        }
        return file.renameTo(new java.io.File(trashDir, file.getName() + "." + System.currentTimeMillis()));
    }

    /**
     * 硬删除文件（物理删除）
     * @param relativePath 相对路径
     * @return true if success
     */
    default boolean hardDelete(String relativePath) {
        java.io.File file = getFile(relativePath);
        if (file == null || !file.exists()) {
            return false;
        }
        return file.delete();
    }

    /**
     * 文件信息
     */
    record FileInfo(String name, long size, long lastModified) {
        public long getSize() { return size; }
    }
}
