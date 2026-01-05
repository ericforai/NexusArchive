// Input: Spring、Lombok、Hutool
// Output: GenericYonSuiteAdapter 类
// Pos: YonSuite 集成 - 通用适配器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.integration.yonsuite.dto.SalesOutDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.SalesOutListRequest;
import com.nexusarchive.integration.yonsuite.dto.SalesOutListResponse;
import com.nexusarchive.integration.yonsuite.dto.VoucherAttachmentRequest;
import com.nexusarchive.integration.yonsuite.dto.VoucherAttachmentResponse;
import com.nexusarchive.integration.yonsuite.mapper.SalesOutMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.VoucherPdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * YonSuite 通用适配器
 * 配置驱动：读取数据库场景配置，动态调用 ERP API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenericYonSuiteAdapter {

    private final YonAuthService yonAuthService;
    private final SalesOutMapper salesOutMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final VoucherPdfGeneratorService pdfGeneratorService;
    private final ObjectMapper objectMapper;

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;

    /**
     * 同步销售出库单列表
     *
     * @param appKey     应用Key
     * @param appSecret  应用密钥
     * @param startDate  开始日期 (yyyy-MM-dd)
     * @param endDate    结束日期 (yyyy-MM-dd)
     * @return 同步的文件ID列表
     */
    public List<String> syncSalesOutList(String appKey, String appSecret, String startDate, String endDate) {
        log.info("开始同步销售出库单列表: {} - {}", startDate, endDate);

        List<String> syncedIds = new ArrayList<>();
        int pageIndex = 1;
        int pageSize = 100;
        boolean hasMore = true;

        while (hasMore) {
            try {
                // 1. 获取 access_token
                String accessToken = yonAuthService.getAccessToken(appKey, appSecret);

                // 2. 构建请求
                SalesOutListRequest request = buildListRequest(pageIndex, pageSize, startDate, endDate);

                // 3. 调用 API
                SalesOutListResponse response = callSalesOutListApi(accessToken, request);

                if (response == null || response.getData() == null ||
                    response.getData().getRecordList() == null ||
                    response.getData().getRecordList().isEmpty()) {
                    log.info("没有更多数据，停止分页");
                    break;
                }

                // 4. 处理每条记录
                List<SalesOutListResponse.SalesOutRecord> records = response.getData().getRecordList();
                for (SalesOutListResponse.SalesOutRecord record : records) {
                    try {
                        String fileId = processSalesOutRecord(record);
                        if (fileId != null) {
                            syncedIds.add(fileId);
                        }
                    } catch (Exception e) {
                        log.error("处理销售出库单记录失败: {}", record.getId(), e);
                    }
                }

                log.info("第 {} 页处理完成，本页 {} 条", pageIndex, records.size());

                // 5. 检查是否还有更多页
                int totalCount = response.getData().getRecordCount();
                int totalPages = (int) Math.ceil((double) totalCount / pageSize);
                hasMore = pageIndex < totalPages;
                pageIndex++;

            } catch (Exception e) {
                log.error("同步销售出库单列表失败，页码: {}", pageIndex, e);
                break;
            }
        }

        log.info("销售出库单同步完成，共同步 {} 条", syncedIds.size());
        return syncedIds;
    }

    /**
     * 同步单个销售出库单详情
     *
     * @param appKey    应用Key
     * @param appSecret 应用密钥
     * @param salesOutId 销售出库单ID
     * @return 文件ID
     */
    public String syncSalesOutDetail(String appKey, String appSecret, String salesOutId) {
        log.info("开始同步销售出库单详情: salesOutId={}", salesOutId);

        try {
            // 1. 获取 access_token
            String accessToken = yonAuthService.getAccessToken(appKey, appSecret);

            // 2. 调用详情 API
            SalesOutDetailResponse response = callSalesOutDetailApi(accessToken, salesOutId);

            if (response == null || response.getData() == null) {
                log.warn("销售出库单详情不存在: {}", salesOutId);
                return null;
            }

            // 3. 映射并保存
            ArcFileContent fileContent = salesOutMapper.toPreArchiveFile(response.getData());
            if (fileContent == null) {
                return null;
            }

            // 4. 幂等性检查
            String fileId = saveOrUpdateFileContent(fileContent);

            // 5. 生成 PDF
            generatePdf(fileId, response.getData());

            log.info("销售出库单详情同步成功: salesOutId={}, fileId={}", salesOutId, fileId);
            return fileId;

        } catch (Exception e) {
            log.error("同步销售出库单详情失败: salesOutId={}", salesOutId, e);
            return null;
        }
    }

    /**
     * 查询凭证附件
     *
     * @param appKey     应用Key
     * @param appSecret  应用密钥
     * @param businessIds 凭证ID列表
     * @return 凭证ID -> 附件列表的映射
     */
    public Map<String, List<VoucherAttachmentResponse.VoucherAttachment>> queryVoucherAttachments(
            String appKey, String appSecret, List<String> businessIds) {
        log.info("开始查询凭证附件: businessIds数量={}", businessIds.size());

        try {
            // 1. 获取 access_token
            String accessToken = yonAuthService.getAccessToken(appKey, appSecret);

            // 2. 构建请求
            VoucherAttachmentRequest request = new VoucherAttachmentRequest();
            request.setBusinessIds(businessIds);

            // 3. 调用 API
            VoucherAttachmentResponse response = callVoucherAttachmentApi(accessToken, request);

            if (response == null || response.getData() == null) {
                log.warn("凭证附件查询返回空数据");
                return Map.of();
            }

            if (!"200".equals(response.getCode())) {
                throw new RuntimeException("API 调用失败: " + response.getMessage());
            }

            log.info("凭证附件查询成功: 共 {} 个凭证有附件", response.getData().size());
            return response.getData();

        } catch (Exception e) {
            log.error("查询凭证附件失败: businessIds={}", businessIds, e);
            throw new RuntimeException("查询凭证附件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用凭证附件查询 API
     */
    private VoucherAttachmentResponse callVoucherAttachmentApi(String accessToken, VoucherAttachmentRequest request) {
        try {
            String url = baseUrl + "/yonbip/EFI/rest/v1/openapi/queryBusinessFiles"
                    + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

            String requestBody = objectMapper.writeValueAsString(request);

            log.debug("调用凭证附件查询 API: url={}, request={}", url, requestBody);

            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .timeout(30_000)
                    .execute();

            String responseBody = response.body();
            log.debug("凭证附件查询 API 响应: {}", responseBody);

            return objectMapper.readValue(responseBody, VoucherAttachmentResponse.class);

        } catch (Exception e) {
            log.error("调用凭证附件查询 API 失败", e);
            throw new RuntimeException("调用 API 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建列表请求
     */
    private SalesOutListRequest buildListRequest(int pageIndex, int pageSize, String startDate, String endDate) {
        SalesOutListRequest request = new SalesOutListRequest();
        request.setPageIndex(pageIndex);
        request.setPageSize(pageSize);

        // 设置日期区间
        String dateRange = startDate + " 00:00:00|" + endDate + " 23:59:59";
        request.setVouchdate(dateRange);

        // 设置必填的 simpleVOs（空数组表示无条件查询）
        request.setSimpleVOs(new ArrayList<>());

        return request;
    }

    /**
     * 调用销售出库单列表 API
     */
    private SalesOutListResponse callSalesOutListApi(String accessToken, SalesOutListRequest request) {
        try {
            String url = baseUrl + "/yonbip/scm/salesout/list"
                    + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

            String requestBody = objectMapper.writeValueAsString(request);

            log.debug("调用销售出库单列表 API: url={}, request={}", url, requestBody);

            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .timeout(30_000)
                    .execute();

            String responseBody = response.body();
            log.debug("销售出库单列表 API 响应: {}", responseBody);

            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);

            if (!"200".equals(jsonResponse.getStr("code"))) {
                throw new RuntimeException("API 调用失败: " + jsonResponse.getStr("message"));
            }

            return objectMapper.readValue(responseBody, SalesOutListResponse.class);

        } catch (Exception e) {
            log.error("调用销售出库单列表 API 失败", e);
            throw new RuntimeException("调用 API 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用销售出库单详情 API
     */
    private SalesOutDetailResponse callSalesOutDetailApi(String accessToken, String salesOutId) {
        try {
            String url = baseUrl + "/yonbip/scm/salesout/detail"
                    + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                    + "&id=" + URLEncoder.encode(salesOutId, StandardCharsets.UTF_8);

            log.debug("调用销售出库单详情 API: url={}", url);

            HttpResponse response = HttpRequest.get(url)
                    .timeout(30_000)
                    .execute();

            String responseBody = response.body();
            log.debug("销售出库单详情 API 响应: {}", responseBody);

            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);

            if (!"200".equals(jsonResponse.getStr("code"))) {
                throw new RuntimeException("API 调用失败: " + jsonResponse.getStr("message"));
            }

            return objectMapper.readValue(responseBody, SalesOutDetailResponse.class);

        } catch (Exception e) {
            log.error("调用销售出库单详情 API 失败", e);
            throw new RuntimeException("调用 API 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理单条销售出库单记录
     */
    private String processSalesOutRecord(SalesOutListResponse.SalesOutRecord record) {
        // 先用列表数据保存基本信息
        ArcFileContent fileContent = salesOutMapper.toPreArchiveFile(record);
        if (fileContent == null) {
            return null;
        }

        return saveOrUpdateFileContent(fileContent);
    }

    /**
     * 保存或更新文件内容
     */
    private String saveOrUpdateFileContent(ArcFileContent fileContent) {
        // 幂等性检查
        ArcFileContent existing = arcFileContentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getBusinessDocNo, fileContent.getBusinessDocNo()));

        if (existing != null) {
            if ("ARCHIVED".equals(existing.getPreArchiveStatus())) {
                log.info("销售出库单已归档，跳过: {}", fileContent.getBusinessDocNo());
                return existing.getId();
            }
            // 更新
            fileContent.setId(existing.getId());
            arcFileContentMapper.updateById(fileContent);
            log.info("更新销售出库单: {}", fileContent.getBusinessDocNo());
            return existing.getId();
        } else {
            // 新增
            arcFileContentMapper.insert(fileContent);
            log.info("新增销售出库单: {}", fileContent.getBusinessDocNo());
            return fileContent.getId();
        }
    }

    /**
     * 生成 PDF
     */
    private void generatePdf(String fileId, Object sourceData) {
        try {
            String jsonData = objectMapper.writeValueAsString(sourceData);
            pdfGeneratorService.generatePdfForPreArchive(fileId, jsonData);
        } catch (Exception e) {
            log.error("生成 PDF 失败: fileId={}", fileId, e);
        }
    }
}
