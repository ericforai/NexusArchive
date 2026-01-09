// Input: Java Service Layer
// Output: FolderMonitorService (文件夹监控服务接口)
// Pos: Service Layer

package com.nexusarchive.service;

import com.nexusarchive.dto.scan.FolderMonitorRequest;
import com.nexusarchive.dto.scan.FolderMonitorVO;
import com.nexusarchive.entity.ScanFolderMonitor;

import java.util.List;

/**
 * 文件夹监控服务接口
 */
public interface FolderMonitorService {

    /**
     * 获取用户的所有监控配置
     */
    List<ScanFolderMonitor> findByUserId(String userId);

    /**
     * 添加监控配置
     */
    ScanFolderMonitor addMonitor(String userId, FolderMonitorRequest request);

    /**
     * 更新监控配置
     */
    ScanFolderMonitor updateMonitor(Long id, String userId, FolderMonitorRequest request);

    /**
     * 删除监控配置
     */
    void deleteMonitor(Long id, String userId);

    /**
     * 切换监控启用状态
     */
    ScanFolderMonitor toggleMonitor(Long id, String userId);

    /**
     * 转换为VO
     */
    default FolderMonitorVO toVO(ScanFolderMonitor entity) {
        return new FolderMonitorVO(
            entity.getId(),
            entity.getFolderPath(),
            entity.getIsActive(),
            entity.getFileFilter(),
            entity.getAutoDelete(),
            entity.getMoveToPath(),
            entity.getCreatedAt()
        );
    }
}
