// Input: Java 标准库、本地模块
// Output: IngestService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.IngestResponse;

/**
 * SIP 接收服务接口
 */
public interface IngestService {
    
    /**
     * 接收并处理会计凭证 SIP 包
     * 
     * @param sipDto SIP 数据包
     * @return 处理结果响应
     */
    IngestResponse ingestSip(AccountingSipDto sipDto);

    /**
     * 处理文件上传
     * @param file 上传的文件
     * @return 上传结果详情
     */
    com.nexusarchive.dto.FileUploadResponse handleFileUpload(org.springframework.web.multipart.MultipartFile file);
    
    /**
     * 正式归档
     * 将凭证池中的记录转换为正式的 AIP 档案包
     * 
     * @param poolItemIds 凭证池记录 ID 列表
     * @throws java.io.IOException 文件操作异常
     */
    void archivePoolItems(java.util.List<String> poolItemIds, String userId) throws java.io.IOException;
}
