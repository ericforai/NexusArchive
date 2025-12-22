// Input: Java 标准库、本地模块
// Output: GlobalSearchService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.GlobalSearchDTO;
import java.util.List;

public interface GlobalSearchService {
    /**
     * Perform a global search across Archives and Metadata.
     *
     * @param query The search query string.
     * @return List of matching results.
     */
    List<GlobalSearchDTO> search(String query);
}
