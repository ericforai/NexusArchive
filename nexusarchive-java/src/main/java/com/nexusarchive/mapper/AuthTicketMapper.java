// Input: MyBatis-Plus BaseMapper
// Output: AuthTicketMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.AuthTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权票据 Mapper
 */
@Mapper
public interface AuthTicketMapper extends BaseMapper<AuthTicket> {
    
    /**
     * 查询指定申请人的授权票据列表
     */
    @Select("SELECT * FROM auth_ticket WHERE applicant_id = #{applicantId} AND deleted = 0 ORDER BY created_at DESC")
    List<AuthTicket> findByApplicantId(String applicantId);
    
    /**
     * 查询过期的授权票据（用于定时任务）
     */
    @Select("SELECT * FROM auth_ticket WHERE status IN ('PENDING', 'FIRST_APPROVED', 'APPROVED') " +
            "AND expires_at < #{now} AND deleted = 0")
    List<AuthTicket> findExpiredTickets(LocalDateTime now);
}





