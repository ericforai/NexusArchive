// Input: Lombok、Spring Security、Spring Framework、Java 标准库、等
// Output: GlobalSearchController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.dto.GlobalSearchDTO;
import com.nexusarchive.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public List<GlobalSearchDTO> search(@RequestParam("q") String query) {
        return globalSearchService.search(query);
    }
}
