// Input: Java 标准库
// Output: VirusScanAdapter 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.adapter;

import java.io.InputStream;

/**
 * 病毒扫描适配器接口
 * Reference: DA/T 92-2022 Clause 6.4 Safety Check
 */
public interface VirusScanAdapter {
    
    /**
     * 扫描文件内容
     * @param inputStream 文件内容流
     * @param fileName 文件名
     * @return true if safe, false if infected
     */
    boolean scan(InputStream inputStream, String fileName);

    /**
     * 扫描文件内容 (兼容旧接口)
     */
    default boolean scan(byte[] fileContent, String fileName) {
        return scan(new java.io.ByteArrayInputStream(fileContent), fileName);
    }
}
