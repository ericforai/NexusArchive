// Input: Lombok、Java 标准库
// Output: ArchiveRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;
import java.util.List;

/**
 * 归档请求 DTO
 */
@Data
public class ArchiveRequest {
    /**
     * 凭证池记录 ID 列表
     */
    private List<String> poolItemIds;
}
