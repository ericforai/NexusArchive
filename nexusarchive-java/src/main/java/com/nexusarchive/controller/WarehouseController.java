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
