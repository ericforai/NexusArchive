// Input: JUnit, Mockito, MyBatis-Plus
// Output: PoolServiceImplTest test cases
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PoolServiceImplTest {

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;

    @InjectMocks
    private PoolServiceImpl poolService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), ArcFileContent.class);
    }

    @Test
    void listByStatus_shouldNormalizeLegacyStatus() {
        when(arcFileContentMapper.selectList(any())).thenReturn(Collections.emptyList());

        poolService.listByStatus("PENDING_ARCHIVE", null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<ArcFileContent>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(arcFileContentMapper).selectList(captor.capture());

        LambdaQueryWrapper<ArcFileContent> wrapper = captor.getValue();
        wrapper.getSqlSegment();
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        assertTrue(params.containsValue("READY_TO_ARCHIVE"), "Params: " + params);
    }
}
