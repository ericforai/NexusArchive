// Input: Java 标准库
// Output: BorrowExpirationService 接口
// Pos: 服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

/**
 * 借阅到期管理服务
 *
 * 功能：
 * 1. 定时检查并标记过期的借阅记录
 * 2. 发送到期前提醒通知
 *
 * PRD 来源: 2026-01-10-query-user-borrow-design.md 第7.3节
 */
public interface BorrowExpirationService {

    /**
     * 扫描并标记过期的借阅记录
     * 将 expected_return_date < 今日的 APPROVED/BORROWED 状态记录标记为 EXPIRED
     *
     * @return 过期记录数量
     */
    int scanAndMarkExpired();

    /**
     * 手动触发扫描（用于管理操作）
     *
     * @return 过期记录数量
     */
    int manualScan();

    /**
     * 获取即将到期的借阅记录（用于提醒）
     *
     * @param days 提前天数
     * @return 即将到期的借阅记录数量
     */
    long getUpcomingExpirations(int days);
}
