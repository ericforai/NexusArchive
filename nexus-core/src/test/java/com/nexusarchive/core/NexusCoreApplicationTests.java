// Input: Spring Boot Test
// Output: Sprint 0 基线启动测试 (排除数据源依赖)
// Pos: NexusCore 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import com.nexusarchive.core.mapper.ArchiveObjectMapper;
import com.nexusarchive.core.mapper.FileContentMapper;
import com.nexusarchive.core.mapper.PreservationAuditMapper;
import com.nexusarchive.core.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
class NexusCoreApplicationTests {
    @MockBean
    private ArchiveObjectMapper archiveObjectMapper;

    @MockBean
    private FileContentMapper fileContentMapper;

    @MockBean
    private PreservationAuditMapper preservationAuditMapper;

    @MockBean
    private StorageService storageService;

    @Test
    void contextLoads() {
    }
}
