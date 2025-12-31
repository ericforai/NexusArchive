// Input: Spring MVC
// Output: 搜索 API
// Pos: NexusCore controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.core.domain.ArchiveObject;
import com.nexusarchive.core.search.ArchiveSearchRequest;
import com.nexusarchive.core.search.ArchiveSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/archives/search")
@RequiredArgsConstructor
public class ArchiveSearchController {

    private final ArchiveSearchService archiveSearchService;

    @PostMapping
    public IPage<ArchiveObject> search(@RequestBody ArchiveSearchRequest request,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return archiveSearchService.search(request, new Page<>(page, size));
    }
}
