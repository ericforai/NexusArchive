// Input: Spring Web、Lombok、LoginAttemptService、ArchiveService
// Output: DebugController 类 (REST Endpoints)
// Pos: 调试接口层 (仅限内部/开发使用)
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final LoginAttemptService loginAttemptService;

    @PostMapping("/unlock/{username}")
    public String unlockUser(@PathVariable String username) {
        loginAttemptService.recordSuccess(username);
        return "User " + username + " unlocked (login attempts cleared).";
    }

}
