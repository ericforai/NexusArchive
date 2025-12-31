// Input: Archive Info
// Output: JAXB Class
// Pos: NexusCore preservation/aip/model
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.aip.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ArchiveInfo {
    @XmlElement(name = "ArchiveId")
    private String id;

    @XmlElement(name = "FiscalYear")
    private String fiscalYear;

    @XmlElement(name = "CategoryCode")
    private String categoryCode;

    @XmlElement(name = "Title")
    private String title;
    
    @XmlElement(name = "Amount")
    private String amount;

    @XmlElement(name = "Status")
    private String status;
}
