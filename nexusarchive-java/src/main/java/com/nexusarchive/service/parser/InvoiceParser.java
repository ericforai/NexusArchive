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
