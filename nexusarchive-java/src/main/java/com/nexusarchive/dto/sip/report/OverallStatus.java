// Input: Java 标准库
// Output: OverallStatus 枚举
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.sip.report;

/**
 * 四性检测总体状态
 */
public enum OverallStatus {
    /**
     * 检测通过
     */
    PASS,
    
    /**
     * 检测失败 (阻断性问题)
     */
    FAIL,
    
    /**
     * 警告 (非阻断性问题)
     */
    WARNING
}
