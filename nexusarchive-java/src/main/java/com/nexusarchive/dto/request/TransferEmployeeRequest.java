// Input: Java 标准库
// Output: TransferEmployeeRequest DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 调岗请求 DTO
 */
@Data
public class TransferEmployeeRequest {
    
    /**
     * 员工ID（外部系统）或用户ID
     */
    private String employeeId;
    
    /**
     * 员工姓名
     */
    private String employeeName;
    
    /**
     * 调岗日期
     */
    private LocalDate transferDate;
    
    /**
     * 原部门ID
     */
    private String previousDeptId;
    
    /**
     * 新部门ID
     */
    private String newDeptId;
    
    /**
     * 原角色ID列表
     */
    private List<String> previousRoleIds;
    
    /**
     * 新角色ID列表
     */
    private List<String> newRoleIds;
    
    /**
     * 调岗原因
     */
    private String reason;
}

