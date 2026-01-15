// Input: Lombok、Jackson
// Output: VoucherAttachmentRequest 类
// Pos: YonSuite 集成 - DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 凭证附件查询请求
 * 对应 YonSuite API: /yonbip/EFI/rest/v1/openapi/queryBusinessFiles
 */
@Data
public class VoucherAttachmentRequest {

    /**
     * 凭证 ID 集合
     */
    @JsonProperty("businessIds")
    private List<String> businessIds;
}
