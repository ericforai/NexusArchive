// Input: JUnit 5、Mockito、MyBatis-Plus、MyBatis、Java 标准库、等
// Output: PreArchiveSubmitServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.converter.OfdConverterHelper;
import com.nexusarchive.service.signature.OfdSignatureHelper;
import com.nexusarchive.service.strategy.ArchivalCodeGenerator;
import com.nexusarchive.util.FileHashUtil;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PreArchiveSubmitServiceTest {

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private ArchiveApprovalService archiveApprovalService;

    @Mock
    private OfdConverterHelper ofdConverterHelper;

    @Mock
    private OfdSignatureHelper ofdSignatureHelper;

    @Mock
    private FileHashUtil fileHashUtil;

    @Mock
    private ArchivalCodeGenerator archivalCodeGeneratorStrategy;

    @InjectMocks
    private PreArchiveSubmitService preArchiveSubmitService;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        TableInfoHelper.initTableInfo(assistant, Archive.class);
        TableInfoHelper.initTableInfo(assistant, ArcFileContent.class);
    }

    @Test
    @DisplayName("completeArchival should skip OFD conversion when disabled")
    void completeArchival_skipsOfdConversion() throws Exception {
        Archive archive = new Archive();
        archive.setId("archive-1");
        archive.setArchiveCode("AC-001");
        archive.setStatus("PENDING");

        when(archiveMapper.selectById("archive-1")).thenReturn(archive);
        when(archiveMapper.update(any(), any())).thenReturn(1);

        ArcFileContent file = new ArcFileContent();
        file.setId("file-1");
        file.setArchivalCode("AC-001");
        file.setFileName("sample.pdf");
        file.setStoragePath("/tmp/sample.pdf");

        when(arcFileContentMapper.selectList(any())).thenReturn(List.of(file));
        doThrow(new IOException("sign disabled")).when(ofdSignatureHelper).signOfd(any(), any(), any(), any());
        assertDoesNotThrow(() -> preArchiveSubmitService.completeArchival("archive-1"));

        verify(ofdConverterHelper, never()).convertToOfd(any(), any());
        verify(arcFileContentMapper).updateById(file);
    }
}
