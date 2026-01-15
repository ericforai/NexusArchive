// Input: JJWT、Jakarta EE、Lombok、Spring Security、等
// Output: JwtAuthenticationFilter 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.nexusarchive.service.CustomUserDetailsService;
import com.nexusarchive.service.TokenBlacklistService;
import com.nexusarchive.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器 (安全重构版本)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else {
            // [ADDED] Support token in query parameter for iframe/browser direct access
            String queryToken = request.getParameter("access_token");
            if (queryToken != null && !queryToken.isEmpty()) {
                jwt = queryToken;
            } else {
                filterChain.doFilter(request, response);
                return;
            }
        }

        try {
            // 1. 检查Token是否在黑名单中
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                // 如果在黑名单中，默默拒绝，不提供任何额外信息
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 验证Token签名和过期时间 (validateToken会抛出异常)
            jwtUtil.validateToken(jwt);
            
            // 3. 提取用户ID，并确保当前没有已认证的用户
            userId = jwtUtil.extractUserId(jwt);
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 4. 从数据库加载用户信息
                UserDetails userDetails = userDetailsService.loadUserById(userId);

                // 5. 创建认证凭证并设置到SecurityContext中
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // 将用户ID放入Request属性中，方便后续Controller或Service层获取
                request.setAttribute("userId", userId);
            }

        } catch (SignatureException e) {
            // [FIXED P0-5] 完全隐藏 Token，仅记录哈希
            String tokenHash = calculateSHA256(jwt).substring(0, 16);
            logger.error("[SECURITY] Invalid JWT Signature - TokenHash: " + tokenHash + 
                ", IP: " + request.getRemoteAddr());
            
        } catch (ExpiredJwtException e) {
            logger.warn("Expired JWT Token - User: " + e.getClaims().getSubject());
            
        } catch (MalformedJwtException e) {
            // [FIXED P0-5] 使用哈希替代 Token
            String tokenHash = calculateSHA256(jwt).substring(0, 16);
            logger.warn("Malformed JWT Token - TokenHash: " + tokenHash + 
                ", IP: " + request.getRemoteAddr());
                
        } catch (JwtException e) {
            logger.error("JWT processing error: " + e.getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("An unexpected error occurred in JwtAuthenticationFilter", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * [ADDED P0-5] 计算 Token 的 SHA-256 哈希
     * 用于日志记录，防止 Token 泄露
     */
    private String calculateSHA256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            return "HASH_ERROR";
        }
    }

    /**
     * [ADDED P0-5] 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
