package com.nexusarchive.service.ofd;

import com.nexusarchive.dto.OfdPreviewResourceResponse;

/**
 * OFD 预览资源决策服务
 */
public interface OfdPreviewResourceService {

    /**
     * 按文件ID解析 OFD 的优先预览资源。
     *
     * @param fileId OFD 文件ID
     * @return 预览资源决策
     */
    OfdPreviewResourceResponse resolve(String fileId);
}
