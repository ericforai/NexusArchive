// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: ArchiveBatchMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ArchiveBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArchiveBatchMapper extends BaseMapper<ArchiveBatch> {
    
    /**
     * 获取最后一条有效批次的哈希值
     */
    @Select("SELECT chained_hash FROM arc_archive_batch ORDER BY id DESC LIMIT 1")
    String getLastChainedHash();

    /**
     * 获取最后一条哈希并上锁 (FOR UPDATE)
     * 确保在高并发下，只有一个事务能读取到最新的尾部哈希并追加
     */
    @Select("SELECT chained_hash FROM arc_archive_batch ORDER BY id DESC LIMIT 1 FOR UPDATE")
    String getLastChainedHashForUpdate();

    /**
     * [ADDED P0-4] 获取下一个批次序列号
     * 使用数据库序列保证原子性和唯一性
     */
    @Select("SELECT nextval('arc_batch_seq')")
    Long getNextBatchSequence();

    /**
     * [ADDED P0-4] 根据序列号获取哈希值
     * 用于构建哈希链时获取前一批次的哈希
     */
    @Select("SELECT chained_hash FROM arc_archive_batch WHERE batch_sequence = #{seq}")
    String getChainedHashBySequence(@org.apache.ibatis.annotations.Param("seq") Long seq);
}
