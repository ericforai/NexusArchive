// Input: MyBatis-Plus、Java 标准库、本地模块
// Output: WarehouseService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Location;

import java.util.List;
import java.util.Map;

public interface WarehouseService {
    Location createLocation(Location location);
    List<Location> getShelves();
    void addItemToShelf(String shelfId, String archiveId);
    void removeItemFromShelf(String shelfId, String archiveId);
    Map<String, Object> getEnvironmentData();
    Map<String, Object> applyCommand(String shelfId, String action);
}
