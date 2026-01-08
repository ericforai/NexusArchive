// Input: Spring Framework、Lombok、YonSuiteClient、ERP DTO、SIP DTO、ErpMapper
// Output: YonSuiteVoucherClient 类
// Pos: 集成模块 - ERP 适配器客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.mapping.ErpMapper;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListRequest;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * YonSuite 凭证客户端
 * 负责处理凭证相关的操作：查询凭证列表、凭证详情、凭证附件
 *
 * <p>重构说明：</p>
 * <ul>
 *   <li>移除硬编码的转换逻辑，使用 ErpMapper 框架进行统一映射</li>
 *   <li>客户端现在专注于 API 调用，数据转换委托给 ErpMapper</li>
 *   <li>返回标准化的 AccountingSipDto 而非 VoucherDTO</li>
 * </ul>
 *
 * @author Agent D (基础设施工程师)
 */
@Component("erpAdapterVoucherClient")
@RequiredArgsConstructor
@Slf4j
public class YonSuiteVoucherClient {

    private final YonSuiteClient yonSuiteClient;
    private final ErpMapper erpMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 同步指定期间范围的凭证
     *
     * @param accessToken 访问令牌（可为空）
     * @param accbookCode 账套代码
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param config      ERP 配置
     * @return 标准化 SIP DTO 列表
     */
    public List<AccountingSipDto> syncVouchers(String accessToken, String accbookCode,
                                          LocalDate startDate, LocalDate endDate, ErpConfig config) {
        try {
            YonVoucherListRequest request = buildVoucherListRequest(accbookCode, startDate, endDate);
            YonVoucherListResponse response = yonSuiteClient.queryVouchers(accessToken, request);

            if (!"200".equals(response.getCode()) || response.getData() == null) {
                log.warn("YonSuite 同步凭证失败 (组织: {}): {}", accbookCode, response.getMessage());
                return Collections.emptyList();
            }

            return convertToSipDtos(response, accbookCode, config);
        } catch (Exception e) {
            log.error("YonSuite 同步凭证异常 (组织: {})", accbookCode, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取凭证详情
     *
     * @param accessToken 访问令牌（可为空）
     * @param voucherId   凭证 ID
     * @param config      ERP 配置
     * @return 标准化 SIP DTO，失败返回 null
     */
    public AccountingSipDto getVoucherDetail(String accessToken, String voucherId, ErpConfig config) {
        try {
            YonVoucherDetailResponse response = yonSuiteClient.queryVoucherById(accessToken, voucherId);

            if (response == null || response.getData() == null) {
                return null;
            }

            return erpMapper.mapToSipDto(response.getData(), "yonsuite", config);
        } catch (Exception e) {
            log.error("YonSuite getVoucherDetail error", e);
            return null;
        }
    }

    /**
     * 获取凭证附件列表（保持兼容性，返回 AttachmentDTO）
     *
     * @param accessToken 访问令牌（可为空）
     * @param voucherId   凭证 ID
     * @return 附件列表
     * @deprecated 附件信息现在通过 AccountingSipDto.getAttachments() 获取
     */
    @Deprecated
    public List<com.nexusarchive.integration.erp.dto.AttachmentDTO> getAttachments(String accessToken, String voucherId) {
        try {
            var response = yonSuiteClient.queryVoucherAttachments(accessToken, voucherId);

            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }

            List<com.nexusarchive.integration.erp.dto.AttachmentDTO> attachments = new ArrayList<>();
            for (var item : response.getData()) {
                com.nexusarchive.integration.erp.dto.AttachmentDTO att =
                    com.nexusarchive.integration.erp.dto.AttachmentDTO.builder()
                        .attachmentId(item.getId())
                        .fileName(item.getFileName())
                        .fileType(item.getFileExtension())
                        .fileSize(item.getFileSize())
                        .downloadUrl(item.getUrl())
                        .build();
                attachments.add(att);
            }
            return attachments;
        } catch (Exception e) {
            log.error("YonSuite getAttachments error", e);
            return Collections.emptyList();
        }
    }

    /**
     * 测试连接（通过查询少量凭证）
     *
     * @param accessToken 访问令牌（可为空）
     * @param accbookCode 账套代码
     * @return 连接是否成功
     */
    public boolean testConnection(String accessToken, String accbookCode) {
        try {
            YonVoucherListRequest request = new YonVoucherListRequest();
            request.setAccbookCode(accbookCode);
            request.setPeriodStart(LocalDate.now().format(DATE_FORMATTER));
            request.setPeriodEnd(request.getPeriodStart());

            YonVoucherListRequest.Pager pager = new YonVoucherListRequest.Pager();
            pager.setPageIndex(1);
            pager.setPageSize(1);
            request.setPager(pager);

            YonVoucherListResponse response = yonSuiteClient.queryVouchers(accessToken, request);
            return "200".equals(response.getCode());
        } catch (Exception e) {
            log.error("YonSuite 凭证连接测试失败", e);
            return false;
        }
    }

    /**
     * 构建凭证列表查询请求
     */
    private YonVoucherListRequest buildVoucherListRequest(String accbookCode,
                                                           LocalDate startDate, LocalDate endDate) {
        YonVoucherListRequest request = new YonVoucherListRequest();
        request.setAccbookCode(accbookCode);
        request.setPeriodStart(startDate.format(DATE_FORMATTER));
        request.setPeriodEnd(endDate.format(DATE_FORMATTER));

        YonVoucherListRequest.Pager pager = new YonVoucherListRequest.Pager();
        pager.setPageIndex(1);
        pager.setPageSize(100);
        request.setPager(pager);

        return request;
    }

    /**
     * 使用 ErpMapper 转换响应为 AccountingSipDto 列表
     *
     * @param response    YonSuite 响应对象
     * @param accbookCode 账套代码
     * @param config      ERP 配置
     * @return SIP DTO 列表
     */
    private List<AccountingSipDto> convertToSipDtos(YonVoucherListResponse response,
                                                     String accbookCode, ErpConfig config) {
        List<AccountingSipDto> vouchers = new ArrayList<>();
        if (response.getData().getRecordList() != null) {
            for (var record : response.getData().getRecordList()) {
                if (record.getHeader() != null) {
                    try {
                        // 使用 ErpMapper 框架进行统一转换
                        AccountingSipDto sipDto = erpMapper.mapToSipDto(
                            record.getHeader(), "yonsuite", config);
                        vouchers.add(sipDto);
                    } catch (Exception e) {
                        log.warn("转换凭证失败 (ID: {}): {}",
                            record.getHeader().getId(), e.getMessage());
                    }
                }
            }
        }
        return vouchers;
    }
}
