// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: WarehouseController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Location;
import com.nexusarchive.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/shelves")
    public Result<List<Location>> getShelves() {
        return Result.success(warehouseService.getShelves());
    }

    @PostMapping("/items")
    public Result<Void> addItemToShelf(@RequestBody Map<String, String> payload) {
        String shelfId = payload.get("shelfId");
        String archiveId = payload.get("archiveId");
        warehouseService.addItemToShelf(shelfId, archiveId);
        return Result.success();
    }

    @DeleteMapping("/items/{id}")
    public Result<Void> removeItemFromShelf(@PathVariable String id, @RequestParam String shelfId) {
        warehouseService.removeItemFromShelf(shelfId, id);
        return Result.success();
    }

    @GetMapping("/environment")
    public Result<Map<String, Object>> getEnvironmentData() {
        return Result.success(warehouseService.getEnvironmentData());
    }

    @PostMapping("/racks/{id}/command")
    public Result<Map<String, Object>> sendCommand(@PathVariable("id") String rackId,
                                                   @RequestBody Map<String, String> payload) {
        String action = payload.get("action");
        return Result.success(warehouseService.applyCommand(rackId, action));
    }
}
