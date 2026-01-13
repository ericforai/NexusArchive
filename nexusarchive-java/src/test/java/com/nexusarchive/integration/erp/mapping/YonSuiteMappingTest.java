package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.VoucherEntryDto;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse.VoucherBody;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse.VoucherHeader;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse.VoucherRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("unit")
public class YonSuiteMappingTest {

    private DefaultErpMapper mapper;

    @Mock
    private MappingConfigLoader configLoader;

    private GroovyMappingEngine scriptEngine;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        scriptEngine = new GroovyMappingEngine();
        mapper = new DefaultErpMapper(configLoader, scriptEngine);
        
        MappingConfig config = new MappingConfig();
        config.setSourceSystem("yonsuite");
        
        // Entries mapping
        ObjectMapping entryMapping = new ObjectMapping();
        entryMapping.setSource("body");
        
        java.util.Map<String, FieldMapping> itemMappings = new java.util.HashMap<>();
        
        // Existing mappings
        itemMappings.put("lineNo", FieldMapping.builder().field("recordnumber").build());
        itemMappings.put("summary", FieldMapping.builder().field("description").build());
        itemMappings.put("subjectCode", FieldMapping.builder().field("accsubject.code").build());
        itemMappings.put("subjectName", FieldMapping.builder().field("accsubject.name").build());
        
        // NEW mappings we added
        itemMappings.put("currencyCode", FieldMapping.builder().field("currency.code").build());
        itemMappings.put("currencyName", FieldMapping.builder().field("currency.name").build());
        itemMappings.put("debitOriginal", FieldMapping.builder().field("debitOriginal").build());
        itemMappings.put("creditOriginal", FieldMapping.builder().field("creditOriginal").build());
        itemMappings.put("exchangeRate", FieldMapping.builder().field("exchangeRate").build());

        // Complex scripts for amount/direction
        itemMappings.put("amount", FieldMapping.builder().script("def debit = it.debitOrg ?: 0; def credit = it.creditOrg ?: 0; (debit != null && debit > 0) ? debit : credit").build());
        itemMappings.put("direction", FieldMapping.builder().script("import com.nexusarchive.common.enums.DirectionType; def debit = it.debitOrg ?: 0; def credit = it.creditOrg ?: 0; if (debit != null && debit > 0) DirectionType.DEBIT else if (credit != null && credit > 0) DirectionType.CREDIT else null").build());

        entryMapping.setItem(itemMappings);
        config.setEntries(entryMapping);
        
        // Header mapping
        java.util.Map<String, FieldMapping> headerMappings = new java.util.HashMap<>();
        headerMappings.put("voucherNumber", FieldMapping.builder().field("header.displayname").build());
        headerMappings.put("accountPeriod", FieldMapping.builder().field("header.period").build());
        config.setHeaderMappings(headerMappings);

        when(configLoader.loadMapping("yonsuite")).thenReturn(config);
    }

    @Test
    void testCurrencyMapping() {
        // Arrange
        VoucherRecord record = new VoucherRecord();
        VoucherHeader header = new VoucherHeader();
        header.setDisplayname("记-1001");
        header.setPeriod("2024-01");
        record.setHeader(header);

        VoucherBody body = new VoucherBody();
        body.setRecordnumber(1);
        body.setDescription("Test Entry");
        
        // Mock currency data
        YonVoucherListResponse.Currency currency = new YonVoucherListResponse.Currency();
        currency.setCode("USD");
        currency.setName("美元");
        body.setCurrency(currency);
        
        body.setDebitOriginal(new BigDecimal("100.00"));
        body.setCreditOriginal(BigDecimal.ZERO);
        body.setExchangeRate(new BigDecimal("7.1"));
        
        body.setDebitOrg(new BigDecimal("710.00")); // 100 * 7.1
        body.setCreditOrg(BigDecimal.ZERO);
        
        record.setBody(List.of(body));
        
        ErpConfig erpConfig = new ErpConfig();

        // Act
        AccountingSipDto sipDto = mapper.mapToSipDto(record, "yonsuite", erpConfig);

        // Assert
        assertNotNull(sipDto);
        assertNotNull(sipDto.getEntries());
        assertEquals(1, sipDto.getEntries().size());
        
        VoucherEntryDto entry = sipDto.getEntries().get(0);
        assertEquals("USD", entry.getCurrencyCode());
        assertEquals("美元", entry.getCurrencyName());
        assertEquals(new BigDecimal("100.00"), entry.getDebitOriginal());
        assertEquals(BigDecimal.ZERO, entry.getCreditOriginal());
        assertEquals(new BigDecimal("7.1"), entry.getExchangeRate());
        
        // Check computed amount from script
        assertEquals(new BigDecimal("710.00"), entry.getAmount());
    }
}
