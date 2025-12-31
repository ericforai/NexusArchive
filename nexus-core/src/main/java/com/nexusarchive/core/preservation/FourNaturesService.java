// Input: FileContent, physicalPath
// Output: List of CheckResult
// Pos: NexusCore preservation/interface
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation;

import com.nexusarchive.core.domain.FileContent;
import java.nio.file.Path;
import java.util.List;

/**
 * 四性检测服务门面
 */
public interface FourNaturesService {
    
    /**
     * 执行所有四性检测
     * @param fileContent 文件元数据
     * @param physicalPath 物理文件路径
     * @return 检测结果列表
     */
    List<CheckResult> validate(FileContent fileContent, Path physicalPath);

    /**
     * 执行特定检测
     * @param fileContent 文件元数据
     * @param physicalPath 物理文件路径
     * @param checkName 检测项名称 (INTEGRITY, AUTHENTICITY, etc.)
     * @return 检测结果
     */
    CheckResult validateSpecific(FileContent fileContent, Path physicalPath, String checkName);
}
