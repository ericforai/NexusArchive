// Input: JUnit 5, Mockito, LicenseService
// Output: Test results for LicenseService
// Pos: Test Layer - Service Testing
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseServiceTest {

    @Test
    void testGetLicensePath_ExplicitConfig(@TempDir Path tempDir) throws IOException {
        LicenseService service = new LicenseService();
        Path licenseFile = tempDir.resolve("custom-license.json");
        Files.writeString(licenseFile, "{}");

        ReflectionTestUtils.setField(service, "licenseFilePath", licenseFile.toString());
        ReflectionTestUtils.setField(service, "archiveRootPath", "/tmp/archives");

        Path result = invokeGetLicensePath(service);
        assertEquals(licenseFile.toAbsolutePath(), result.toAbsolutePath());
    }

    @Test
    void testGetLicensePath_DerivedPathExists(@TempDir Path tempDir) throws IOException {
        LicenseService service = new LicenseService();
        
        // Structure: /tempDir/data/archives (archiveRootPath)
        // Expected: /tempDir/data/license.json
        Path dataDir = tempDir.resolve("data");
        Path archivesDir = dataDir.resolve("archives");
        Files.createDirectories(archivesDir);
        
        Path expectedLicense = dataDir.resolve("license.json");
        Files.writeString(expectedLicense, "{}");

        ReflectionTestUtils.setField(service, "archiveRootPath", archivesDir.toString());

        Path result = invokeGetLicensePath(service);
        assertEquals(expectedLicense.toAbsolutePath(), result.toAbsolutePath());
    }

    @Test
    void testGetLicensePath_Fallback() {
        // This test assumes ./data/license.json might or might not exist in the test env.
        // We mainly want to verify that when derived path is missing, it returns something reasonable.
        // If ./data/license.json exists in the project root (where tests run), it turns the fallback.
        
        LicenseService service = new LicenseService();
        // Point to a non-existent derived path
        ReflectionTestUtils.setField(service, "archiveRootPath", "/non/existent/path/archives");

        // The logic:
        // 1. derived = /non/existent/path/license.json (exists? No)
        // 2. fallback = ./data/license.json
        
        // If the project actually has ./data/license.json (which it does based on user context), 
        // the code should return ./data/license.json
        
        Path result = invokeGetLicensePath(service);
        Path standardFallback = Paths.get("./data/license.json");
        
        if (Files.exists(standardFallback)) {
            assertEquals(standardFallback.toAbsolutePath(), result.toAbsolutePath(), "Should fallback to standard ./data/license.json when it exists");
        } else {
             // If standard fallback also doesn't exist, it returns derived path
             Path derived = Paths.get("/non/existent/path/license.json");
             assertEquals(derived.toAbsolutePath(), result.toAbsolutePath());
        }
    }

    private Path invokeGetLicensePath(LicenseService service) {
        return (Path) ReflectionTestUtils.invokeMethod(service, "getLicensePath");
    }
}
