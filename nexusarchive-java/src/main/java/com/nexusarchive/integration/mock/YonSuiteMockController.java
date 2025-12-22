// Input: Lombok、Spring Framework、Java 标准库
// Output: YonSuiteMockController 类
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/mock/yonsuite")
@Slf4j
public class YonSuiteMockController {

    @GetMapping("/voucher/{id}")
    public ResponseEntity<Map<String, Object>> getVoucher(@PathVariable String id) {
        log.info("Mock YonSuite: Query Voucher {}", id);
        
        Map<String, Object> voucher = new HashMap<>();
        voucher.put("id", id);
        voucher.put("code", "V-" + id);
        voucher.put("date", "2023-12-01");
        voucher.put("type", "记账凭证");
        voucher.put("amount", 1000.00);
        voucher.put("summary", "Mock Voucher Data");
        
        // Mock entries
        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("summary", "Expense");
        entry1.put("account_code", "6601");
        entry1.put("debit", 1000.00);
        entry1.put("credit", 0.00);
        
        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("summary", "Bank");
        entry2.put("account_code", "1002");
        entry2.put("debit", 0.00);
        entry2.put("credit", 1000.00);
        
        voucher.put("entries", java.util.List.of(entry1, entry2));
        
        return ResponseEntity.ok(Map.of("code", "200", "data", voucher, "message", "success"));
    }
}
