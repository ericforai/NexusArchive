package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexusarchive.entity.BasFonds;

public interface BasFondsService extends IService<BasFonds> {
    
    /**
     * 根据全宗及其它属性初始化档号生成器
     */
    void initSequence(String fondsCode, String year, String category);
}
