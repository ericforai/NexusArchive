// Input: FileContent, physicalPath
// Output: Usability Check Result
// Pos: NexusCore preservation/impl
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.impl;

import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.preservation.CheckResult;
import com.nexusarchive.core.preservation.FourNaturesCheck;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UsabilityCheck implements FourNaturesCheck {

    @Override
    public CheckResult check(FileContent fileContent, Path physicalPath) {
        String fileType = fileContent.getFileType();
        try {
            if ("PDF".equalsIgnoreCase(fileType)) {
                try (PDDocument doc = Loader.loadPDF(physicalPath.toFile())) {
                    // Try to access some content to ensure it's not corrupt
                    int pages = doc.getNumberOfPages();
                    if (pages <= 0) {
                         return CheckResult.fail(getName(), "可用性校验失败: PDF 页数为0", "Empty PDF");
                    }
                    return CheckResult.pass(getName(), "可用性校验通过 (PDF Valid, Pages: " + pages + ")");
                }
            } else if ("OFD".equalsIgnoreCase(fileType)) {
                // Determine if it's a valid ZIP at least (OFD is zip based)
                // Just pass for now as we don't have OFD parser lib yet
                return CheckResult.pass(getName(), "可用性校验通过 (OFD 格式暂仅校验扩展名)");
            } else {
                // Generic check: File size > 0
                if (physicalPath.toFile().length() == 0) {
                    return CheckResult.fail(getName(), "可用性校验失败: 文件为空", "File size is 0");
                }
                return CheckResult.pass(getName(), "可用性校验通过 (Generic check)");
            }
        } catch (Exception e) {
            log.error("Usability check error", e);
            return CheckResult.fail(getName(), "可用性校验失败: 文件损坏或无法解析", e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "USABILITY";
    }
}
