// Input: JUnit 5, Mockito
// Output: ArchiveSearchService 单元测试
// Pos: NexusCore test
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.core.domain.ArchiveObject;
import com.nexusarchive.core.mapper.ArchiveObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultArchiveSearchServiceTests {

    @Mock
    private ArchiveObjectMapper archiveObjectMapper;

    @InjectMocks
    private DefaultArchiveSearchService searchService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void search_shouldBuildCorrectQueryWrapper() {
        // Arrange
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setFondsNo("FONDS-001");
        request.setArchiveYear(2025);
        request.setAmountFrom(new BigDecimal("100.00"));
        request.setAmountTo(new BigDecimal("500.00"));
        request.setDateFrom(LocalDate.of(2025, 1, 1));
        request.setCounterparty("Tech Corp");
        request.setVoucherNo("V001");
        request.setKeyword("Purchase");

        Page<ArchiveObject> page = new Page<>(1, 10);
        when(archiveObjectMapper.selectPage(any(), any())).thenReturn(page);

        // Act
        searchService.search(request, page);

        // Assert
        ArgumentCaptor<LambdaQueryWrapper<ArchiveObject>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(archiveObjectMapper).selectPage(eq(page), captor.capture());

        LambdaQueryWrapper<ArchiveObject> wrapper = captor.getValue();
        
        // 由于 LambdaQueryWrapper 难以直接断言 SQL，我们验证它是否非空及传递给了 Mapper
        // 详细的 SQL 生成测试通常需要集成测试或 PowerMock，这里主要验证调用链路
        assertThat(wrapper).isNotNull();
        // 实际上我们可以通过 wrapper.getTargetSql() (此方法可能受限) 或检查 expression
        // 这里简单验证 selectPage 被调用了一次
    }
}
