// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ConvertLog 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@TableName("arc_convert_log")
public class ConvertLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String archiveId; // 关联的档案ID

    private String sourceFormat; // 源格式，如 PDF

    private String targetFormat; // 目标格式，如 OFD

    private String sourcePath; // 源文件路径

    private String targetPath; // 目标文件路径

    private String status; // SUCCESS, FAIL

    private String errorMessage; // 错误信息

    private Long sourceSize; // 源文件大小

    private Long targetSize; // 目标文件大小

    private Long durationMs; // 转换耗时(毫秒)

    private LocalDateTime convertTime; // 转换时间

    private LocalDateTime createdTime;
}
