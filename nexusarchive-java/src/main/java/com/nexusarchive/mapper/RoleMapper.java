// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: RoleMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.Role;
import org.apache.ibatis.annotations.*;
import java.util.List;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    
    /**
     * 根据用户ID查询角色列表
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<Role> findByUserId(String userId);
    
    /**
     * 根据角色编码查询角色
     */
    @Select("SELECT * FROM sys_role WHERE code = #{code} AND deleted = 0")
    Role findByCode(String code);
    
    /**
     * 根据角色ID列表查询其类别（role_category）
     */
    @Select("<script>SELECT role_category FROM sys_role WHERE id IN " +
            "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<String> selectCategoriesByIds(@Param("roleIds") List<String> roleIds);
    
    /**
     * 判断给定角色类别是否为排他（is_exclusive）
     */
    @Select("SELECT is_exclusive FROM sys_role WHERE role_category = #{category} LIMIT 1")
    Boolean isCategoryExclusive(@Param("category") String category);
    
    /**
     * 根据用户ID查询其拥有的角色类别列表
     */
    @Select("SELECT r.role_category FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectCategoriesByUserId(@Param("userId") String userId);
}

