package com.nexusarchive.service;

/**
 * 自动关联服务接口
 */
public interface IAutoAssociationService {

    /**
     * 夜间定时任务
     */
    void runNightlyJob();

    /**
     * 触发单个凭证的自动关联
     * @param voucherId 凭证ID
     */
    void triggerAssociation(String voucherId);

    /**
     * 获取凭证关联的文件列表
     * @param voucherId 凭证ID
     * @return 关联文件列表
     */
    java.util.List<com.nexusarchive.dto.relation.LinkedFileDto> getLinkedFiles(String voucherId);
}
