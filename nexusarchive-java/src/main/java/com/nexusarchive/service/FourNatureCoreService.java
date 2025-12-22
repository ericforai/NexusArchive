// Input: Java 标准库、本地模块
// Output: FourNatureCoreService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import java.io.InputStream;
import com.nexusarchive.dto.sip.report.CheckItem;

/**
 * 四性检测核心原子服务
 * 提供针对单个文件的基础检测能力，供上层业务服务调用
 * 
 * @author Agent B
 */
public interface FourNatureCoreService {

    /**
     * 单文件真实性检测
     * @param inputStream 文件内容流
     * @param fileName 文件名
     * @param expectedHash 期望哈希
     * @param hashAlgo 哈希算法
     * @param fileType 文件类型 (PDF, OFD 等)
     * @return 检测项报告
     */
    CheckItem checkSingleFileAuthenticity(InputStream inputStream, String fileName, String expectedHash, String hashAlgo, String fileType);

    /**
     * 单文件可用性检测
     * @param inputStream 文件内容流
     * @param fileName 文件名
     * @param declaredType 声明的文件类型
     * @return 检测项报告
     */
    CheckItem checkSingleFileUsability(InputStream inputStream, String fileName, String declaredType);

    /**
     * 单文件安全性检测
     * @param inputStream 文件内容流
     * @param fileName 文件名
     * @return 检测项报告
     */
    CheckItem checkSingleFileSafety(InputStream inputStream, String fileName);
}
