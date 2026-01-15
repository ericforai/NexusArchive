package com.nexusarchive.service.borrow;

// Input: Borrow Commands
// Output: BorrowRequest Entity / Domain Events
// Pos: service/borrow
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import com.nexusarchive.domain.borrow.ApproveBorrowRequestCommand;
import com.nexusarchive.domain.borrow.SubmitBorrowRequestCommand;
import com.nexusarchive.domain.borrow.BorrowRequestVO;
import com.nexusarchive.entity.BorrowRequest;

/**
 * 借阅申请核心业务服务
 * 
 * 遵循深奥的简洁：接口即契约，方法必须原子化。
 */
public interface BorrowRequestService {

    /**
     * 提交借阅申请
     * @param command 经过校验的申请指令
     * @return 已持久化的借阅申请实体
     */
    BorrowRequest submit(SubmitBorrowRequestCommand command);

    /**
     * 审批借阅申请
     * @param command 经过校验的审批指令
     */
    void approve(ApproveBorrowRequestCommand command);

    /**
     * 确认档案借出 (转为 BORROWING 状态)
     * @param requestId 申请ID
     */
    void confirmOut(String requestId);

    /**
     * 归还档案 (转为 RETURNED 状态)
     * @param requestId 申请ID
     * @param operatorId 操作人ID
     */
    /**
     * 分页查询借阅申请
     * @param page 分页对象
     * @param status 状态过滤
     * @param keyword 关键词
     * @return 分页展示对象
     */
    com.baomidou.mybatisplus.core.metadata.IPage<BorrowRequestVO> list(com.baomidou.mybatisplus.extension.plugins.pagination.Page<BorrowRequest> page, String status, String keyword);

    void returnArchives(String requestId, String operatorId);
}
