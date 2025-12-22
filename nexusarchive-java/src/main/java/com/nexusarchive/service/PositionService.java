// Input: MyBatis-Plus、Java 标准库、本地模块
// Output: PositionService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.dto.request.CreatePositionRequest;
import com.nexusarchive.dto.request.UpdatePositionRequest;
import com.nexusarchive.entity.Position;

public interface PositionService {
    Position create(CreatePositionRequest request);
    Position update(UpdatePositionRequest request);
    void delete(String id);
    Page<Position> list(int page, int limit, String search, String status);
}
