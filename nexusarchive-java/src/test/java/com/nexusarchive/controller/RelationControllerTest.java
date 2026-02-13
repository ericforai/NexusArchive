package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.dto.relation.RelationGraphDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.AttachmentService;
import com.nexusarchive.service.IAutoAssociationService;
import com.nexusarchive.service.IArchiveRelationService;
import com.nexusarchive.service.relation.RelationDirectionResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RelationControllerTest {

    @Mock
    private IAutoAssociationService autoAssociationService;
    @Mock
    private ArchiveService archiveService;
    @Mock
    private ArchiveMapper archiveMapper;
    @Mock
    private IArchiveRelationService archiveRelationService;
    @Mock
    private AttachmentService attachmentService;
    @Mock
    private VoucherRelationMapper voucherRelationMapper;
    @Mock
    private OriginalVoucherMapper originalVoucherMapper;
    @Mock
    private OriginalVoucherFileMapper originalVoucherFileMapper;
    @Mock
    private ArcFileContentMapper arcFileContentMapper;
    @Mock
    private RelationDirectionResolver relationDirectionResolver;

    @InjectMocks
    private RelationController relationController;

    @Test
    @DisplayName("getRelationGraph should resolve original voucher code to related accounting voucher")
    void getRelationGraph_shouldResolveOriginalVoucherCode() {
        String originalVoucherCode = "INV-202311-089";
        String originalVoucherId = "seed-invoice-001";
        String accountingVoucherId = "seed-voucher-001";

        // archive table does not contain the input INV code
        when(archiveMapper.selectById(originalVoucherCode)).thenReturn(null);
        when(archiveMapper.selectOne(any())).thenReturn(null);

        OriginalVoucher originalVoucher = OriginalVoucher.builder()
            .id(originalVoucherId)
            .voucherNo(originalVoucherCode)
            .sourceDocId(originalVoucherCode)
            .fondsCode("DEMO")
            .fiscalYear("2023")
            .build();
        when(originalVoucherMapper.selectById(originalVoucherCode)).thenReturn(null);
        when(originalVoucherMapper.selectOne(any())).thenReturn(originalVoucher);

        VoucherRelation relation = new VoucherRelation();
        relation.setAccountingVoucherId(accountingVoucherId);
        when(voucherRelationMapper.findByOriginalVoucherId(originalVoucherId)).thenReturn(List.of(relation));

        Archive centerArchive = new Archive();
        centerArchive.setId(accountingVoucherId);
        centerArchive.setArchiveCode("JZ-202311-0052");
        centerArchive.setFondsNo("DEMO");
        centerArchive.setStatus("archived");
        when(archiveMapper.selectById(accountingVoucherId)).thenReturn(centerArchive);

        when(archiveRelationService.list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<ArchiveRelation>>any()))
            .thenReturn(Collections.emptyList());
        when(attachmentService.getAttachmentLinks(accountingVoucherId)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(accountingVoucherId)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any())).thenReturn(List.of(centerArchive));
        when(relationDirectionResolver.resolve(any(), any()))
            .thenReturn(RelationGraphDto.DirectionalView.builder()
                .upstream(List.of())
                .downstream(List.of())
                .layers(java.util.Map.of())
                .mainline(List.of())
                .build());

        Result<RelationGraphDto> result = relationController.getRelationGraph(originalVoucherCode);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(accountingVoucherId, result.getData().getCenterId());
        assertNotNull(result.getData().getDirectionalView());
    }

    @Test
    @DisplayName("getRelationGraph should resolve voucher from arc_file_content archival_code mapping")
    void getRelationGraph_shouldResolveFromArcFileContentArchivalCode() {
        String archivalCode = "INV-202311-093";
        String originalVoucherId = "seed-invoice-002";
        String accountingVoucherId = "seed-voucher-002";

        when(archiveMapper.selectById(archivalCode)).thenReturn(null);
        when(archiveMapper.selectOne(any())).thenReturn(null);
        when(originalVoucherMapper.selectById(archivalCode)).thenReturn(null);
        when(originalVoucherMapper.selectOne(any())).thenReturn(null);

        ArcFileContent fileContent = new ArcFileContent();
        fileContent.setItemId(originalVoucherId);
        when(arcFileContentMapper.selectOne(any())).thenReturn(fileContent);

        OriginalVoucher originalVoucher = OriginalVoucher.builder()
            .id(originalVoucherId)
            .voucherNo("INV-202311-092")
            .fondsCode("DEMO")
            .fiscalYear("2023")
            .build();
        when(originalVoucherMapper.selectById(originalVoucherId)).thenReturn(originalVoucher);

        VoucherRelation relation = new VoucherRelation();
        relation.setAccountingVoucherId(accountingVoucherId);
        when(voucherRelationMapper.findByOriginalVoucherId(originalVoucherId)).thenReturn(List.of(relation));

        Archive centerArchive = new Archive();
        centerArchive.setId(accountingVoucherId);
        centerArchive.setArchiveCode("JZ-202311-0053");
        centerArchive.setFondsNo("DEMO");
        centerArchive.setStatus("archived");
        when(archiveMapper.selectById(accountingVoucherId)).thenReturn(centerArchive);

        when(archiveRelationService.list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<ArchiveRelation>>any()))
            .thenReturn(Collections.emptyList());
        when(attachmentService.getAttachmentLinks(accountingVoucherId)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(accountingVoucherId)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any())).thenReturn(List.of(centerArchive));
        when(relationDirectionResolver.resolve(any(), any()))
            .thenReturn(RelationGraphDto.DirectionalView.builder()
                .upstream(List.of())
                .downstream(List.of())
                .layers(java.util.Map.of())
                .mainline(List.of())
                .build());

        Result<RelationGraphDto> result = relationController.getRelationGraph(archivalCode);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(accountingVoucherId, result.getData().getCenterId());
        assertNotNull(result.getData().getDirectionalView());
    }
}
