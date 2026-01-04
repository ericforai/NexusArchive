// Input: EmployeeLifecycleEvent, UserService
// Output: UserLifecycleService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.request.OnboardEmployeeRequest;
import com.nexusarchive.dto.request.OffboardEmployeeRequest;
import com.nexusarchive.dto.request.TransferEmployeeRequest;

/**
 * 用户生命周期服务
 * 
 * 功能：
 * 1. 入职自动创建账号
 * 2. 离职自动停用账号并回收权限
 * 3. 调岗自动调整权限
 * 4. 处理待处理的生命周期事件
 * 
 * PRD 来源: Section 7.1 - 身份与账号生命周期
 */
public interface UserLifecycleService {
    
    /**
     * 入职处理
     * 
     * @param request 入职请求
     * @return 创建的用户ID
     */
    String onboardEmployee(OnboardEmployeeRequest request);
    
    /**
     * 离职处理
     * 
     * @param request 离职请求
     */
    void offboardEmployee(OffboardEmployeeRequest request);
    
    /**
     * 调岗处理
     * 
     * @param request 调岗请求
     */
    void transferEmployee(TransferEmployeeRequest request);
    
    /**
     * 处理待处理的生命周期事件
     * 定时任务调用
     */
    void processPendingEvents();
}



