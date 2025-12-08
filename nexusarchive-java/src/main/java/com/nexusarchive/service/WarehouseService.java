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
