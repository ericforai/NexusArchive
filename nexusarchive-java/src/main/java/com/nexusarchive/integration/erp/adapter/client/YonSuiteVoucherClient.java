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
     * <p>简化流程：直接使用列表 API 数据，不调用详情 API。</p>
     * <p>原因分析：</p>
     * <ul>
     *   <li>列表 API 的 VoucherBody 已包含 debitOrg/creditOrg 字段（金额）</li>
     *   <li>列表 API 的 AccSubject 包含 name 字段（科目名称）</li>
     *   <li>详情 API 反而不返回科目名称（只有科目代码）</li>
     *   <li>调用详情 API 会造成 N+1 性能问题（100 条凭证 = 101 次 API 调用）</li>
     * </ul>
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

            List<AccountingSipDto> vouchers = new ArrayList<>();
            if (response.getData().getRecordList() != null) {
                for (var record : response.getData().getRecordList()) {
                    if (record.getHeader() != null) {
                        try {
                            AccountingSipDto dto = erpMapper.mapToSipDto(record, "yonsuite", config);
                            if (dto != null && dto.getEntries() != null && !dto.getEntries().isEmpty()) {
                                vouchers.add(dto);
                                log.debug("凭证映射成功: yonId={}, voucherNo={}",
                                    record.getHeader().getId(), dto.getHeader().getVoucherNumber());
                            }
                        } catch (Exception e) {
                            log.warn("凭证映射失败 (yonId: {}): {}",
                                record.getHeader().getId(), e.getMessage());
                        }
                    }
                }
            }

            log.info("YonSuite 凭证同步完成: 总数={}, 成功={}",
                response.getData() != null ? response.getData().getRecordCount() : 0, vouchers.size());
            return vouchers;
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

            // 详情 API 返回扁平结构，需要包装为列表 API 的嵌套结构
            // 这样才能使用相同的映射配置
            var wrappedDetail = wrapDetailAsRecord(response.getData());
            return erpMapper.mapToSipDto(wrappedDetail, "yonsuite", config);
        } catch (Exception e) {
            log.error("YonSuite getVoucherDetail error", e);
            return null;
        }
    }

    /**
     * 将详情 API 的扁平响应包装为列表 API 的嵌套结构
     * 详情 API: VoucherDetail { id, displayName, makeTime, bodies }
     * 列表 API: VoucherRecord { header: VoucherHeader { id, displayName, makeTime }, body: VoucherBody[] }
     */
    private Object wrapDetailAsRecord(YonVoucherDetailResponse.VoucherDetail detail) {
        // 创建一个包装对象，使其结构与列表 API 的 VoucherRecord 一致
        return new Object() {
            public final YonVoucherListResponse.VoucherHeader header = toVoucherHeader(detail);
            public final List<YonVoucherListResponse.VoucherBody> body = toVoucherBodies(detail.getBodies());

            private YonVoucherListResponse.VoucherHeader toVoucherHeader(YonVoucherDetailResponse.VoucherDetail d) {
                YonVoucherListResponse.VoucherHeader h = new YonVoucherListResponse.VoucherHeader();
                h.setId(d.getId());
                h.setBillcode(d.getBillCode());
                h.setDisplaybillcode(d.getDisplayName());
                h.setDescription(d.getDescription());
                h.setPeriod(d.getPeriodUnion());
                h.setDisplayname(d.getDisplayName());  // 注意：字段名是 displayname
                h.setSrcsystem(d.getSrcSystem());
                h.setVoucherstatus(d.getVoucherStatus());
                h.setTotalDebitOrg(d.getTotalDebitOrg());
                h.setTotalCreditOrg(d.getTotalCreditOrg());
                h.setMaketime(d.getMakeTime());  // 注意：字段名是 maketime
                h.setTs(d.getTs());
                // 转换 RefObject (不同类型)
                if (d.getMakerObj() != null) {
                    YonVoucherListResponse.RefObject maker = new YonVoucherListResponse.RefObject();
                    maker.setId(d.getMakerObj().getId());
                    maker.setCode(d.getMakerObj().getCode());
                    maker.setName(d.getMakerObj().getName());
                    h.setMaker(maker);
                }
                if (d.getAuditorObj() != null) {
                    YonVoucherListResponse.RefObject auditor = new YonVoucherListResponse.RefObject();
                    auditor.setId(d.getAuditorObj().getId());
                    auditor.setCode(d.getAuditorObj().getCode());
                    auditor.setName(d.getAuditorObj().getName());
                    h.setAuditor(auditor);
                }
                if (d.getTallyManObj() != null) {
                    YonVoucherListResponse.RefObject tallyman = new YonVoucherListResponse.RefObject();
                    tallyman.setId(d.getTallyManObj().getId());
                    tallyman.setCode(d.getTallyManObj().getCode());
                    tallyman.setName(d.getTallyManObj().getName());
                    h.setTallyman(tallyman);
                }
                return h;
            }

            @SuppressWarnings("unchecked")
            private List<YonVoucherListResponse.VoucherBody> toVoucherBodies(List<YonVoucherDetailResponse.VoucherBodyDetail> bodies) {
                if (bodies == null) {
                    return new ArrayList<>();
                }
                List<YonVoucherListResponse.VoucherBody> result = new ArrayList<>();
                for (var b : bodies) {
                    YonVoucherListResponse.VoucherBody body = new YonVoucherListResponse.VoucherBody();
                    body.setId(b.getId());
                    body.setVoucherid(b.getId());  // 详情 API 没有直接 voucherid，使用 id
                    body.setRecordnumber(b.getRecordNumber());
                    body.setDescription(b.getDescription());
                    body.setDebitOriginal(b.getDebitOriginal());
                    body.setCreditOriginal(b.getCreditOriginal());
                    body.setDebitOrg(b.getDebitOrg());
                    body.setCreditOrg(b.getCreditOrg());
                    // 构造 AccSubject 对象
                    YonVoucherListResponse.AccSubject acc = new YonVoucherListResponse.AccSubject();
                    acc.setId(b.getAccSubjectVid());
                    acc.setCode(b.getAccSubject());
                    body.setAccsubject(acc);
                    result.add(body);
                }
                return result;
            }
        };
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
}
