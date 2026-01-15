// Input: Java 标准库、本地模块
// Output: InvoiceParser 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.parser;

import com.nexusarchive.dto.parser.ParsedInvoice;
import java.io.File;

public interface InvoiceParser {
    
    /**
     * 判断是否支持该文件类型
     */
    boolean supports(String fileType);
    
    /**
     * 解析文件
     */
    ParsedInvoice parse(File file);
}
