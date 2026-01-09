package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 临时数据库修复控制器
 */
@RestController
@RequestMapping("/db-fix")
@RequiredArgsConstructor
public class DatabaseFixController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/fix-borrowing-updated-at")
    public Result<String> fixBorrowingUpdatedAt(@RequestBody Map<String, String> payload) {
        String sql = "ALTER TABLE biz_borrowing ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP";
        try {
            jdbcTemplate.execute(sql);
            return Result.success("修复成功：biz_borrowing.updated_at 列已添加");
        } catch (Exception e) {
            return Result.error("修复失败: " + e.getMessage());
        }
    }
}
