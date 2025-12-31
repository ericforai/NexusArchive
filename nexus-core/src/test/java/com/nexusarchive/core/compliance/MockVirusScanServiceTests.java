// Input: JUnit + MockVirusScanService
// Output: Mock 病毒扫描测试
// Pos: NexusCore tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockVirusScanServiceTests {
    private static final Path SAMPLES_DIR = Paths.get("src/test/resources/test-samples");
    
    private final MockVirusScanService virusScanService = new MockVirusScanService();

    @Test
    void shouldDetectEicarTestVirus() {
        Path eicar = SAMPLES_DIR.resolve("eicar.txt");
        VirusScanResult result = virusScanService.scan(eicar);
        
        assertFalse(result.clean());
        assertEquals("Eicar-Test-Signature", result.virusName());
        assertEquals("ClamAV", result.scanEngine());
    }

    @Test
    void shouldPassCleanFile() {
        Path validXml = SAMPLES_DIR.resolve("valid-invoice.xml");
        VirusScanResult result = virusScanService.scan(validXml);
        
        assertTrue(result.clean());
    }

    @Test
    void shouldPassFakePdf() {
        Path fakePdf = SAMPLES_DIR.resolve("fake-pdf.pdf");
        VirusScanResult result = virusScanService.scan(fakePdf);
        
        assertTrue(result.clean());
    }
}
