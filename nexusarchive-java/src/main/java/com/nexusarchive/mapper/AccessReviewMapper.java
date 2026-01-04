// Input: MyBatis-Plus BaseMapper
// Output: AccessReviewMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.AccessReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访问权限复核 Mapper
 */
@Mapper
public interface AccessReviewMapper extends BaseMapper<AccessReview> {
}



