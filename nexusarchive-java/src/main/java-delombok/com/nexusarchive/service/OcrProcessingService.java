// Input: Java 标准库
// Output: OcrProcessingService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.OcrResult;

/**
 * OCR 处理服务接口
 *
 * 提供同步和异步两种 OCR 识别方式
 */
public interface OcrProcessingService {

    /**
     * 异步处理 OCR 识别
     *
     * @param id 工作区记录 ID
     * @param filePath 文件路径
     */
    void processAsync(Long id, String filePath);

    /**
     * 同步处理 OCR 识别
     *
     * @param filePath 文件路径
     * @param engine OCR 引擎类型
     * @return OCR 识别结果
     */
    OcrResult processSync(String filePath, String engine);
}
