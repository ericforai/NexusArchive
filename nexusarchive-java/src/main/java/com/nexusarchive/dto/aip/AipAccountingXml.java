package com.nexusarchive.dto.aip;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结构化会计数据 (accounting.xml)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "AccountingData")
public class AipAccountingXml {

    @JacksonXmlProperty(localName = "FondsCode")
    private String fondsCode;

    @JacksonXmlProperty(localName = "ArchivalCode")
    private String archivalCode;

    @JacksonXmlProperty(localName = "FiscalYear")
    private String fiscalYear;

    @JacksonXmlProperty(localName = "FiscalPeriod")
    private String fiscalPeriod;

    @JacksonXmlProperty(localName = "RetentionPeriod")
    private String retentionPeriod;

    @JacksonXmlProperty(localName = "CategoryCode")
    private String categoryCode;

    @JacksonXmlProperty(localName = "Title")
    private String title;

    @JacksonXmlProperty(localName = "Creator")
    private String creator;

    @JacksonXmlProperty(localName = "OrgName")
    private String orgName;
    
    @JacksonXmlProperty(localName = "SecurityLevel")
    private String securityLevel;
}
