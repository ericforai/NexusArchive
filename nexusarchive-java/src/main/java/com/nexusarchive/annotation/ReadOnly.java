// Input: Java 标准库
// Output: ReadOnly 注解 - 标记只读方法
// Pos: 基础设施层 - 数据源路由
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.annotation;

import java.lang.annotation.*;

/**
 * 只读操作注解
 * <p>标记在方法或类上，表示该方法/类中的所有方法都是只读操作，
 * 应该路由到从库执行</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @ReadOnly
 * public List<Archive> findAllArchives() {
 *     // 此查询会路由到从库
 *     return archiveMapper.selectList(null);
 * }
 * }</pre>
 *
 * <p>⚠️ 重要限制和注意事项：</p>
 * <ul>
 *   <li><b>功能开关</b>: 此注解仅在 {@code rw-split.enabled=true} 时生效</li>
 *
 *   <li><b>Read-Your-Writes 一致性问题</b>:
 *   <br>⚠️ 如果在写操作后立即调用此注解标记的方法，可能会读到旧数据（复制延迟）。
 *   <br>示例：
 *   <pre>{@code
 *   archiveService.createArchive(archive);  // 写入主库
 *   // 主从复制延迟期间，下面可能读到旧数据或空数据
 *   int count = statsService.getTotalArchives();  // 从从库读取
 *   }</pre>
 *   <br>如需强一致性读，请使用 {@code @Transactional(readOnly=true)}</li>
 *
 *   <li><b>与 @Async 不兼容</b>:
 *   <br>⚠️ 此注解在 {@code @Async} 异步方法中无效。
 *   <br>异步方法在独立线程中执行，ThreadLocal 上下文不会传递。
 *   <br>对于异步只读操作，请显式使用 {@code @Transactional(readOnly=true)}</li>
 *
 *   <li><b>与 @Transactional 的交互</b>:
 *   <br>同时使用时，{@code @Transactional} 的语义优先：
 *   <ul>
 *     <li>{@code @Transactional(readOnly=true)} + {@code @ReadOnly} → 从库（SLAVE）</li>
 *     <li>{@code @Transactional(readOnly=false)} + {@code @ReadOnly} → 主库（MASTER，写事务优先）</li>
 *   </ul></li>
 *
 *   <li><b>最佳实践</b>:
 *   <ul>
 *     <li>对统计数据、列表查询等容忍旧数据的场景使用此注解</li>
 *     <li>对写后立即读的场景使用 {@code @Transactional(readOnly=true)}</li>
 *     <li>避免在需要精确计数的业务逻辑中使用此注解</li>
 *   </ul></li>
 * </ul>
 *
 * @see org.springframework.transaction.annotation.Transactional
 * @since 2.1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
}
