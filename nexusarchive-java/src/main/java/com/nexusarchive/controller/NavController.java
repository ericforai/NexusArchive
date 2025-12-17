package com.nexusarchive.controller;

import com.nexusarchive.mapper.ArchiveMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态导航菜单控制器
 * 负责提供基于数据的动态菜单结构
 */
@RestController
@RequestMapping("/nav")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "导航接口", description = "动态菜单与路由")
public class NavController {

    private final ArchiveMapper archiveMapper;

    @Operation(summary = "获取账簿类型列表", description = "根据全宗号获取存在的会计账簿类型")
    @GetMapping("/books")
    public List<String> getBookTypes(@RequestParam(defaultValue = "DEMO") String fondsNo) {
        log.info("Fetching dynamic book types for fonds: {}", fondsNo);
        List<String> types = archiveMapper.selectDistinctBookTypes(fondsNo);
        
        // 过滤空值
        if (types != null) {
            return types.stream()
                .filter(t -> t != null && !t.isEmpty())
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
