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
