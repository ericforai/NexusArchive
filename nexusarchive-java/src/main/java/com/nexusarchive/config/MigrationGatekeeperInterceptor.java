// Input: Jackson、Jakarta EE、Lombok、Spring Framework、等
// Output: MigrationGatekeeperInterceptor 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class MigrationGatekeeperInterceptor implements HandlerInterceptor {

    private final ResilientFlywayRunner flywayRunner;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (flywayRunner.isReady()) {
            return true;
        }

        // Allow health check always (Should be excluded by WebMvcConfig, but double check)
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/health")) {
            return true;
        }

        // Reject everything else
        response.setStatus(503);
        response.setContentType("application/json;charset=UTF-8");
        
        Result<Object> result = Result.error(503, "System Initializing (Database Migration in Progress)");
        response.getWriter().write(objectMapper.writeValueAsString(result));
        
        return false;
    }
}
