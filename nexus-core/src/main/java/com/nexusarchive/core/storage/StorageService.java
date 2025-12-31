// Input: 存储路径 (AccessPath)
// Output: 文件流/元数据
// Pos: NexusCore storage abstraction
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 存储服务抽象接口
 * 支持本地磁盘、NAS、对象存储 (OSS/S3)
 */
public interface StorageService {

    /**
     * 判断文件是否存在
     * @param storagePath 存储路径 (相对或绝对，取决于实现)
     * @return true if exists
     */
    boolean exists(String storagePath);

    /**
     * 获取文件大小 (字节)
     * @param storagePath 存储路径
     * @return file size in bytes
     * @throws IOException if error occurs
     */
    long getLength(String storagePath) throws IOException;

    /**
     * 获取文件输入流
     * @param storagePath 存储路径
     * @return InputStream (caller must close)
     * @throws IOException if error occurs
     */
    InputStream getInputStream(String storagePath) throws IOException;
    
    /**
     * 获取文件输入流 (支持 Range)
     * @param storagePath 存储路径
     * @param offset 起始偏移量
     * @param length 读取长度 (若 < 0 则读到末尾)
     * @return InputStream
     * @throws IOException if error occurs
     */
    InputStream getInputStream(String storagePath, long offset, long length) throws IOException;
}
