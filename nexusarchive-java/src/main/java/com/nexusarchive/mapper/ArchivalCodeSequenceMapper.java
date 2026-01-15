// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: ArchivalCodeSequenceMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ArchivalCodeSequence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArchivalCodeSequenceMapper extends BaseMapper<ArchivalCodeSequence> {

    /**
     * 获取下一个序列值 (悲观锁)
     * @return 当前值
     */
    @Select("SELECT current_val FROM sys_archival_code_sequence " +
            "WHERE fonds_code = #{fondsCode} AND fiscal_year = #{year} AND category_code = #{category} " +
            "FOR UPDATE")
    Integer selectCurrentValForUpdate(@Param("fondsCode") String fondsCode, 
                                     @Param("year") String year, 
                                     @Param("category") String category);

    /**
     * 增加序列值
     */
    @Update("UPDATE sys_archival_code_sequence SET current_val = current_val + 1, updated_time = NOW() " +
            "WHERE fonds_code = #{fondsCode} AND fiscal_year = #{year} AND category_code = #{category}")
    int incrementVal(@Param("fondsCode") String fondsCode,
                     @Param("year") String year,
                     @Param("category") String category);
                     
    /**
     * 初始化序列 (仅当不存在时)
     */
    @Update("INSERT INTO sys_archival_code_sequence (fonds_code, fiscal_year, category_code, current_val) " +
            "VALUES (#{fondsCode}, #{year}, #{category}, 0) " +
            "ON CONFLICT (fonds_code, fiscal_year, category_code) DO NOTHING")
    void initSequence(@Param("fondsCode") String fondsCode, 
                      @Param("year") String year, 
                      @Param("category") String category);
}
