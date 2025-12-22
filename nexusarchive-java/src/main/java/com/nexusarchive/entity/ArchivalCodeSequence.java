// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ArchivalCodeSequence 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 档号生成计数器
 * 对应表: sys_archival_code_sequence
 */
@Data
@TableName("sys_archival_code_sequence")
public class ArchivalCodeSequence {

    /**
     * 全宗号 (Composite Key)
     */
    private String fondsCode;

    /**
     * 会计年度 (Composite Key)
     */
    private String fiscalYear;

    /**
     * 档案类别 (Composite Key)
     */
    private String categoryCode;

    /**
     * 当前流水号
     */
    private Integer currentVal;

    private LocalDateTime updatedTime;
}
