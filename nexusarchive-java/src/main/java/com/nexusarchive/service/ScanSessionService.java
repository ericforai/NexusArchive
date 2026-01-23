// Input: Spring Framework, Service Interface Pattern
// Output: ScanSessionService 接口 - 扫描会话管理
// Pos: Service Interface Layer

package com.nexusarchive.service;

/**
 * 扫描会话服务接口
 *
 * <p>管理移动端扫码上传的会话生命周期：</p>
 * <ol>
 *   <li>创建会话 - 生成唯一 sessionId 并持久化到 Redis</li>
 *   <li>验证会话 - 检查会话是否有效（未过期）</li>
 *   <li>获取用户 - 根据 sessionId 获取关联的用户 ID</li>
 *   <li>删除会话 - 手动终止会话</li>
 * </ol>
 *
 * <p>会话特性：</p>
 * <ul>
 *   <li>有效期：30 分钟（Redis TTL 自动过期）</li>
 *   <li>存储：Redis（支持分布式部署）</li>
 *   <li>用途：移动端扫码上传文件时的临时认证</li>
 * </ul>
 */
public interface ScanSessionService {

    /**
     * 创建扫描会话
     *
     * @param userId 用户ID
     * @return 会话ID（UUID 格式）
     */
    String createSession(String userId);

    /**
     * 验证会话是否有效
     *
     * @param sessionId 会话ID
     * @return 是否有效
     */
    boolean validateSession(String sessionId);

    /**
     * 获取会话关联的用户ID
     *
     * @param sessionId 会话ID
     * @return 用户ID，如果会话不存在或已过期返回 null
     */
    String getSessionUserId(String sessionId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId);
}
