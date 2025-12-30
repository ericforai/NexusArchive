// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: BasFondsMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.BasFonds;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BasFondsMapper extends BaseMapper<BasFonds> {

    /**
     * 检查该全宗号下是否存在已归档档案
     * @param fondsCode 全宗号
     * @return 存在归档档案返回 true
     */
    @Select("SELECT EXISTS(SELECT 1 FROM acc_archive WHERE fonds_no = #{fondsCode} AND status = 'archived' LIMIT 1)")
    boolean hasArchivedRecords(@Param("fondsCode") String fondsCode);
}

