// Input: Java 标准库
// Output: DataSourceContextHolder 类 - 数据源路由上下文
// Pos: 基础设施层 - 数据源路由
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config.datasource;

/**
 * 数据源路由上下文持有者
 * <p>使用 ThreadLocal 存储当前线程的数据源类型，实现方法级别的读写分离</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 设置为主库
 * DataSourceContextHolder.setDataSourceType(DataSourceType.MASTER);
 *
 * // 获取当前数据源类型
 * DataSourceType type = DataSourceContextHolder.getDataSourceType();
 *
 * // 清理上下文（通常在方法执行后）
 * DataSourceContextHolder.clearDataSourceType();
 * }</pre>
 *
 * @since 2.1.0
 */
public final class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    private DataSourceContextHolder() {
        // 工具类，禁止实例化
    }

    /**
     * 设置当前线程的数据源类型
     *
     * @param dataSourceType 数据源类型
     */
    public static void setDataSourceType(DataSourceType dataSourceType) {
        if (dataSourceType == null) {
            clearDataSourceType();
            return;
        }
        CONTEXT.set(dataSourceType);
    }

    /**
     * 获取当前线程的数据源类型
     * <p>如果未设置，默认返回 {@link DataSourceType#SLAVE}</p>
     *
     * @return 数据源类型
     */
    public static DataSourceType getDataSourceType() {
        DataSourceType type = CONTEXT.get();
        return type == null ? DataSourceType.SLAVE : type;
    }

    /**
     * 清理当前线程的数据源类型
     * <p>应在方法执行完成后调用，避免 ThreadLocal 内存泄漏</p>
     */
    public static void clearDataSourceType() {
        CONTEXT.remove();
    }
}
