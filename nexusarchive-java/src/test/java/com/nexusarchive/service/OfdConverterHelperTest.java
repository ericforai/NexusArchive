// Input: JUnit 5、临时目录、OfdConverterHelper、仓库 PDF fixture
// Output: OfdConverterHelperTest 类
// Pos: 服务层转换能力验证测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.service.converter.OfdConverterHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OfdConverterHelperTest {

    @Test
    @Tag("unit")
    void convertsPdfFixtureToNonEmptyOfd(@TempDir Path tempDir) throws Exception {
        Path repoRoot = Path.of(System.getProperty("user.dir")).getParent();
        Path sourcePdf = repoRoot.resolve("dist/demo/reimb-form.pdf");
        Path targetOfd = tempDir.resolve("converted.ofd");

        new OfdConverterHelper().convertToOfd(sourcePdf, targetOfd);

        assertTrue(Files.exists(targetOfd), "应生成 OFD 文件");
        assertTrue(Files.size(targetOfd) > 0, "生成的 OFD 文件不应为空");
    }
}
