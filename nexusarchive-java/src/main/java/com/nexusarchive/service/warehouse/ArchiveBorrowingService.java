// Input: ArchiveBorrowingMapper 接口
// Output: ArchiveBorrowingService 业务逻辑层
// Pos: src/main/java/com/nexusarchive/service/warehouse
// 一旦我被更新，开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.warehouse;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusarchive.entity.warehouse.ArchiveBorrowing;
import com.nexusarchive.mapper.warehouse.ArchiveBorrowingMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实物借阅 Service 层
 *
 * 业务逻辑：
 * 1. 借阅单号生成：BW-{YYYY}-{3位流水号}
 * 2. 借阅审批流程
 * 3. 归还确认
 * 4. 逾期检查和提醒
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ArchiveBorrowingService extends ServiceImpl<ArchiveBorrowingMapper, ArchiveBorrowing> {

    private final ArchiveBorrowingMapper borrowingMapper;

    /**
     * 分页查询借阅记录列表
     *
     * @param containerId 档案袋ID（可选）
     * @param status 状态（可选）
     * @param borrower 借阅人（可选）
     * @param fondsId 全宗ID
     * @return 借阅记录列表
     */
    public List<ArchiveBorrowing> list(Long containerId, String status, String borrower, Long fondsId) {
        return borrowingMapper.selectList(containerId, status, borrower, fondsId);
    }

    /**
     * 创建借阅申请
     *
     * @param entity 借阅实体
     * @return 创建的借阅记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ArchiveBorrowing create(ArchiveBorrowing entity) {
        // 生成借阅单号
        String borrowNo = generateBorrowNo(entity.getFondsId());
        entity.setBorrowNo(borrowNo);
        entity.setStatus("borrowed");
        entity.setBorrowDate(LocalDate.now());

        borrowingMapper.insert(entity);
        return entity;
    }

    /**
     * 审批借阅申请
     *
     * @param id 借阅ID
     * @param approvedBy 审批人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long approvedBy) {
        ArchiveBorrowing borrowing = borrowingMapper.selectById(id);
        if (borrowing == null) {
            throw new RuntimeException("借阅记录不存在: " + id);
        }
        if (!"borrowed".equals(borrowing.getStatus())) {
            throw new RuntimeException("只有已借出的记录才能审批");
        }

        // 更新审批信息
        ArchiveBorrowing updateEntity = new ArchiveBorrowing();
        updateEntity.setId(id);
        updateEntity.setApprovedBy(approvedBy);
        updateEntity.setApprovedAt(LocalDateTime.now());
        borrowingMapper.update(updateEntity);
    }

    /**
     * 确认归还
     *
     * @param id 借阅ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(Long id) {
        ArchiveBorrowing borrowing = borrowingMapper.selectById(id);
        if (borrowing == null) {
            throw new RuntimeException("借阅记录不存在: " + id);
        }
        if ("returned".equals(borrowing.getStatus())) {
            throw new RuntimeException("该借阅已归还");
        }

        // 确认归还
        borrowingMapper.confirmReturn(id, LocalDate.now());
        borrowingMapper.updateStatus(id, "returned");

        // 同时更新档案袋状态
        // TODO: 调用 ContainerService 更新状态为 available
    }

    /**
     * 生成借阅单号
     * 规则：BW-{YYYY}-{3位流水号}
     */
    private String generateBorrowNo(Long fondsId) {
        String year = LocalDate.now().toString().substring(0, 4);
        String maxNo = borrowingMapper.getNextBorrowNo(fondsId);

        int nextNum = 1;
        if (maxNo != null && maxNo.startsWith("BW-" + year)) {
            try {
                String numStr = maxNo.substring(8); // BW-YYYY- 后的数字
                nextNum = Integer.parseInt(numStr) + 1;
            } catch (NumberFormatException e) {
                nextNum = 1;
            }
        }

        return String.format("BW-%s-%03d", year, nextNum);
    }

    /**
     * 统计逾期借阅数量
     *
     * @param fondsId 全宗ID
     * @return 逾期数量
     */
    public int countOverdue(Long fondsId) {
        return borrowingMapper.countOverdue(fondsId);
    }

    /**
     * 查询逾期借阅列表
     *
     * @param fondsId 全宗ID
     * @return 逾期借阅列表
     */
    public List<ArchiveBorrowing> listOverdue(Long fondsId) {
        return borrowingMapper.selectOverdueList(fondsId);
    }

    /**
     * 检查并更新逾期状态
     * 应该由定时任务调用
     *
     * @param fondsId 全宗ID
     * @return 更新的逾期数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateOverdueStatus(Long fondsId) {
        List<ArchiveBorrowing> overdueList = borrowingMapper.selectOverdueList(fondsId);
        int updated = 0;
        LocalDate today = LocalDate.now();

        for (ArchiveBorrowing borrowing : overdueList) {
            if ("borrowed".equals(borrowing.getStatus())
                && borrowing.getExpectedReturnDate() != null
                && today.isAfter(borrowing.getExpectedReturnDate())) {

                borrowingMapper.updateStatus(borrowing.getId(), "overdue");
                updated++;
            }
        }

        return updated;
    }
}
