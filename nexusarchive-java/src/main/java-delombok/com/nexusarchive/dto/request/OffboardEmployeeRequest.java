// Input: Java 标准库
// Output: OffboardEmployeeRequest DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 离职请求 DTO
 */
@Data
public class OffboardEmployeeRequest {
    
    /**
     * 员工ID（外部系统）或用户ID
     */
    private String employeeId;
    
    /**
     * 员工姓名
     */
    private String employeeName;
    
    /**
     * 离职日期
     */
    private LocalDate offboardDate;
    
    /**
     * 离职原因
     */
    private String reason;
}





