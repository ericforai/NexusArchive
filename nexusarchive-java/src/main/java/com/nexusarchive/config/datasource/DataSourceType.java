// Input: Java 标准库
// Output: DataSourceType 枚举 - 数据源类型
// Pos: 基础设施层 - 数据源路由
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config.datasource;

/**
 * 数据源类型枚举
 * <p>用于读写分离场景的路由标识</p>
 *
 * @since 2.1.0
 */
public enum DataSourceType {

    /**
     * 主库 - 用于写操作和强一致性读操作
     */
    MASTER,

    /**
     * 从库 - 用于普通读操作
     */
    SLAVE
}
