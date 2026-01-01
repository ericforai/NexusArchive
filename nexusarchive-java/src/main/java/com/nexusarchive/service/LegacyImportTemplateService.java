// Input: Apache POI、文件IO
// Output: LegacyImportTemplateService 模板生成服务
// Pos: 历史数据导入模板服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * 历史数据导入模板生成服务
 * 
 * 功能：
 * 1. 生成CSV格式导入模板
 * 2. 生成Excel格式导入模板
 */
public interface LegacyImportTemplateService {
    
    /**
     * 生成CSV格式导入模板
     * 
     * @return CSV模板文件资源
     */
    Resource generateCsvTemplate();
    
    /**
     * 生成Excel格式导入模板
     * 
     * @return Excel模板文件资源
     */
    Resource generateExcelTemplate();
}

