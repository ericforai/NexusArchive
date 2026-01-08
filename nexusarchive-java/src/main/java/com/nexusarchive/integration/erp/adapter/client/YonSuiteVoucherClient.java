// Input: Spring Framework、Lombok、YonSuiteClient、ERP DTO
// Output: YonSuiteVoucherClient 类
// Pos: 集成模块 - ERP 适配器客户端

package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.integration.erp.dto.AttachmentDTO;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
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
 * @author Agent D (基础设施工程师)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YonSuiteVoucherClient {

    private final YonSuiteClient yonSuiteClient;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 同步指定期间范围的凭证
     *
     * @param accessToken 访问令牌（可为空）
     * @param accbookCode 账套代码
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 凭证列表
     */
    public List<VoucherDTO> syncVouchers(String accessToken, String accbookCode,
                                          LocalDate startDate, LocalDate endDate) {
        try {
            YonVoucherListRequest request = buildVoucherListRequest(accbookCode, startDate, endDate);
            YonVoucherListResponse response = yonSuiteClient.queryVouchers(accessToken, request);

            if (!"200".equals(response.getCode()) || response.getData() == null) {
                log.warn("YonSuite 同步凭证失败 (组织: {}): {}", accbookCode, response.getMessage());
                return Collections.emptyList();
            }

            return convertToVoucherDTOs(response, accbookCode);
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
     * @return 凭证详情，失败返回 null
     */
    public VoucherDTO getVoucherDetail(String accessToken, String voucherId) {
        try {
            YonVoucherDetailResponse response = yonSuiteClient.queryVoucherById(accessToken, voucherId);

            if (response == null || response.getData() == null) {
                return null;
            }

            return convertToVoucherDTO(response.getData());
        } catch (Exception e) {
            log.error("YonSuite getVoucherDetail error", e);
            return null;
        }
    }

    /**
     * 获取凭证附件列表
     *
     * @param accessToken 访问令牌（可为空）
     * @param voucherId   凭证 ID
     * @return 附件列表
     */
    public List<AttachmentDTO> getAttachments(String accessToken, String voucherId) {
        try {
            var response = yonSuiteClient.queryVoucherAttachments(accessToken, voucherId);

            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }

            List<AttachmentDTO> attachments = new ArrayList<>();
            for (var item : response.getData()) {
                AttachmentDTO att = AttachmentDTO.builder()
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
     * 转换响应为 DTO 列表
     */
    private List<VoucherDTO> convertToVoucherDTOs(YonVoucherListResponse response, String accbookCode) {
        List<VoucherDTO> vouchers = new ArrayList<>();
        if (response.getData().getRecordList() != null) {
            for (var record : response.getData().getRecordList()) {
                if (record.getHeader() != null) {
                    vouchers.add(convertToVoucherDTO(record.getHeader(), accbookCode, record.getBody()));
                }
            }
        }
        return vouchers;
    }

    /**
     * 转换单个凭证记录
     */
    private VoucherDTO convertToVoucherDTO(YonVoucherListResponse.VoucherHeader header,
                                            String accbookCode, List<?> body) {
        LocalDate voucherDate = parseVoucherDate(header.getMaketime());
        String voucherNo = deriveVoucherNo(header);
        String voucherWord = deriveVoucherWord(header);
        String summary = deriveSummary(header, body);

        VoucherDTO dto = VoucherDTO.builder()
                .voucherId(header.getId())
                .voucherNo(voucherNo)
                .voucherWord(voucherWord)
                .voucherDate(voucherDate)
                .accountPeriod(header.getPeriod())
                .summary(summary)
                .status(header.getVoucherstatus())
                .debitTotal(header.getTotalDebitOrg())
                .creditTotal(header.getTotalCreditOrg())
                .accbookCode(accbookCode)
                .build();

        if (header.getMaker() != null) {
            dto.setCreator(header.getMaker().getName());
        }
        if (header.getAuditor() != null) {
            dto.setAuditor(header.getAuditor().getName());
        }
        if (header.getTallyman() != null) {
            dto.setPoster(header.getTallyman().getName());
        }

        return dto;
    }

    /**
     * 转换凭证详情响应
     */
    private VoucherDTO convertToVoucherDTO(YonVoucherDetailResponse.VoucherDetail detail) {
        VoucherDTO dto = VoucherDTO.builder()
                .voucherId(detail.getId())
                .voucherNo(detail.getDisplayName())
                .accountPeriod(detail.getPeriodUnion())
                .summary(detail.getDescription())
                .status(detail.getVoucherStatus())
                .debitTotal(detail.getTotalDebitOrg())
                .creditTotal(detail.getTotalCreditOrg())
                .build();

        if (detail.getMakerObj() != null) {
            dto.setCreator(detail.getMakerObj().getName());
        }
        if (detail.getAuditorObj() != null) {
            dto.setAuditor(detail.getAuditorObj().getName());
        }
        if (detail.getTallyManObj() != null) {
            dto.setPoster(detail.getTallyManObj().getName());
        }

        return dto;
    }

    /**
     * 解析凭证日期
     */
    private LocalDate parseVoucherDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 推导凭证号
     */
    private String deriveVoucherNo(YonVoucherListResponse.VoucherHeader header) {
        String voucherNo = header.getDisplayname();
        if (voucherNo == null || voucherNo.isEmpty()) {
            voucherNo = header.getDisplaybillcode();
        }
        return voucherNo;
    }

    /**
     * 推导凭证字
     */
    private String deriveVoucherWord(YonVoucherListResponse.VoucherHeader header) {
        if (header.getVouchertype() != null) {
            String voucherWord = header.getVouchertype().getVoucherstr();
            if (voucherWord == null) {
                voucherWord = header.getVouchertype().getName();
            }
            return voucherWord;
        }
        return null;
    }

    /**
     * 推导摘要
     */
    private String deriveSummary(YonVoucherListResponse.VoucherHeader header, List<?> body) {
        String summary = header.getDescription();
        if ((summary == null || summary.isEmpty()) && body != null && !body.isEmpty()) {
            // 假设 body 的第一个元素有 description
            summary = ""; // 简化处理
        }
        return summary;
    }
}
