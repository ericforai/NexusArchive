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
 * EEP (Electronic Encapsulated Package) XML 结构
 * Reference: DA/T 94-2022 封装包结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "eep_package")
public class EepXmlStructure {

    @JacksonXmlProperty(localName = "header")
    private Header header;

    @JacksonXmlProperty(localName = "entity_data")
    private EntityData entityData;

    @JacksonXmlProperty(localName = "digital_objects")
    private DigitalObjects digitalObjects;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        @JacksonXmlProperty(localName = "version")
        private String version;

        @JacksonXmlProperty(localName = "created_time")
        private String createdTime;

        @JacksonXmlProperty(localName = "archival_code")
        private String archivalCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityData {
        @JacksonXmlProperty(localName = "fonds_code")
        private String fondsCode;

        @JacksonXmlProperty(localName = "account_period")
        private String accountPeriod;

        @JacksonXmlProperty(localName = "voucher_type")
        private String voucherType;

        @JacksonXmlProperty(localName = "voucher_number")
        private String voucherNumber;

        @JacksonXmlProperty(localName = "voucher_date")
        private String voucherDate;

        @JacksonXmlProperty(localName = "total_amount")
        private String totalAmount;

        @JacksonXmlProperty(localName = "currency_code")
        private String currencyCode;
        
        @JacksonXmlProperty(localName = "issuer")
        private String issuer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DigitalObjects {
        @JacksonXmlProperty(localName = "object")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<DigitalObject> objects;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DigitalObject {
        @JacksonXmlProperty(localName = "filename")
        private String filename;

        @JacksonXmlProperty(localName = "format")
        private String format;

        @JacksonXmlProperty(localName = "hash_algorithm")
        private String hashAlgorithm;

        @JacksonXmlProperty(localName = "hash_value")
        private String hashValue;
        
        @JacksonXmlProperty(localName = "size_bytes")
        private Long sizeBytes;
    }
}
