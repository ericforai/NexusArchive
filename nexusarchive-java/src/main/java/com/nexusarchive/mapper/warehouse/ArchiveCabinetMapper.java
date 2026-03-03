// Input: ArchiveCabinet 实体
// Output: ArchiveCabinetMapper 接口
// Pos: src/main/java/com/nexusarchive/mapper/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper.warehouse;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.warehouse.ArchiveCabinet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 档案柜 Mapper 接口
 *
 * 提供档案柜的数据库操作方法
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Mapper
public interface ArchiveCabinetMapper extends BaseMapper<ArchiveCabinet> {

    /**
     * 检查柜号是否存在
     *
     * @param code 柜号
     * @param fondsId 全宗ID
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM archives_cabinet WHERE code = #{code} AND fonds_id = #{fondsId} AND (#{excludeId} IS NULL OR id != #{excludeId})")
    boolean existsByCode(@Param("code") String code, @Param("fondsId") String fondsId, @Param("excludeId") Long excludeId);

    /**
     * 获取最大柜号
     *
     * @param fondsId 全宗ID
     * @return 最大柜号
     */
    @Select("SELECT MAX(code) FROM archives_cabinet WHERE fonds_id = #{fondsId}")
    String selectMaxCode(@Param("fondsId") String fondsId);

    /**
     * 统计档案柜数量
     *
     * @param fondsId 全宗ID
     * @return 档案柜总数
     */
    @Select("SELECT COUNT(*) FROM archives_cabinet WHERE fonds_id = #{fondsId}")
    int countByFondsId(@Param("fondsId") String fondsId);

    /**
     * 根据柜号查询档案柜
     *
     * @param code 柜号
     * @return 档案柜实体
     */
    @Select("SELECT * FROM archives_cabinet WHERE code = #{code} LIMIT 1")
    ArchiveCabinet selectByCode(@Param("code") String code);

    /**
     * 根据柜号和全宗ID查询档案柜
     *
     * @param code 柜号
     * @param fondsId 全宗ID
     * @return 档案柜实体
     */
    @Select("SELECT * FROM archives_cabinet WHERE code = #{code} AND fonds_id = #{fondsId} LIMIT 1")
    ArchiveCabinet selectByCodeAndFondsId(@Param("code") String code, @Param("fondsId") String fondsId);
}
