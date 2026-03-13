package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.AuditInspectionLogMapper;
import com.nexusarchive.service.compliance.ComplianceCheckFacade;
import com.nexusarchive.service.helper.ComplianceCheckHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ComplianceCheckServiceTest {

    @Mock
    private AuditInspectionLogMapper auditInspectionLogMapper;
    @Mock
    private ComplianceCheckFacade complianceCheckFacade;
    @Mock
    private ComplianceCheckHelper complianceCheckHelper;

    private ComplianceCheckService complianceCheckService;

    @BeforeEach
    void setUp() {
        complianceCheckService = new ComplianceCheckService(auditInspectionLogMapper, complianceCheckFacade, complianceCheckHelper);
        
        com.nexusarchive.service.compliance.ComplianceResult mockResult = new com.nexusarchive.service.compliance.ComplianceResult();
        lenient().when(complianceCheckFacade.checkCompliance(any(), any())).thenReturn(mockResult);
    }

    @Test
    @DisplayName("合规性检查基本功能验证")
    void testBasicComplianceCheck() {
        Archive archive = new Archive();
        archive.setCategoryCode("AC01");
        List<ArcFileContent> files = new ArrayList<>();
        
        var result = complianceCheckService.checkCompliance(archive, files);
        assertNotNull(result);
    }
}
