// Input: MyBatis-Plus、Spring、Java 标准库、本地模块
// Output: BorrowingScopePolicyImpl 类
// Pos: borrowing/infra
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.infra;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.modules.borrowing.app.BorrowingScopePolicy;
import com.nexusarchive.modules.borrowing.domain.Borrowing;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BorrowingScopePolicyImpl implements BorrowingScopePolicy {


    @Override
    public void apply(QueryWrapper<Borrowing> wrapper, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return;
        }

        if (context.isSelf()) {
            if (context.userId() != null) {
                wrapper.eq("user_id", context.userId());
            } else {
                wrapper.eq("1", "0");
            }
            return;
        }

        // 基于 fonds_no（全宗号）进行数据隔离
        Set<String> allowedFonds = context.allowedFonds();
        if (!allowedFonds.isEmpty()) {
            // 根据允许的全宗号列表过滤借阅记录
            wrapper.in("fonds_no", allowedFonds);
            return;
        }

        // 如果没有允许访问的全宗，则不允许访问任何数据
        wrapper.eq("1", "0");
    }

    public void apply(LambdaQueryWrapper<Borrowing> wrapper, DataScopeContext context) {
        if (context == null || context.isAll()) {
            return;
        }

        if (context.isSelf()) {
            if (context.userId() != null) {
                wrapper.eq(Borrowing::getUserId, context.userId());
            } else {
                wrapper.apply("1 = 0");
            }
            return;
        }

        Set<String> deptIds = context.departmentIds();
        if (!deptIds.isEmpty()) {
            List<String> archiveIds = archiveService.getArchiveIdsByDepartmentIds(deptIds);
            if (archiveIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(Borrowing::getArchiveId, archiveIds);
            }
            return;
        }

        if (StringUtils.hasText(context.departmentId())) {
            List<String> archiveIds = archiveService.getArchiveIdsByDepartmentIds(Collections.singleton(context.departmentId()));
            if (archiveIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(Borrowing::getArchiveId, archiveIds);
            }
            return;
        }

        if (context.userId() != null) {
            wrapper.eq(Borrowing::getUserId, context.userId());
        } else {
            wrapper.apply("1 = 0");
        }
    }
}
