// Input: Lombok、Java 标准库
// Output: FondsScopeResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 用户全宗权限响应
 * 用于返回用户已分配的全宗列表和可分配的全宗列表
 */
@Data
public class FondsScopeResponse {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 已分配的全宗号列表
     */
    private List<String> assignedFonds;

    /**
     * 可用的全宗列表
     */
    private List<FondsInfo> availableFonds;

    /**
     * 全宗信息
     */
    @Data
    public static class FondsInfo {
        /**
         * 全宗号
         */
        private String fondsCode;

        /**
         * 全宗名称
         */
        private String fondsName;

        /**
         * 立档单位名称
         */
        private String companyName;
    }
}
