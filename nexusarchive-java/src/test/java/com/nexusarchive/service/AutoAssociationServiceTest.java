// Input: MyBatis-Plus、Jackson、org.junit、org.mockito、等
// Output: AutoAssociationServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusarchive.dto.parser.ParsedInvoice;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.strategy.AmountDateMatchStrategy;
import com.nexusarchive.service.strategy.ExactMatchStrategy;
import com.nexusarchive.service.strategy.MatchingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoAssociationServiceTest {

    @Mock
    private ArchiveMapper archiveMapper;
    @Mock
    private IArchiveRelationService archiveRelationService;

    private AutoAssociationService autoAssociationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        List<MatchingStrategy> strategies = Arrays.asList(
                new ExactMatchStrategy(objectMapper),
                new AmountDateMatchStrategy(objectMapper)
        );

        autoAssociationService = new AutoAssociationService(
                archiveMapper,
                archiveRelationService,
                strategies,
                objectMapper
        );
    }

    @Test
    void testTriggerAssociation_ExactMatch() throws Exception {
        // Prepare Voucher
        Archive voucher = new Archive();
        voucher.setId("v1");
        voucher.setCategoryCode("AC01");
        voucher.setFondsNo("F001");
        voucher.setFiscalYear("2023");
        voucher.setArchiveCode("V-001");
        
        VoucherHeadDto voucherHead = new VoucherHeadDto();
        voucherHead.setRemark("Payment for Invoice INV-123");
        voucherHead.setTotalAmount(new BigDecimal("100.00"));
        voucherHead.setVoucherDate(LocalDate.of(2023, 1, 1));
        voucher.setStandardMetadata(objectMapper.writeValueAsString(voucherHead));

        // Prepare Candidate Invoice
        Archive invoice = new Archive();
        invoice.setId("i1");
        invoice.setCategoryCode("AC04");
        invoice.setArchiveCode("I-001");
        
        ParsedInvoice parsedInvoice = new ParsedInvoice();
        parsedInvoice.setInvoiceNumber("INV-123");
        parsedInvoice.setTotalAmount(new BigDecimal("100.00"));
        parsedInvoice.setIssueDate(LocalDate.of(2023, 1, 5)); // Date mismatch > 3 days, but exact match should work
        invoice.setStandardMetadata(objectMapper.writeValueAsString(parsedInvoice));

        // Mock behaviors
        when(archiveMapper.selectById("v1")).thenReturn(voucher);
        when(archiveMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.singletonList(invoice));
        when(archiveRelationService.count(any(QueryWrapper.class))).thenReturn(0L);

        // Execute
        autoAssociationService.triggerAssociation("v1");

        // Verify Relation Created
        ArgumentCaptor<ArchiveRelation> captor = ArgumentCaptor.forClass(ArchiveRelation.class);
        verify(archiveRelationService).save(captor.capture());
        ArchiveRelation relation = captor.getValue();
        assertEquals("v1", relation.getSourceId());
        assertEquals("i1", relation.getTargetId());
        assertEquals("SYSTEM_AUTO", relation.getRelationType());
    }

    @Test
    void testTriggerAssociation_AmountDateMatch() throws Exception {
        // Prepare Voucher
        Archive voucher = new Archive();
        voucher.setId("v2");
        voucher.setCategoryCode("AC01");
        voucher.setFondsNo("F001");
        voucher.setFiscalYear("2023");
        
        VoucherHeadDto voucherHead = new VoucherHeadDto();
        voucherHead.setRemark("Generic Payment");
        voucherHead.setTotalAmount(new BigDecimal("500.00"));
        voucherHead.setVoucherDate(LocalDate.of(2023, 5, 10));
        voucher.setStandardMetadata(objectMapper.writeValueAsString(voucherHead));

        // Prepare Candidate Invoice
        Archive invoice = new Archive();
        invoice.setId("i2");
        invoice.setCategoryCode("AC04");
        
        ParsedInvoice parsedInvoice = new ParsedInvoice();
        parsedInvoice.setInvoiceNumber("INV-999"); // No match in remark
        parsedInvoice.setTotalAmount(new BigDecimal("500.00")); // Amount match
        parsedInvoice.setIssueDate(LocalDate.of(2023, 5, 12)); // Date within 3 days
        invoice.setStandardMetadata(objectMapper.writeValueAsString(parsedInvoice));

        // Mock behaviors
        when(archiveMapper.selectById("v2")).thenReturn(voucher);
        when(archiveMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.singletonList(invoice));
        when(archiveRelationService.count(any(QueryWrapper.class))).thenReturn(0L);

        // Execute
        autoAssociationService.triggerAssociation("v2");

        // Verify Relation Created
        verify(archiveRelationService).save(any(ArchiveRelation.class));
    }
}
