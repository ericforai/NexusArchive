// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: WarehouseServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Location;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.LocationMapper;
import com.nexusarchive.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final LocationMapper locationMapper;
    private final ArchiveMapper archiveMapper;

    @Override
    @Transactional
    public Location createLocation(Location location) {
        location.setStatus("NORMAL");
        location.setUsedCount(0);
        locationMapper.insert(location);
        return location;
    }

    @Override
    public List<Location> getShelves() {
        // Return all locations of type SHELF
        return locationMapper.selectList(new QueryWrapper<Location>()
                .eq("type", "SHELF"));
    }

    @Override
    @Transactional
    public void addItemToShelf(String shelfId, String archiveId) {
        Location shelf = locationMapper.selectById(shelfId);
        if (shelf == null) {
            throw new RuntimeException("Shelf not found");
        }
        
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new RuntimeException("Archive not found");
        }

        // Update archive location
        archive.setLocation(shelf.getPath() + "/" + shelf.getName());
        archiveMapper.updateById(archive);

        // Update shelf usage
        shelf.setUsedCount(shelf.getUsedCount() + 1);
        if (shelf.getUsedCount() >= shelf.getCapacity()) {
            shelf.setStatus("FULL");
        }
        locationMapper.updateById(shelf);
    }

    @Override
    @Transactional
    public void removeItemFromShelf(String shelfId, String archiveId) {
        Location shelf = locationMapper.selectById(shelfId);
        if (shelf == null) {
            throw new RuntimeException("Shelf not found");
        }

        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new RuntimeException("Archive not found");
        }

        // Clear archive location
        archive.setLocation(null);
        archiveMapper.updateById(archive);

        // Update shelf usage
        if (shelf.getUsedCount() > 0) {
            shelf.setUsedCount(shelf.getUsedCount() - 1);
            shelf.setStatus("NORMAL");
        }
        locationMapper.updateById(shelf);
    }

    @Override
    public Map<String, Object> getEnvironmentData() {
        // Mock environment data
        Map<String, Object> data = new HashMap<>();
        data.put("temperature", 22.5);
        data.put("humidity", 45.0);
        data.put("status", "NORMAL");
        data.put("lastUpdated", java.time.LocalDateTime.now().toString());
        return data;
    }

    @Override
    public Map<String, Object> applyCommand(String shelfId, String action) {
        Location shelf = locationMapper.selectById(shelfId);
        if (shelf == null) {
            throw new RuntimeException("Shelf not found");
        }

        String normalized = action == null ? "" : action.trim().toUpperCase();
        switch (normalized) {
            case "OPEN" -> shelf.setStatus("OPEN");
            case "CLOSE" -> shelf.setStatus("CLOSED");
            case "VENT" -> shelf.setStatus("VENTILATING");
            case "LOCK" -> shelf.setStatus("LOCKED");
            case "UNLOCK" -> shelf.setStatus("NORMAL");
            default -> throw new IllegalArgumentException("Unsupported action: " + action);
        }
        locationMapper.updateById(shelf);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", shelf.getStatus());
        resp.put("shelfId", shelfId);
        return resp;
    }
}
