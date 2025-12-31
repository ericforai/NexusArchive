// Input: FileContent entity
// Output: CheckResult
// Pos: NexusCore preservation/interface
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation;

import com.nexusarchive.core.domain.FileContent;
import java.nio.file.Path;

/**
 * 四性检测接口
 * 定义真实性(Authenticity)、完整性(Integrity)、可用性(Usability)、安全性(Security)的检测规范
 */
public interface FourNaturesCheck {
    /**
     * 执行检测
     * @param fileContent 文件元数据对象
     * @param physicalPath 文件实际存储路径 (可能不同于 fileContent 中的路径，例如在归档前的临时文件)
     * @return 检测结果
     */
    CheckResult check(FileContent fileContent, Path physicalPath);

    /**
     * 获取检测项名称
     * @return CHECK_NAME
     */
    String getName();
}
