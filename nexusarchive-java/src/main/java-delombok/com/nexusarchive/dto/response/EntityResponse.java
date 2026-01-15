// Input: Lombok、Java 标准库、SysEntity Entity
// Output: EntityResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 法人实体响应 DTO
 * <p>
 * 用于 Controller 返回法人实体信息，避免直接暴露 SysEntity Entity
 * </p>
 */
@Data
public class EntityResponse {

    /**
     * 法人ID
     */
    private String id;

    /**
     * 法人名称
     */
    private String name;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 企业类型
     */
    private String enterpriseType;

    /**
     * 行业分类
     */
    private String industryCategory;

    /**
     * 成立时间
     */
    private LocalDateTime establishedDate;

    /**
     * 法定代表人
     */
    private String legalRepresentative;

    /**
     * 注册地址
     */
    private String registeredAddress;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 状态: ACTIVE, INACTIVE, SUSPENDED
     */
    private String status;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    // Manual Setters to fix compilation issues if Lombok fails
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCreditCode(String creditCode) { this.creditCode = creditCode; }
    public void setEnterpriseType(String enterpriseType) { this.enterpriseType = enterpriseType; }
    public void setIndustryCategory(String industryCategory) { this.industryCategory = industryCategory; }
    public void setEstablishedDate(LocalDateTime establishedDate) { this.establishedDate = establishedDate; }
    public void setLegalRepresentative(String legalRepresentative) { this.legalRepresentative = legalRepresentative; }
    public void setRegisteredAddress(String registeredAddress) { this.registeredAddress = registeredAddress; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public void setStatus(String status) { this.status = status; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
