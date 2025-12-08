package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    User findByUsername(String username);
    
    /**
     * 根据工号查询用户
     */
    @Select("SELECT * FROM sys_user WHERE employee_id = #{employeeId} AND deleted = 0")
    User findByEmployeeId(String employeeId);
    
    /**
     * 插入用户角色关联（批量）
     */
    @Insert("<script>INSERT INTO sys_user_role (user_id, role_id) VALUES " +
            "<foreach collection='roleIds' item='roleId' separator=','> (#{userId}, #{roleId}) </foreach></script>")
    void insertUserRoles(@Param("userId") String userId, @Param("roleIds") List<String> roleIds);
    
    /**
     * 删除用户的所有角色关联
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteUserRoles(@Param("userId") String userId);
    
    /**
     * 查询所有未删除的用户
     */
    @Select("SELECT * FROM sys_user WHERE deleted = 0")
    List<User> selectAll();

    /**
     * 查询有效用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 'active'")
    int countActiveUsers();
    
    /**
     * 根据用户ID查询其角色ID列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<String> selectRoleIdsByUserId(@Param("userId") String userId);
}
