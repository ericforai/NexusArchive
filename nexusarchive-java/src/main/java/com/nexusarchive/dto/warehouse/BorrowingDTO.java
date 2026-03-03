// Input: 借阅业务需求
// Output: BorrowingDTO 和相关 VO 类
// Pos: src/main/java/com/nexusarchive/dto/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * 借阅 DTO
 *
 * 用于创建借阅申请的数据传输对象
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Schema(description = "借阅创建/更新请求")
public class BorrowingDTO {

    @Schema(description = "档案袋ID", example = "1")
    @NotNull(message = "档案袋ID不能为空")
    private Long containerId;

    @Schema(description = "借阅人", example = "张三")
    @NotBlank(message = "借阅人不能为空")
    @Size(max = 100, message = "借阅人名称长度不能超过100个字符")
    private String borrower;

    @Schema(description = "借阅部门", example = "财务部")
    @Size(max = 200, message = "借阅部门长度不能超过200个字符")
    private String borrowerDept;

    @Schema(description = "借阅日期", example = "2024-01-13")
    private LocalDate borrowDate;

    @Schema(description = "预计归还日期", example = "2024-02-13")
    @NotNull(message = "预计归还日期不能为空")
    @Future(message = "预计归还日期必须晚于当前日期")
    private LocalDate expectedReturnDate;

    @Schema(description = "所属全宗ID", example = "1")
    private Long fondsId;

    @Schema(description = "备注", example = "备注信息")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}

/**
 * 借阅详情 VO
 */
@Data
@Schema(description = "借阅详情响应")
class BorrowingDetailVO {

    @Schema(description = "借阅ID")
    private Long id;

    @Schema(description = "借阅单号")
    private String borrowNo;

    @Schema(description = "档案袋ID")
    private Long containerId;

    @Schema(description = "袋号")
    private String containerNo;

    @Schema(description = "借阅人")
    private String borrower;

    @Schema(description = "借阅部门")
    private String borrowerDept;

    @Schema(description = "借阅日期")
    private String borrowDate;

    @Schema(description = "预计归还日期")
    private String expectedReturnDate;

    @Schema(description = "借阅状态")
    private String status;

    @Schema(description = "实际归还日期")
    private String actualReturnDate;

    @Schema(description = "审批人")
    private String approvedBy;

    @Schema(description = "审批时间")
    private String approvedAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "是否逾期")
    private Boolean isOverdue;
}
