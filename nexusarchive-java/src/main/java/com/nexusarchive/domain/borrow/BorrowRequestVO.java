package com.nexusarchive.domain.borrow;

import com.nexusarchive.common.constants.DateFormat;
import com.nexusarchive.entity.BorrowRequest;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.format.DateTimeFormatter;

/**
 * 借阅申请展示对象
 */
@Data
public class BorrowRequestVO extends BorrowRequest {
    
    /**
     * 档案标题 (如果是多个档案，则为 "Title1 等N个档案")
     */
    private String archiveTitle;

    /**
     * 格式化日期
     */
    private String borrowDate;
    private String expectedReturnDate;
    private String actualReturnDate;
    
    // 冗余是为了方便前端直接映射
    private String userName; // specific for frontend compatibility if needed
    
    public static BorrowRequestVO from(BorrowRequest entity, String titles) {
        BorrowRequestVO vo = new BorrowRequestVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setArchiveTitle(titles);
        
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DateFormat.DATE);
        if (entity.getExpectedStartDate() != null) vo.setBorrowDate(entity.getExpectedStartDate().format(fmt));
        if (entity.getExpectedEndDate() != null) vo.setExpectedReturnDate(entity.getExpectedEndDate().format(fmt));
        if (entity.getActualEndDate() != null) vo.setActualReturnDate(entity.getActualEndDate().format(fmt));
        
        vo.setUserName(entity.getApplicantName());
        return vo;
    }
}
