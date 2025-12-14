package com.nexusarchive.integration.core.connector;

import com.nexusarchive.integration.core.context.SyncContext;
import com.nexusarchive.integration.core.model.FileAttachmentDTO;
import com.nexusarchive.integration.core.model.UnifiedDocumentDTO;

import java.io.InputStream;
import java.util.List;

/**
 * 通用源系统连接器接口
 * 任何接入系统(YonSuite, Kingdee, OA)都需实现此接口
 */
public interface SourceConnector {

    /**
     * 获取连接器类型标识
     * 
     * @return 例如: "YONSUITE", "KINGDEE"
     */
    String getConnectorType();

    /**
     * 获取连接器显示名称
     */
    String getDisplayName();

    /**
     * 批量抓取文档列表
     * 
     * @param context 同步上下文
     * @return 统一文档列表
     */
    List<UnifiedDocumentDTO> fetchDocuments(SyncContext context);

    /**
     * 获取单个文档详情 (部分列表接口可能不返回全量信息)
     * 
     * @param context 上下文
     * @param docId   文档原始ID
     * @return 文档详情
     */
    UnifiedDocumentDTO fetchDocumentDetail(SyncContext context, String docId);

    /**
     * 获取文档关联的附件列表
     * 
     * @param context 上下文
     * @param docId   文档原始ID
     * @return 附件列表
     */
    List<FileAttachmentDTO> fetchAttachments(SyncContext context, String docId);

    /**
     * 下载文件流
     * 
     * @param context 上下文
     * @param url     下载地址或文件标识
     * @return 文件输入流
     */
    InputStream downloadFile(SyncContext context, String url);
}
