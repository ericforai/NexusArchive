package com.nexusarchive.dto.aip;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AIP 包总索引 (index.xml)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "Package")
public class AipIndexXml {

    @JacksonXmlProperty(localName = "File")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<AipIndexFile> files;
}
