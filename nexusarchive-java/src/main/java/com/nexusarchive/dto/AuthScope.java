// Input: Java 标准库、Jackson
// Output: AuthScope DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 授权票据访问范围
 * 
 * 用于定义跨全宗访问的权限范围
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthScope {
    
    /**
     * 归档年度列表（可选）
     */
    private List<Integer> archiveYears;
    
    /**
     * 档案类型列表（可选）
     */
    private List<String> docTypes;
    
    /**
     * 关键词列表（可选）
     */
    private List<String> keywords;
    
    /**
     * 访问类型: READ_ONLY(只读), READ_WRITE(读写)
     * 默认: READ_ONLY
     */
    private String accessType = "READ_ONLY";
}


