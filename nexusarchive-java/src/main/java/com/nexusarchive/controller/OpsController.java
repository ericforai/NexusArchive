// Input: Spring Framework、Java 标准库
// Output: OpsController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops")
public class OpsController {
    @GetMapping("/self-check")
    public ResponseEntity<String> selfCheck() {
        return ResponseEntity.ok("OK");
    }
}
