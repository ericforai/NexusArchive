package com.nexusarchive.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类 (RS256 非对称加密版本)
 *
 * 功能:
 * - 生成JWT Token
 * - 验证JWT Token
 * - 解析JWT Token
 * - 刷新JWT Token
 */
@Component
public class JwtUtil {

    @Value("${jwt.public-key-location}")
    private String publicKeyLocation;

    @Value("${jwt.private-key-location}")
    private String privateKeyLocation;

    @Value("${jwt.expiration}")
    private Long expiration;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    private final ResourceLoader resourceLoader;

    public JwtUtil(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 初始化时加载公钥和私钥
     * 生产环境强烈建议从KMS或其他安全凭证服务加载，避免文件系统读取
     */
    @PostConstruct
    public void initKeys() {
        try {
            this.publicKey = loadPublicKey(publicKeyLocation);
            this.privateKey = loadPrivateKey(privateKeyLocation);
        } catch (Exception e) {
            // 在生产环境，如果密钥加载失败，应阻止应用启动
            throw new IllegalStateException("Failed to load JWT public/private keys. Application cannot start securely.", e);
        }
    }

    private PublicKey loadPublicKey(String location) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream is = resource.getInputStream()) {
            String key = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }
    }

    private PrivateKey loadPrivateKey(String location) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream is = resource.getInputStream()) {
            String key = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "") // Handle PKCS#1 vs PKCS#8
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes); // Expect PKCS#8 format
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        }
    }

    /**
     * 生成Token
     */
    public String generateToken(String username, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username); // username 仍在 claims 中，但不再用于 validateToken 校验
        return createToken(claims, userId); // subject 改为 userId (不可变), 降低耦合
    }

    /**
     * 创建Token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject) // 使用 userId 作为 subject
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(privateKey) // JJWT 0.12+ 自动识别算法
                .compact();
    }

    /**
     * 从Token中提取用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    /**
     * 从Token中提取用户ID
     */
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject); // subject 现在是 userId
    }

    /**
     * 从Token中提取过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 提取Claims
     */
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 提取所有Claims (包含验签和过期时间校验)
     * @throws SignatureException 如果签名无效
     * @throws ExpiredJwtException 如果token过期
     * @throws MalformedJwtException 如果token格式不正确
     * @throws IllegalArgumentException 其他非法参数
     */
    public Claims extractAllClaims(String token) throws JwtException, IllegalArgumentException {
        return Jwts.parser()
                .verifyWith(publicKey) // 使用公钥验签
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证Token (仅检查签名和过期时间)
     * @throws SignatureException 如果签名无效
     * @throws ExpiredJwtException 如果token过期
     * @throws MalformedJwtException 如果token格式不正确
     * @throws IllegalArgumentException 其他非法参数
     * @return true 如果token有效 (签名正确且未过期)
     */
    public Boolean validateToken(String token) throws JwtException, IllegalArgumentException {
        extractAllClaims(token); // 尝试提取 claims，如果签名无效或过期会抛出异常
        return true; // 如果没有抛出异常，则认为有效
    }
}