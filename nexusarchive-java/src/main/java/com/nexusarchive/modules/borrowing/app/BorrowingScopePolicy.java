// Input: MyBatis-Plus、DataScopeContext
// Output: BorrowingScopePolicy 接口
// Pos: borrowing/app
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.app;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.modules.borrowing.domain.Borrowing;
import com.nexusarchive.service.DataScopeService.DataScopeContext;

public interface BorrowingScopePolicy {
    void apply(QueryWrapper<Borrowing> wrapper, DataScopeContext context);
}
