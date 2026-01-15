// Input: Spring Framework、JDBC、Lombok、SLF4J
// Output: DataConsistencyValidator 类
// Pos: 配置层 - 启动时数据一致性检查
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据一致性验证器
 * <p>
 * 在应用启动时检查外键引用完整性，防止"孤儿数据"导致查询不到结果
 * </p>
 * <p>
 * 检查项：
 * <ul>
 *   <li>原始凭证的 fonds_code 是否存在于 sys_entity 表</li>
 *   <li>原始凭证的 voucher_type 是否存在于 sys_original_voucher_type 表</li>
 * </ul>
 * </p>
 *
 * @see <a href="docs/plans/2026-01-15-doc-pool-empty-bug-postmortem.md">问题分析文档</a>
 */
@Component
@Order(100)  // 在 Flyway 迁移之后执行
@Slf4j
@RequiredArgsConstructor
public class DataConsistencyValidator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${data.consistency.check.enabled:true}")
    private boolean checkEnabled;

    @Value("${data.consistency.check.fail-on-error:false}")
    private boolean failOnError;

    @Override
    public void run(ApplicationArguments args) {
        if (!checkEnabled) {
            log.info("[DataConsistency] 数据一致性检查已禁用 (data.consistency.check.enabled=false)");
            return;
        }

        log.info("[DataConsistency] 开始数据一致性检查...");

        boolean hasErrors = false;

        // 检查1：原始凭证的全宗代码引用完整性
        hasErrors |= checkVoucherFondsReference();

        // 检查2：原始凭证的类型代码引用完整性
        hasErrors |= checkVoucherTypeReference();

        if (hasErrors && failOnError) {
            throw new IllegalStateException(
                "数据一致性检查失败，应用无法启动。请修复上述问题或设置 data.consistency.check.fail-on-error=false"
            );
        }

        log.info("[DataConsistency] 数据一致性检查完成");
    }

    /**
     * 检查：arc_original_voucher.fonds_code 必须存在于 sys_entity.id
     */
    private boolean checkVoucherFondsReference() {
        String sql = """
            SELECT DISTINCT v.fonds_code, COUNT(*) as voucher_count
            FROM arc_original_voucher v
            WHERE v.deleted = 0
            AND NOT EXISTS (
                SELECT 1 FROM sys_entity e 
                WHERE e.id = v.fonds_code 
                AND e.deleted = 0
            )
            GROUP BY v.fonds_code
            """;

        List<FondsIssue> orphanFonds = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new FondsIssue(
                rs.getString("fonds_code"),
                rs.getInt("voucher_count")
            )
        );

        if (!orphanFonds.isEmpty()) {
            log.error("[DataConsistency] 发现 {} 个孤儿全宗代码，这些单据将无法被查询到：",
                orphanFonds.size());
            for (FondsIssue issue : orphanFonds) {
                log.error("[DataConsistency]   - 全宗代码: {}, 影响单据数: {} 条。解决方法：在 sys_entity 表中创建该法人实体，或更新单据的 fonds_code。",
                    issue.fondsCode(), issue.voucherCount());
            }
            log.error("[DataConsistency] 修复 SQL 示例：");
            log.error("[DataConsistency]   INSERT INTO sys_entity (id, name, status) VALUES ('DEMO', '演示全宗', 'ACTIVE');");
            log.error("[DataConsistency]   UPDATE arc_original_voucher SET fonds_code = 'DEMO' WHERE fonds_code = 'XXX';");
            return true;
        }

        log.debug("[DataConsistency] 全宗代码引用完整性检查通过");
        return false;
    }

    /**
     * 检查：arc_original_voucher.voucher_type 是否存在于 sys_original_voucher_type.type_code
     */
    private boolean checkVoucherTypeReference() {
        String sql = """
            SELECT DISTINCT v.voucher_type, COUNT(*) as voucher_count
            FROM arc_original_voucher v
            WHERE v.deleted = 0
            AND NOT EXISTS (
                SELECT 1 FROM sys_original_voucher_type t 
                WHERE t.type_code = v.voucher_type 
                AND t.enabled = true
            )
            GROUP BY v.voucher_type
            """;

        List<TypeIssue> unknownTypes = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new TypeIssue(
                rs.getString("voucher_type"),
                rs.getInt("voucher_count")
            )
        );

        if (!unknownTypes.isEmpty()) {
            log.warn("[DataConsistency] 发现 {} 个未知类型代码：", unknownTypes.size());
            for (TypeIssue issue : unknownTypes) {
                log.warn("[DataConsistency]   - 类型代码: {}, 影响单据数: {} 条。" +
                        "解决方法：在 sys_original_voucher_type 表中添加该类型，或在 Service 中添加类型别名映射。",
                    issue.voucherType(), issue.voucherCount());
            }
            return true;
        }

        log.debug("[DataConsistency] 类型代码引用完整性检查通过");
        return false;
    }

    record FondsIssue(String fondsCode, int voucherCount) {}
    record TypeIssue(String voucherType, int voucherCount) {}
}
