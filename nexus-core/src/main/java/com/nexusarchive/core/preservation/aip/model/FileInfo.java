// Input: File Info
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
public class FileInfo {
    @XmlElement(name = "FileId")
    private String id;
    
    @XmlElement(name = "FileName")
    private String fileName;
    
    @XmlElement(name = "FileType")
    private String fileType;
    
    @XmlElement(name = "OriginalHash")
    private String originalHash;
    
    @XmlElement(name = "HashAlgorithm")
    private String hashAlgorithm;
    
    @XmlElement(name = "Size")
    private Long size;
    
    @XmlElement(name = "StoragePath")
    private String storagePath;
}
