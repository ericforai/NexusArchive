// Input: 复杂度历史文件读取
// Output: 质量监控服务接口
// Pos: Service 层 - 质量监控
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.quality.ComplexityHistoryDto;

/**
 * 质量监控服务接口
 * 提供代码复杂度历史数据的读取和快照生成功能
 *
 * @author Agent D (基础设施工程师)
 */
public interface QualityMonitorService {

    /**
     * 获取完整的复杂度历史数据
     *
     * @return 复杂度历史数据
     */
    ComplexityHistoryDto getComplexityHistory();

    /**
     * 生成新的复杂度快照
     * 执行 ESLint 检查并追加新快照到历史文件
     *
     * @return 更新后的历史数据
     */
    ComplexityHistoryDto generateSnapshot();
}
