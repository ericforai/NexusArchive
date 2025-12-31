// Input: AIP Metadata Structure
// Output: JAXB Annotated Class
// Pos: NexusCore preservation/aip/model
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.aip.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "AipMetadata")
@XmlAccessorType(XmlAccessType.FIELD)
public class AipMetadata {

    @XmlElement(name = "FondsInfo")
    private FondsInfo fondsInfo;

    @XmlElement(name = "ArchiveInfo")
    private ArchiveInfo archiveInfo;

    @XmlElementWrapper(name = "FileList")
    @XmlElement(name = "File")
    private List<FileInfo> files;

    @XmlElementWrapper(name = "AuditLogs")
    @XmlElement(name = "LogEntry")
    private List<AuditLogInfo> auditLogs;
}
