// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: IngestRequestStatusMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.IngestRequestStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IngestRequestStatusMapper extends BaseMapper<IngestRequestStatus> {

    /**
     * 统计指定全宗的待处理任务数量
     * <p>通过关联 Archive 表进行全宗过滤，统计状态非 COMPLETED/FAILED 的任务</p>
     *
     * @param fondsCodes 全宗代码列表，空列表或 null 时统计所有
     * @return 待处理任务数量
     */
    Long countPendingByFonds(@Param("fondsCodes") List<String> fondsCodes);
}
