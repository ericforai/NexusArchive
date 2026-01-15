// Input: JUnit 5、Mockito、Java 标准库、等
// Output: OfdConvertControllerTest 测试用例类（验证转换禁用）
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.OfdConvertService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class OfdConvertControllerTest {

    @Mock
    private OfdConvertService ofdConvertService;

    @InjectMocks
    private OfdConvertController ofdConvertController;

    @Test
    @DisplayName("convertToOfd should be disabled")
    void convertToOfd_disabled() {
        Result<Map<String, Object>> result = ofdConvertController.convertToOfd("archive-1");

        assertEquals(410, result.getCode());
        assertTrue(result.getMessage().contains("禁用"));
        verifyNoInteractions(ofdConvertService);
    }

    @Test
    @DisplayName("batchConvertToOfd should be disabled")
    void batchConvertToOfd_disabled() {
        Result<Integer> result = ofdConvertController.batchConvertToOfd(List.of("archive-1"));

        assertEquals(410, result.getCode());
        assertTrue(result.getMessage().contains("禁用"));
        verifyNoInteractions(ofdConvertService);
    }
}
