package com.nexusarchive.verification;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.service.DigitalSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@ComponentScan(
    basePackages = "com.nexusarchive",
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.nexusarchive.controller.*")
)
public class VerifySignatureFix implements CommandLineRunner {

    @Autowired
    private DigitalSignatureService signatureService;

    public static void main(String[] args) {
        System.setProperty("archive.root.path", "./data/archives");
        SpringApplication.run(VerifySignatureFix.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Starting Verification of DigitalSignatureService Fix ===");

        // 1. Create a dummy file in the new storage structure
        File dir = new File("./data/archives/test_fonds/2024");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File testFile = new File(dir, "test_doc.txt");
        Files.write(testFile.toPath(), "Content for verification".getBytes());
        System.out.println("✅ Helper: Created test file at " + testFile.getAbsolutePath());

        // 2. Mock an ArcFileContent entity
        ArcFileContent mockFile = new ArcFileContent();
        mockFile.setFileName("test_doc.txt");
        // Relative path logic check
        mockFile.setStoragePath("test_fonds/2024/test_doc.txt");
        // We expect signature verification to FAIL on signature format, but PASS the file reading part.
        // If file reading fails, it throws an Exception, not returning a result object.
        mockFile.setSignValue("dummy_signature".getBytes());
        mockFile.setCertificate(java.util.Base64.getEncoder().encodeToString("dummy_cert".getBytes()));

        try {
            System.out.println("🚀 Triggering verifySignature...");
            DigitalSignatureService.VerificationResult result = signatureService.verifySignature(mockFile);
            
            System.out.println("Current Result Message: " + result.getErrorMessage());
            
            // Check if failure is due to crypto (GOOD) or file not found (BAD)
            if ("验证失败".equals(result.getErrorMessage()) || result.getErrorMessage().contains("不支持的签名算法") || result.getErrorMessage().contains("Could not parse certificate")) {
                 System.out.println("✅ SUCCESS: File was read successfully! (Crypto failure is expected for dummy data)");
            } else {
                 System.out.println("⚠️ UNEXPECTED RESULT: " + result.getErrorMessage());
            }

        } catch (Exception e) {
             System.out.println("❌ CRITICAL FAILURE: Exception thrown during verification process.");
             e.printStackTrace();
             System.exit(1);
        }
        
        System.out.println("=== Verification Complete ===");
        System.exit(0);
    }
}
