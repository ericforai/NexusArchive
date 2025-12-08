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
