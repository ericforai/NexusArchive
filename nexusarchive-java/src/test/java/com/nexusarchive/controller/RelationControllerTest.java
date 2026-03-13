package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.relation.RelationGraphDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.IArchiveRelationService;
import com.nexusarchive.service.helper.RelationGraphHelper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RelationControllerTest {

    @Mock
    private ArchiveMapper archiveMapper;
    @Mock
    private RelationGraphHelper relationGraphHelper;
    @Mock
    private IArchiveRelationService archiveRelationService;

    @InjectMocks
    private RelationController relationController;

    @Test
    @DisplayName("getRelationGraph should resolve original voucher code to related accounting voucher")
    void getRelationGraph_shouldResolveOriginalVoucherCode() {
        String originalVoucherCode = "INV-202311-089";
        String originalVoucherId = "seed-invoice-001";
        String accountingVoucherId = "seed-voucher-001";

        when(archiveMapper.selectById(originalVoucherCode)).thenReturn(null);
        when(archiveMapper.selectOne(any())).thenReturn(null);

        OriginalVoucher originalVoucher = OriginalVoucher.builder()
            .id(originalVoucherId)
            .voucherNo(originalVoucherCode)
            .build();
        when(relationGraphHelper.resolveOriginalVoucher(originalVoucherCode)).thenReturn(originalVoucher);
        when(relationGraphHelper.findRelatedAccountingVoucherId(originalVoucherId)).thenReturn(accountingVoucherId);

        Archive centerArchive = new Archive();
        centerArchive.setId(accountingVoucherId);
        centerArchive.setArchiveCode("JZ-202311-0052");
        when(archiveMapper.selectById(accountingVoucherId)).thenReturn(centerArchive);

        RelationGraphDto mockDto = RelationGraphDto.builder()
                .centerId(accountingVoucherId)
                .nodes(List.of())
                .edges(List.of())
                .directionalView(RelationGraphDto.DirectionalView.builder().build())
                .build();
        when(relationGraphHelper.buildGraph(any(), any(), anyBoolean(), any(), any())).thenReturn(mockDto);
        when(archiveRelationService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(Collections.emptyList());

        Result<RelationGraphDto> result = relationController.getRelationGraph(originalVoucherCode);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(accountingVoucherId, result.getData().getCenterId());
    }

    @Test
    @DisplayName("getRelationGraph should resolve voucher from arc_file_content archival_code mapping")
    void getRelationGraph_shouldResolveFromArcFileContentArchivalCode() {
        String archivalCode = "INV-202311-093";
        String originalVoucherId = "seed-invoice-002";
        String accountingVoucherId = "seed-voucher-002";

        when(archiveMapper.selectById(archivalCode)).thenReturn(null);
        when(archiveMapper.selectOne(any())).thenReturn(null);

        OriginalVoucher originalVoucher = OriginalVoucher.builder()
            .id(originalVoucherId)
            .voucherNo("INV-202311-092")
            .build();
        when(relationGraphHelper.resolveOriginalVoucher(archivalCode)).thenReturn(originalVoucher);
        when(relationGraphHelper.findRelatedAccountingVoucherId(originalVoucherId)).thenReturn(accountingVoucherId);

        Archive centerArchive = new Archive();
        centerArchive.setId(accountingVoucherId);
        centerArchive.setArchiveCode("JZ-202311-0053");
        when(archiveMapper.selectById(accountingVoucherId)).thenReturn(centerArchive);

        RelationGraphDto mockDto = RelationGraphDto.builder()
                .centerId(accountingVoucherId)
                .nodes(List.of())
                .edges(List.of())
                .directionalView(RelationGraphDto.DirectionalView.builder().build())
                .build();
        when(relationGraphHelper.buildGraph(any(), any(), anyBoolean(), any(), any())).thenReturn(mockDto);
        when(archiveRelationService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(Collections.emptyList());

        Result<RelationGraphDto> result = relationController.getRelationGraph(archivalCode);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(accountingVoucherId, result.getData().getCenterId());
    }
}
