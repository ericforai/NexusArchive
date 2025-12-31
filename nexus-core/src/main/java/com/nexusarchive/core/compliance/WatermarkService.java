// Input: PDF 文件 + 水印配置
// Output: 带水印的 PDF 流
// Pos: NexusCore compliance/watermark
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.InputStream;

/**
 * 水印服务接口
 * 
 * PRD 来源: PRD 2.2 - 服务端流式加水印
 */
public interface WatermarkService {
    /**
     * 为 PDF 添加水印
     * 
     * @param source 原始 PDF 输入流
     * @param config 水印配置
     * @return 带水印的 PDF 输出流
     */
    InputStream addWatermark(InputStream source, WatermarkConfig config);
}
