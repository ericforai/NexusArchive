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

    /**
     * 检查全宗号是否可以修改
     * @param fondsCode 全宗号
     * @return true = 可修改，false = 存在归档档案不可修改
     */
    boolean canModifyFondsCode(String fondsCode);

    /**
     * 更新全宗信息（带全宗号不可变约束）
     * @param fonds 全宗实体
     * @return 更新成功返回 true
     * @throws com.nexusarchive.common.exception.BusinessException 当全宗号被修改且存在归档档案时抛出
     */
    boolean updateFonds(BasFonds fonds);
}

