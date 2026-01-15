// Input: JUnit 5, Mockito, VoucherPersistenceService
// Output: VoucherPersistenceServiceTest test cases
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.erp;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.engine.ErpMappingEngine;
import com.nexusarchive.service.VoucherPdfGeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class VoucherPersistenceServiceTest {

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private VoucherPdfGeneratorService pdfGeneratorService;

    @Mock
    private ErpMappingEngine mappingEngine;

    @Mock
    private ErpAdapter adapter;

    @InjectMocks
    private VoucherPersistenceService voucherPersistence;

    @Test
    @DisplayName("Save voucher should persist sourceData before generating PDF")
    void saveVoucher_persistsSourceDataBeforePdfGeneration() {
        VoucherDTO dto = VoucherDTO.builder()
            .voucherId("voucher-1")
            .voucherNo("记-4")
            .voucherDate(LocalDate.of(2025, 8, 9))
            .debitTotal(new BigDecimal("100.00"))
            .creator("tester")
            .build();
        String voucherJson = "{\"voucherNo\":\"记-4\"}";

        when(arcFileContentMapper.selectCount(any())).thenReturn(0L);
        when(arcFileContentMapper.insert(any(ArcFileContent.class))).thenReturn(1);
        when(arcFileMetadataIndexMapper.insert(any(ArcFileMetadataIndex.class))).thenReturn(1);
        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);
        when(adapter.getName()).thenReturn("YonSuite");

        ArcFileContent saved = voucherPersistence.saveVoucher(
            dto,
            null,
            adapter,
            LocalDate.of(2025, 8, 9),
            "YonSuite",
            voucherJson
        );

        ArgumentCaptor<ArcFileContent> contentCaptor = ArgumentCaptor.forClass(ArcFileContent.class);
        verify(arcFileContentMapper).insert(contentCaptor.capture());
        ArcFileContent inserted = contentCaptor.getValue();

        assertEquals(voucherJson, inserted.getSourceData());
        verify(pdfGeneratorService).generatePdfForPreArchive(inserted.getId(), voucherJson);
        assertNotNull(saved);
    }
}
