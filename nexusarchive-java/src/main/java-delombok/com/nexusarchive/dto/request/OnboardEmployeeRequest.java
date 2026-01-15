// Input: Java 标准库
// Output: OnboardEmployeeRequest DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 入职请求 DTO
 */
@Data
public class OnboardEmployeeRequest {
    
    /**
     * 员工ID（外部系统）
     */
    private String employeeId;
    
    /**
     * 员工姓名
     */
    private String employeeName;
    
    /**
     * 入职日期
     */
    private LocalDate onboardDate;
    
    /**
     * 组织ID（集团型架构必需）
     */
    private String organizationId;
    
    /**
     * 角色ID列表
     */
    private List<String> roleIds;
    
    /**
     * 用户名（可选，不提供则自动生成）
     */
    private String username;
    
    /**
     * 初始密码（可选，不提供则生成临时密码）
     */
    private String initialPassword;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
}

