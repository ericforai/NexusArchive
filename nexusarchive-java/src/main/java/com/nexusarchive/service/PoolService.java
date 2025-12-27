// Input: Spring Framework, JDK
// Output: PoolService 接口
// Pos: 业务逻辑接口层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.PoolItemDto;
import com.nexusarchive.dto.search.CandidateSearchRequest;
import java.util.List;

/**
 * 电子凭证池服务接口
 */
public interface PoolService {

    /**
     * 搜索可关联的候选凭证
     * 
     * @param request 搜索条件
     * @return 候选凭证列表
     */
    List<PoolItemDto> searchCandidates(CandidateSearchRequest request);
}
