// Input: MyBatis-Plus BaseMapper
// Output: FondsHistoryMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.FondsHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 全宗沿革 Mapper
 */
@Mapper
public interface FondsHistoryMapper extends BaseMapper<FondsHistory> {
    
    /**
     * 查询指定全宗的沿革历史
     */
    @Select("SELECT * FROM fonds_history WHERE fonds_no = #{fondsNo} AND deleted = 0 ORDER BY effective_date DESC, created_at DESC")
    List<FondsHistory> findByFondsNo(String fondsNo);
    
    /**
     * 查询指定事件类型的沿革记录
     */
    @Select("SELECT * FROM fonds_history WHERE event_type = #{eventType} AND deleted = 0 ORDER BY effective_date DESC")
    List<FondsHistory> findByEventType(String eventType);
}

