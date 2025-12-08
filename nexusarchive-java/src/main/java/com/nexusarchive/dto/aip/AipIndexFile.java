package com.nexusarchive.dto.aip;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIP 索引文件条目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AipIndexFile {

    /**
     * 文件类型: Main (主件), Attachment (附件)
     */
    @JacksonXmlProperty(isAttribute = true)
    private String type;

    /**
     * 相对路径文件名
     */
    @JacksonXmlProperty(isAttribute = true)
    private String filename;

    /**
     * 关联关系: Support (从属), etc.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String relation;
}
