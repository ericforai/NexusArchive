// Input: 检索服务接口
// Output: 档案搜索结果
// Pos: NexusCore search
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.search;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.core.domain.ArchiveObject;

/**
 * 档案高级检索服务
 */
public interface ArchiveSearchService {
    
    /**
     * 执行高级检索
     * 
     * @param request 检索条件
     * @param pageable 分页参数
     * @return 分页结果
     */
    IPage<ArchiveObject> search(ArchiveSearchRequest request, Page<ArchiveObject> pageable);
}
