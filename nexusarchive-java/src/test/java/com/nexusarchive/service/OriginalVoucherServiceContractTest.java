// Input: JUnit 5、Mockito、MyBatis-Plus
// Output: OriginalVoucherServiceContractTest 类
// Pos: 测试层 - 前后端契约测试 (已重构为纯单元测试以避免数据库依赖)
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.mapper.OriginalVoucherTypeMapper;
import com.nexusarchive.service.parser.PdfInvoiceParser;
import com.nexusarchive.service.helper.OriginalVoucherHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 原始凭证服务契约测试
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("原始凭证服务契约测试")
public class OriginalVoucherServiceContractTest {

    @Mock
    private OriginalVoucherMapper voucherMapper;
    @Mock
    private OriginalVoucherFileMapper fileMapper;
    @Mock
    private VoucherRelationMapper relationMapper;
    @Mock
    private OriginalVoucherTypeMapper typeMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private PdfInvoiceParser pdfInvoiceParser;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private OriginalVoucherHelper helper;

    @InjectMocks
    private OriginalVoucherService voucherService;

    @Test
    @DisplayName("类型别名映射：BANK_RECEIPT 应能查到 BANK_SLIP 数据")
    void shouldQueryBankSlipVouchersWithBankReceiptType() {
        // Given
        OriginalVoucher voucher = new OriginalVoucher();
        voucher.setVoucherType("BANK_SLIP");
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(voucher));

        when(voucherMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(helper.getTypeAliases("BANK_RECEIPT")).thenReturn(List.of("BANK_RECEIPT", "BANK_SLIP"));

        // When
        Page<OriginalVoucher> result = voucherService.getVouchers(
            1, 10, null, null, "BANK_RECEIPT", null, null, null, "ENTRY"
        );

        // Then
        assertThat(result.getRecords()).isNotEmpty();
        assertThat(result.getRecords().get(0).getVoucherType()).isEqualTo("BANK_SLIP");
    }
}
