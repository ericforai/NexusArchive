package com.nexusarchive.config;

import com.nexusarchive.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT认证过滤器
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        final String userId;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.extractAllClaims(jwt);
            username = claims.getSubject();
            userId = claims.get("userId", String.class);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 在实际生产中，这里可能需要从数据库加载用户详情以获取最新的权限信息
                // 但为了性能，也可以直接从Token中获取基本信息，或者使用缓存
                // 这里我们构建一个简单的UserDetails对象
                UserDetails userDetails = User.builder()
                        .username(username)
                        .password("") // 密码不重要，因为已经通过JWT验证
                        .authorities(new ArrayList<>()) // TODO: 从Token或数据库加载权限
                        .build();

                if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // 将用户ID放入Request属性中，方便Controller获取
                    request.setAttribute("userId", userId);
                }
            }
        } catch (Exception e) {
            // Token验证失败，不设置Authentication，由Spring Security处理
            logger.error("JWT Authentication failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
