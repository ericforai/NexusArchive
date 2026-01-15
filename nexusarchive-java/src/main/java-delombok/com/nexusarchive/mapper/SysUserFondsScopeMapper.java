// Input: MyBatis-Plus、Java 标准库
// Output: SysUserFondsScopeMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.SysUserFondsScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserFondsScopeMapper extends BaseMapper<SysUserFondsScope> {

    @Select("SELECT fonds_no FROM sys_user_fonds_scope WHERE user_id = #{userId} AND deleted = 0 ORDER BY fonds_no")
    List<String> findFondsNoByUserId(@Param("userId") String userId);

    /**
     * 逻辑删除用户的所有全宗权限
     * 使用 MyBatis-Plus 的逻辑删除机制
     * @param userId 用户ID
     * @return 删除的记录数
     */
    @org.apache.ibatis.annotations.Update("UPDATE sys_user_fonds_scope SET deleted = 1 WHERE user_id = #{userId} AND deleted = 0")
    int deleteByUserId(@Param("userId") String userId);
}
