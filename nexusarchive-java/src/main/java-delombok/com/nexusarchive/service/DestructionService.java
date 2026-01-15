// Input: MyBatis-Plus、Java 标准库、本地模块
// Output: DestructionService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Destruction;

import java.util.List;

public interface DestructionService {
    Destruction createDestruction(Destruction destruction);
    Page<Destruction> getDestructions(int page, int limit, String status);
    void approveDestruction(String id, String approverId, String comment);
    void executeDestruction(String id);
}
