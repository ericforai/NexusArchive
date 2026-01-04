// Input: Lombok、Java 标准库
// Output: VoucherDataDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

/**
 * 凭证分录数据 DTO
 */
@Data
public class VoucherDataDto {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 源数据
     */
    private String sourceData;

    /**
     * 凭证字
     */
    private String voucherWord;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 文档日期
     */
    private String docDate;

    /**
     * 创建人
     */
    private String creator;
}
