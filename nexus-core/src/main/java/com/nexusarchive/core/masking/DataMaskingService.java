// Input: 脱敏服务接口
// Output: 脱敏后的数据
// Pos: NexusCore masking
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.masking;

import java.util.List;

/**
 * 数据脱敏服务
 */
public interface DataMaskingService {
    
    /**
     * 对单个值应用脱敏
     * @param fieldName 字段名 (用于匹配规则)
     * @param value 原始值
     * @return 脱敏后的值
     */
    String mask(String fieldName, String value);
    
    /**
     * 对对象进行脱敏 (原地修改或返回副本，视实现而定)
     * @param object 待脱敏对象
     * @return 脱敏后的对象
     */
    <T> T maskObject(T object);
    
    /**
     * 批量脱敏
     */
    <T> List<T> maskList(List<T> list);
}
