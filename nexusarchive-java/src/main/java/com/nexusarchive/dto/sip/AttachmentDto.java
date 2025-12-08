package com.nexusarchive.dto.sip;

import com.nexusarchive.common.enums.ArchiveFileType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

/**
 * 附件信息 DTO
 * Reference: DA/T 104-2024 附件元数据规范
 */
public class AttachmentDto {
    
    @NotBlank(message = "文件名不能为空")
    private String fileName;
    
    @NotBlank(message = "文件类型不能为空")
    private String fileType;
    
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;
    
    @NotBlank(message = "文件内容不能为空")
    private String base64Content;
    
    private String fileHash;
    
    private String hashAlgorithm;

    public AttachmentDto() {}

    public AttachmentDto(String fileName, String fileType, Long fileSize, String base64Content, String fileHash, String hashAlgorithm) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.base64Content = base64Content;
        this.fileHash = fileHash;
        this.hashAlgorithm = hashAlgorithm;
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getBase64Content() { return base64Content; }
    public void setBase64Content(String base64Content) { this.base64Content = base64Content; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getHashAlgorithm() { return hashAlgorithm; }
    public void setHashAlgorithm(String hashAlgorithm) { this.hashAlgorithm = hashAlgorithm; }
    
    /**
     * 验证文件类型是否合法
     */
    public void validateFileType() {
        if (!ArchiveFileType.isValid(this.fileType)) {
            throw new IllegalArgumentException(
                String.format("文件类型 '%s' 不支持，允许的格式：OFD, PDF, XML, JPG, PNG", this.fileType)
            );
        }
    }

    public static AttachmentDtoBuilder builder() {
        return new AttachmentDtoBuilder();
    }

    public static class AttachmentDtoBuilder {
        private AttachmentDto dto = new AttachmentDto();

        public AttachmentDtoBuilder fileName(String fileName) { dto.setFileName(fileName); return this; }
        public AttachmentDtoBuilder fileType(String fileType) { dto.setFileType(fileType); return this; }
        public AttachmentDtoBuilder fileSize(Long fileSize) { dto.setFileSize(fileSize); return this; }
        public AttachmentDtoBuilder base64Content(String base64Content) { dto.setBase64Content(base64Content); return this; }
        public AttachmentDtoBuilder fileHash(String fileHash) { dto.setFileHash(fileHash); return this; }
        public AttachmentDtoBuilder hashAlgorithm(String hashAlgorithm) { dto.setHashAlgorithm(hashAlgorithm); return this; }
        
        public AttachmentDto build() { return dto; }
    }
}
