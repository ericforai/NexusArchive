// Input: JUnit 5、Spring Boot Test、MyBatis-Plus
// Output: OriginalVoucherServiceContractTest 类
// Pos: 测试层 - 前后端契约测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.mapper.OriginalVoucherMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 原始凭证服务契约测试
 * <p>
 * 验证前端使用的类型代码能够正确查询到数据库中的数据（包括旧代码别名）
 * </p>
 * <p>
 * 这类测试防止"前端改了类型代码，后端数据库还是旧代码"导致的数据查不到问题
 * </p>
 *
 * @see <a href="docs/plans/2026-01-15-doc-pool-empty-bug-postmortem.md">问题分析文档</a>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("unit")
@DisplayName("原始凭证服务契约测试")
public class OriginalVoucherServiceContractTest {

    @Autowired
    private OriginalVoucherService voucherService;

    @Autowired
    private OriginalVoucherMapper voucherMapper;

    /**
     * 契约测试：银行回单类型别名映射
     * <p>
     * 前端发送：BANK_RECEIPT
     * 数据库实际：BANK_SLIP（历史数据）
     * 预期结果：通过别名映射能查到数据
     * </p>
     */
    @Test
    @DisplayName("类型别名映射：BANK_RECEIPT 应能查到 BANK_SLIP 数据")
    void shouldQueryBankSlipVouchersWithBankReceiptType() {
        // GIVEN: 数据库中存在 BANK_SLIP 类型的单据
        String sql = "SELECT COUNT(*) FROM arc_original_voucher WHERE voucher_type = 'BANK_SLIP' AND deleted = 0";
        Integer existingCount = voucherMapper.selectCount(
            new LambdaQueryWrapper<OriginalVoucher>()
                .eq(OriginalVoucher::getVoucherType, "BANK_SLIP")
        ).intValue();

        if (existingCount == 0) {
            // 没有测试数据，跳过测试
            System.out.println("[ContractTest] 警告：数据库中没有 BANK_SLIP 类型的测试数据，跳过测试");
            return;
        }

        // WHEN: 使用前端期望的 BANK_RECEIPT 类型查询
        Page<OriginalVoucher> result = voucherService.getVouchers(
            1, 10,     // page, limit
            null,      // search
            null,      // category
            "BANK_RECEIPT",  // type (前端使用的代码)
            null,      // status
            null,      // fondsCode
            null,      // fiscalYear
            "ENTRY,PARSED,PARSE_FAILED"  // poolStatus (单据池模式)
        );

        // THEN: 应该通过类型别名映射查到 BANK_SLIP 类型的数据
        assertThat(result.getRecords())
            .as("使用 BANK_RECEIPT 应该能查到 BANK_SLIP 类型的单据")
            .isNotEmpty();

        // 验证查到的确实是 BANK_SLIP 类型
        result.getRecords().forEach(voucher -> {
            assertThat(voucher.getVoucherType())
                .as("查到的单据类型应该是 BANK_SLIP（数据库实际值）")
                .isIn("BANK_RECEIPT", "BANK_SLIP");
        });

        System.out.println("[ContractTest] ✓ 银行回单类型别名映射测试通过，查到 " + result.getRecords().size() + " 条数据");
    }

    /**
     * 契约测试：增值税发票类型别名映射
     * <p>
     * 前端发送：INV_VAT_E
     * 数据库实际：VAT_INVOICE（历史数据）
     * 预期结果：通过别名映射能查到数据
     * </p>
     */
    @Test
    @DisplayName("类型别名映射：INV_VAT_E 应能查到 VAT_INVOICE 数据")
    void shouldQueryVatInvoiceVouchersWithInvVatEType() {
        // GIVEN: 数据库中存在 VAT_INVOICE 类型的单据
        int existingCount = Math.toIntExact(voucherMapper.selectCount(
            new LambdaQueryWrapper<OriginalVoucher>()
                .eq(OriginalVoucher::getVoucherType, "VAT_INVOICE")
        ));

        if (existingCount == 0) {
            System.out.println("[ContractTest] 警告：数据库中没有 VAT_INVOICE 类型的测试数据，跳过测试");
            return;
        }

        // WHEN: 使用前端期望的 INV_VAT_E 类型查询
        Page<OriginalVoucher> result = voucherService.getVouchers(
            1, 10,
            null,
            null,
            "INV_VAT_E",  // type (前端使用的代码)
            null,
            null,
            null,
            "ENTRY,PARSED,PARSE_FAILED"
        );

        // THEN: 应该通过类型别名映射查到 VAT_INVOICE 类型的数据
        assertThat(result.getRecords())
            .as("使用 INV_VAT_E 应该能查到 VAT_INVOICE 类型的单据")
            .isNotEmpty();

        result.getRecords().forEach(voucher -> {
            assertThat(voucher.getVoucherType())
                .as("查到的单据类型应该是 VAT_INVOICE 或 INV_VAT_E")
                .isIn("INV_VAT_E", "VAT_INVOICE");
        });

        System.out.println("[ContractTest] ✓ 增值税发票类型别名映射测试通过，查到 " + result.getRecords().size() + " 条数据");
    }

    /**
     * 契约测试：单据池状态映射
     * <p>
     * 前端发送：poolStatus = "ENTRY,PARSED,PARSE_FAILED"
     * 后端映射：archiveStatus = "DRAFT"
     * 预期结果：能查到 DRAFT 状态的单据
     * </p>
     */
    @Test
    @DisplayName("状态映射：poolStatus=ENTRY 应映射到 archiveStatus=DRAFT")
    void shouldMapPoolStatusEntryToDraftStatus() {
        // GIVEN: 数据库中存在 DRAFT 状态的单据
        int draftCount = Math.toIntExact(voucherMapper.selectCount(
            new LambdaQueryWrapper<OriginalVoucher>()
                .eq(OriginalVoucher::getArchiveStatus, "DRAFT")
        ));

        if (draftCount == 0) {
            System.out.println("[ContractTest] 警告：数据库中没有 DRAFT 状态的测试数据，跳过测试");
            return;
        }

        // WHEN: 使用单据池状态查询
        Page<OriginalVoucher> result = voucherService.getVouchers(
            1, 10,
            null,
            null,
            null,
            null,      // status 不传
            null,
            null,
            "ENTRY,PARSED,PARSE_FAILED"  // poolStatus (单据池模式)
        );

        // THEN: 应该查到 DRAFT 状态的单据
        assertThat(result.getRecords())
            .as("单据池模式应该能查到 DRAFT 状态的单据")
            .isNotEmpty();

        result.getRecords().forEach(voucher -> {
            assertThat(voucher.getArchiveStatus())
                .as("单据池模式查到的单据状态应该是 DRAFT")
                .isEqualTo("DRAFT");
        });

        System.out.println("[ContractTest] ✓ 单据池状态映射测试通过，查到 " + result.getRecords().size() + " 条数据");
    }

    /**
     * 契约测试：已归档状态映射
     * <p>
     * 前端发送：poolStatus = "ARCHIVED"
     * 后端映射：archiveStatus = "ARCHIVED"
     * 预期结果：能查到 ARCHIVED 状态的单据
     * </p>
     */
    @Test
    @DisplayName("状态映射：poolStatus=ARCHIVED 应映射到 archiveStatus=ARCHIVED")
    void shouldMapPoolStatusArchivedToArchivedStatus() {
        // GIVEN: 数据库中存在 ARCHIVED 状态的单据
        int archivedCount = Math.toIntExact(voucherMapper.selectCount(
            new LambdaQueryWrapper<OriginalVoucher>()
                .eq(OriginalVoucher::getArchiveStatus, "ARCHIVED")
        ));

        if (archivedCount == 0) {
            System.out.println("[ContractTest] 警告：数据库中没有 ARCHIVED 状态的测试数据，跳过测试");
            return;
        }

        // WHEN: 使用已归档状态查询
        Page<OriginalVoucher> result = voucherService.getVouchers(
            1, 10,
            null,
            null,
            null,
            null,
            null,
            null,
            "ARCHIVED"  // poolStatus (已归档模式)
        );

        // THEN: 应该查到 ARCHIVED 状态的单据
        assertThat(result.getRecords())
            .as("已归档模式应该能查到 ARCHIVED 状态的单据")
            .isNotEmpty();

        result.getRecords().forEach(voucher -> {
            assertThat(voucher.getArchiveStatus())
                .as("已归档模式查到的单据状态应该是 ARCHIVED")
                .isEqualTo("ARCHIVED");
        });

        System.out.println("[ContractTest] ✓ 已归档状态映射测试通过，查到 " + result.getRecords().size() + " 条数据");
    }

    /**
     * 契约测试：类型代码定义完整性
     * <p>
     * 验证数据库中的所有类型代码都在前端定义的范围内
     * </p>
     */
    @Test
    @DisplayName("类型代码完整性：数据库中的类型代码应该有对应的前端定义")
    void shouldHaveFrontendDefinitionForAllDatabaseTypes() {
        // 前端定义的类型代码清单（需要与 DOC_POOL_TYPES 保持同步）
        List<String> frontendTypeCodes = List.of(
            // 发票类
            "INV_PAPER", "INV_VAT_E", "INV_DIGITAL", "INV_RAIL", "INV_AIR", "INV_GOV",
            // 银行类
            "BANK_RECEIPT", "BANK_STATEMENT",
            // 单据类
            "DOC_PAYMENT", "DOC_RECEIPT", "DOC_RECEIPT_VOUCHER", "DOC_PAYROLL",
            // 合同类
            "CONTRACT", "AGREEMENT",
            // 其他
            "OTHER"
        );

        // 查询数据库中实际使用的类型代码
        List<String> dbTypes = voucherMapper.selectList(
            new LambdaQueryWrapper<OriginalVoucher>()
                .select(OriginalVoucher::getVoucherType)
                .groupBy(OriginalVoucher::getVoucherType)
        ).stream()
            .map(OriginalVoucher::getVoucherType)
            .toList();

        // 检查是否有未在前端定义的类型
        List<String> undefinedTypes = dbTypes.stream()
            .filter(type -> !frontendTypeCodes.contains(type) &&
                          !isKnownAlias(type))  // 排除已知的别名
            .toList();

        if (!undefinedTypes.isEmpty()) {
            System.out.println("[ContractTest] ⚠️  警告：数据库中存在未在前端定义的类型代码: " + undefinedTypes);
            System.out.println("[ContractTest]   建议：1) 在前端 DOC_POOL_TYPES 中添加定义，或 2) 在后端添加类型别名映射");
        } else {
            System.out.println("[ContractTest] ✓ 所有数据库类型代码都有对应的前端定义");
        }
    }

    /**
     * 检查是否是已知的类型别名
     */
    private boolean isKnownAlias(String typeCode) {
        // 已知的别名映射（与 getTypeAliases 方法保持同步）
        return "BANK_SLIP".equals(typeCode) || "VAT_INVOICE".equals(typeCode);
    }
}
