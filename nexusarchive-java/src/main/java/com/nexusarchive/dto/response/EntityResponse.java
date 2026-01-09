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
}
