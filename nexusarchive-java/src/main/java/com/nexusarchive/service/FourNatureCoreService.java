package com.nexusarchive.service;

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
     * @param content 文件内容
     * @param fileName 文件名
     * @param expectedHash 期望哈希
     * @param hashAlgo 哈希算法
     * @param fileType 文件类型 (PDF, OFD 等)
     * @return 检测项报告
     */
    CheckItem checkSingleFileAuthenticity(byte[] content, String fileName, String expectedHash, String hashAlgo, String fileType);

    /**
     * 单文件可用性检测
     * @param content 文件内容
     * @param fileName 文件名
     * @param declaredType 声明的文件类型
     * @return 检测项报告
     */
    CheckItem checkSingleFileUsability(byte[] content, String fileName, String declaredType);

    /**
     * 单文件安全性检测
     * @param content 文件内容
     * @param fileName 文件名
     * @return 检测项报告
     */
    CheckItem checkSingleFileSafety(byte[] content, String fileName);
}
