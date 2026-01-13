package com.nexusarchive.mapper;

// Input: SQL Queries / Entity
// Output: Data persistence
// Pos: mapper
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.BorrowRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 借阅申请 Mapper 接口
 */
@Mapper
public interface BorrowRequestMapper extends BaseMapper<BorrowRequest> {
}
