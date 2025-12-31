// Input: Audit Log Info
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
public class AuditLogInfo {
    @XmlElement(name = "ActionType")
    private String actionType;
    
    @XmlElement(name = "Operator")
    private String operator;
    
    @XmlElement(name = "CheckTime")
    private String checkTime;
    
    @XmlElement(name = "Result")
    private String result;
    
    @XmlElement(name = "Details")
    private String details;
}
