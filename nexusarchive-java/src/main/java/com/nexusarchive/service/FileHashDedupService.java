// Input: FileHashDedupScope Entity
// Output: FileHashDedupService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.request.FileHashDedupScopeRequest;

/**
 * 文件哈希去重范围服务
 * 
 * 功能：
 * 1. 配置去重范围（同全宗/授权范围/全局）
 * 2. 检查文件哈希是否在允许的去重范围内
 * 3. 防止跨全宗数据关联泄露
 * 
 * PRD 来源: Section 7.3 - 文件存储与防病毒
 */
public interface FileHashDedupService {
    
    /**
     * 配置去重范围
     * 
     * @param request 去重范围请求
     * @return 配置ID
     */
    String configureDedupScope(FileHashDedupScopeRequest request);
    
    /**
     * 检查文件哈希是否允许去重
     * 
     * @param fondsNo 全宗号
     * @param fileHash 文件哈希
     * @param targetFondsNo 目标全宗号（用于跨全宗场景）
     * @return 是否允许去重
     */
    boolean isDedupAllowed(String fondsNo, String fileHash, String targetFondsNo);
    
    /**
     * 获取全宗的去重范围配置
     * 
     * @param fondsNo 全宗号
     * @return 去重范围类型
     */
    String getDedupScopeType(String fondsNo);
}

