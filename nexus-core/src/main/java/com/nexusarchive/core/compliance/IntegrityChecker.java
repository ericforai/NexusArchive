// Input: XML 与 OFD/PDF 文件
// Output: 元数据一致性校验结果
// Pos: NexusCore compliance/integrity
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.nio.file.Path;

/**
 * 完整性检测器接口
 * 
 * PRD 来源: PRD 3.1 - 必须校验 XML 元数据与版式文件(OFD/PDF)内容的一致性
 */
public interface IntegrityChecker {
    /**
     * 校验 XML 元数据与版式文件一致性
     * 
     * @param xmlPath XML 文件路径
     * @param formatPath OFD/PDF 文件路径
     * @return 校验结果
     */
    IntegrityCheckResult verify(Path xmlPath, Path formatPath);
}
