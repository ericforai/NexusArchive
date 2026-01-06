// Input: FileStoragePolicy Entity
// Output: FileStoragePolicyService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.request.FileStoragePolicyRequest;
import com.nexusarchive.entity.FileStoragePolicy;

import java.util.List;

/**
 * 文件存储策略服务
 * 
 * 功能：
 * 1. 配置不可变策略
 * 2. 配置保留策略
 * 3. 检查文件是否受策略保护
 * 
 * PRD 来源: Section 7.3 - 文件存储与防病毒
 */
public interface FileStoragePolicyService {
    
    /**
     * 创建或更新存储策略
     * 
     * @param request 策略请求
     * @return 策略ID
     */
    String createOrUpdatePolicy(FileStoragePolicyRequest request);
    
    /**
     * 查询全宗的存储策略
     * 
     * @param fondsNo 全宗号
     * @return 策略列表
     */
    List<FileStoragePolicy> getPoliciesByFonds(String fondsNo);
    
    /**
     * 检查文件是否受不可变策略保护
     * 
     * @param fondsNo 全宗号
     * @param filePath 文件路径
     * @return 是否受保护
     */
    boolean isFileImmutable(String fondsNo, String filePath);
    
    /**
     * 检查文件是否在保留期内
     * 
     * @param fondsNo 全宗号
     * @param filePath 文件路径
     * @return 是否在保留期内
     */
    boolean isFileInRetention(String fondsNo, String filePath);
}





