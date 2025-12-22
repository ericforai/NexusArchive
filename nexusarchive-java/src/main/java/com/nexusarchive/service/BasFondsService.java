// Input: MyBatis-Plus、Java 标准库、本地模块
// Output: BasFondsService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexusarchive.entity.BasFonds;

public interface BasFondsService extends IService<BasFonds> {
    
    /**
     * 根据全宗及其它属性初始化档号生成器
     */
    void initSequence(String fondsCode, String year, String category);
}
