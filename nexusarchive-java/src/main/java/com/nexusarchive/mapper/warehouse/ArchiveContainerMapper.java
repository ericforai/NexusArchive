// Input: ArchiveContainer 实体类
// Output: ArchiveContainer Mapper 接口
// Pos: src/main/java/com/nexusarchive/mapper/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveContainer;
import com.nexusarchive.dto.warehouse.ContainerDTO;
import com.nexusarchive.dto.warehouse.ContainerDetailVO;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 档案袋 Mapper 接口
 *
 * 提供档案袋的数据库操作方法
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Mapper
public interface ArchiveContainerMapper {

    /**
     * 插入档案袋
     *
     * @param entity 档案袋实体
     * @return 影响行数
     */
    int insert(ArchiveContainer entity);

    /**
     * 更新档案袋
     *
     * @param entity 档案袋实体
     * @return 影响行数
     */
    int update(ArchiveContainer entity);

    /**
     * 根据 ID 查询档案袋
     *
     * @param id 档案袋ID
     * @return 档案袋实体
     */
    ArchiveContainer selectById(Long id);

    /**
     * 根据袋号查询档案袋
     *
     * @param containerNo 袋号
     * @return 档案袋实体
     */
    ArchiveContainer selectByContainerNo(@Param("containerNo") String containerNo);

    /**
     * 条件查询档案袋列表
     *
     * @param cabinetId 档案柜ID（可选）
     * @param status 状态（可选）
     * @param keyword 关键字搜索（袋号）
     * @param fondsId 全宗ID
     * @return 档案袋列表
     */
    List<ArchiveContainer> selectList(
        @Param("cabinetId") Long cabinetId,
        @Param("status") String status,
        @Param("keyword") String keyword,
        @Param("fondsId") String fondsId
    );

    /**
     * 根据档案柜ID统计数量
     *
     * @param cabinetId 档案柜ID
     * @return 档案袋数量
     */
    int countByCabinetId(@Param("cabinetId") Long cabinetId);

    /**
     * 检查袋号是否存在
     *
     * @param containerNo 袋号
     * @param fondsId 全宗ID
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean existsByContainerNo(
        @Param("containerNo") String containerNo,
        @Param("fondsId") String fondsId,
        @Param("excludeId") Long excludeId
    );

    /**
     * 更新档案袋状态
     *
     * @param id 档案袋ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 批量更新柜号
     *
     * @param cabinetId 档案柜ID
     * @param oldCode 旧柜号
     * @param newCode 新柜号
     * @return 影响行数
     */
    int batchUpdateCabinetCode(
        @Param("cabinetId") Long cabinetId,
        @Param("oldCode") String oldCode,
        @Param("newCode") String newCode
    );

    /**
     * 获取下一个袋号
     *
     * @param fondsId 全宗ID
     * @return 下一个袋号
     */
    String getNextContainerNo(@Param("fondsId") String fondsId);

    /**
     * 获取最大袋号
     *
     * @param fondsId 全宗ID
     * @return 最大袋号
     */
    String selectMaxNo(@Param("fondsId") String fondsId);

    /**
     * 统计各状态档案袋数量
     *
     * @param cabinetId 档案柜ID
     * @return 统计结果 Map<status, count>
     */
    List<Map<String, Object>> countByStatus(@Param("cabinetId") Long cabinetId);
}
