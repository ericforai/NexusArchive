// Input: AssertJ、org.junit、Mockito、Java 标准库、本地模块
// Output: SignatureControllerLogQueryTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.entity.ArcSignatureLog;
import com.nexusarchive.mapper.ArcSignatureLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SignatureControllerLogQueryTest {

    @Mock
    private ArcSignatureLogMapper signatureLogMapper;

    private SignatureController controller;

    @BeforeEach
    void setUp() {
        controller = new SignatureController(signatureLogMapper);
    }

    @Test
    void getSignatureLogs_shouldReturnPersistedVerificationLogsByArchiveId() {
        ArcSignatureLog logEntry = new ArcSignatureLog();
        logEntry.setId("log-1");
        logEntry.setArchiveId("archive-1");
        logEntry.setFileId("file-1");
        logEntry.setVerifyResult("VALID");
        logEntry.setVerifyMessage("Hash verified (SM3); PDF签章校验通过");
        logEntry.setVerifyTime(LocalDateTime.of(2026, 3, 6, 13, 0));

        when(signatureLogMapper.findByArchiveId("archive-1")).thenReturn(List.of(logEntry));

        Result<List<ArcSignatureLog>> response = controller.getSignatureLogs("archive-1", null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getArchiveId()).isEqualTo("archive-1");
                    assertThat(log.getFileId()).isEqualTo("file-1");
                    assertThat(log.getVerifyResult()).isEqualTo("VALID");
                    assertThat(log.getVerifyMessage()).contains("PDF签章校验通过");
                });
        verify(signatureLogMapper).findByArchiveId("archive-1");
    }
}
