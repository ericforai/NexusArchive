// Input: Java 标准库
// Output: FileHashDedupScopeRequest DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

/**
 * 文件哈希去重范围请求 DTO
 */
@Data
public class FileHashDedupScopeRequest {
    
    /**
     * 全宗号
     */
    private String fondsNo;
    
    /**
     * 去重范围: SAME_FONDS, AUTHORIZED, GLOBAL
     */
    private String scopeType;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
}





