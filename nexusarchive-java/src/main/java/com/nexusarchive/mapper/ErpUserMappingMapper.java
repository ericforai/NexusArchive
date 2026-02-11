// Input: MyBatis-Plus BaseMapper
// Output: ErpUserMappingMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ErpUserMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ErpUserMappingMapper extends BaseMapper<ErpUserMapping> {

    @Select("SELECT * FROM erp_user_mapping WHERE client_id = #{clientId} AND erp_user_job_no = #{erpUserJobNo} AND status = 'ACTIVE' LIMIT 1")
    ErpUserMapping findActive(String clientId, String erpUserJobNo);
}
