// Input: Lombok、Java 标准库、本地模块
// Output: BatchIngestRequest 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import com.nexusarchive.dto.sip.AccountingSipDto;
import lombok.Data;
import java.util.List;

@Data
public class BatchIngestRequest {
    private String sourceSystem;
    private String batchId;
    private List<AccountingSipDto> vouchers;
}
