// Input: JUnit 5、Mockito、Jackson、MyBatis-Plus、本地模块
// Output: FondsHistoryServiceImplTest 测试类（校验全宗重命名审计快照）
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.entity.FondsHistory;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.FondsHistoryMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.BasFondsService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class FondsHistoryServiceImplTest {

    @Mock
    private FondsHistoryMapper fondsHistoryMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private BasFondsService basFondsService;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private LambdaQueryChainWrapper<BasFonds> oldFondsQuery;

    @Mock
    private LambdaQueryChainWrapper<BasFonds> existingFondsQuery;

    @InjectMocks
    private FondsHistoryServiceImpl fondsHistoryService;

    @BeforeEach
    void setUpTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), Archive.class);
    }

    @Test
    @DisplayName("全宗重命名会记录 before/after 审计快照")
    void renameFonds_recordsAuditSnapshots() {
        BasFonds oldFonds = new BasFonds();
        oldFonds.setId("fonds-001");
        oldFonds.setFondsCode("F001");
        oldFonds.setFondsName("历史全宗");
        oldFonds.setCompanyName("示例公司");
        oldFonds.setOrgId("ORG-001");
        oldFonds.setDescription("用于重命名测试");

        when(basFondsService.lambdaQuery()).thenReturn(oldFondsQuery, existingFondsQuery);
        when(oldFondsQuery.eq(anySFunction(), eq("F001"))).thenReturn(oldFondsQuery);
        when(oldFondsQuery.one()).thenReturn(oldFonds);
        when(existingFondsQuery.eq(anySFunction(), eq("F002"))).thenReturn(existingFondsQuery);
        when(existingFondsQuery.one()).thenReturn(null);
        when(archiveMapper.selectCount(any())).thenReturn(6L, 6L);
        when(archiveMapper.update(eq(null), any())).thenReturn(6);
        when(basFondsService.updateById(any(BasFonds.class))).thenReturn(true);
        when(fondsHistoryMapper.insert(any(FondsHistory.class))).thenAnswer(invocation -> {
            FondsHistory history = invocation.getArgument(0);
            history.setId("history-001");
            return 1;
        });

        String historyId = fondsHistoryService.renameFonds(
                "F001",
                "F002",
                LocalDate.of(2026, 3, 9),
                "组织代码统一",
                "AUTH-2026-0099",
                "operator-001"
        );

        assertThat(historyId).isEqualTo("history-001");
        assertThat(oldFonds.getFondsCode()).isEqualTo("F002");

        ArgumentCaptor<Map<String, Object>> beforeCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> afterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).logBusinessSnapshot(
                eq("operator-001"),
                eq("operator-001"),
                eq("FONDS_RENAME"),
                eq("FONDS_HISTORY"),
                eq("F001"),
                eq("SUCCESS"),
                eq("关键业务链路审计：全宗重命名"),
                eq("LOW"),
                beforeCaptor.capture(),
                afterCaptor.capture(),
                eq("F001"),
                eq("F002"),
                eq("AUTH-2026-0099")
        );

        assertThat(beforeCaptor.getValue()).containsEntry("fondsNo", "F001");
        assertThat(beforeCaptor.getValue()).containsEntry("archiveCount", 6L);
        assertThat(afterCaptor.getValue()).containsEntry("currentFondsNo", "F002");
        assertThat(afterCaptor.getValue()).containsEntry("historyId", "history-001");
        assertThat(afterCaptor.getValue()).containsEntry("approvalTicketId", "AUTH-2026-0099");
    }

    @SuppressWarnings("unchecked")
    private SFunction<BasFonds, ?> anySFunction() {
        return (SFunction<BasFonds, ?>) any(SFunction.class);
    }
}
