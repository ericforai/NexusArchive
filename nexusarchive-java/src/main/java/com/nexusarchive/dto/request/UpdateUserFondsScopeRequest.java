// Input: Lombok、Java 标准库
// Output: UpdateUserFondsScopeRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 更新用户全宗权限请求
 * 用于设置用户可以访问的全宗列表
 */
@Data
public class UpdateUserFondsScopeRequest {
    /**
     * 全宗号列表
     * 空列表表示用户没有任何全宗访问权限
     */
    private List<String> fondsCodes;
}
