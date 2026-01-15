// Input: Hutool、Lombok、Spring Framework、Java 标准库
// Output: YonPaymentApplyFileService 类
// Pos: YonSuite 集成 - 服务层

package com.nexusarchive.integration.yonsuite.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.yonsuite.dto.YonPaymentApplyFileRequest;
import com.nexusarchive.integration.yonsuite.dto.YonPaymentApplyFileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * YonSuite 付款申请单文件查询服务
 * <p>
 * 通过文件 ID 获取付款申请单文件下载地址
 * </p>
 * <p>接口: POST /yonbip/EFI/paymentApply/file/url</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class YonPaymentApplyFileService {

    private static final String ENDPOINT = "/yonbip/EFI/paymentApply/file/url";
    private static final int MAX_FILE_IDS = 20;

    private final YonAuthService yonAuthService;

    /**
     * 批量查询文件下载地址
     *
     * @param config ERP 配置
     * @param fileIds 文件 ID 列表 (最多 20 个)
     * @return 文件下载信息列表
     * @throws BusinessException 当查询失败时抛出
     */
    public List<YonPaymentApplyFileResponse.FileData> queryFileUrls(
            ErpConfig config,
            List<String> fileIds) {

        // 参数校验
        if (config == null || config.getBaseUrl() == null) {
            throw new BusinessException("ERP 配置无效: BaseUrl 为空");
        }

        if (fileIds == null || fileIds.isEmpty()) {
            log.warn("文件 ID 列表为空");
            return List.of();
        }

        if (fileIds.size() > MAX_FILE_IDS) {
            throw new BusinessException(String.format(
                "文件 ID 数量超过限制: %d > %d", fileIds.size(), MAX_FILE_IDS));
        }

        log.info("查询付款申请单文件下载地址: 文件数={}, fileIds={}",
                 fileIds.size(), fileIds);

        try {
            // 获取访问令牌
            String accessToken = yonAuthService.getAccessToken(
                config.getAppKey(),
                config.getAppSecret()
            );

            // 构建完整 URL
            String fullUrl = config.getBaseUrl() + ENDPOINT;
            String urlWithToken = fullUrl + "?access_token=" +
                URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

            // 构建请求体
            YonPaymentApplyFileRequest request = YonPaymentApplyFileRequest.of(fileIds);
            String requestBody = JSONUtil.toJsonStr(request);

            log.debug("请求 URL: {}, Body: {}", urlWithToken, requestBody);

            // 发送请求
            HttpResponse response = HttpRequest.post(urlWithToken)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .timeout(10000)
                .execute();

            // 检查 HTTP 状态
            if (!response.isOk()) {
                log.error("HTTP 请求失败: Status={}", response.getStatus());
                throw new BusinessException("HTTP 请求失败: " + response.getStatus());
            }

            // 解析响应
            String responseBody = response.body();
            YonPaymentApplyFileResponse fileResponse = JSONUtil.toBean(
                responseBody,
                YonPaymentApplyFileResponse.class
            );

            // 检查业务状态码
            if (!fileResponse.isSuccess()) {
                log.error("查询失败: code={}, message={}",
                          fileResponse.getCode(), fileResponse.getMessage());
                throw new BusinessException(
                    "查询付款申请单文件失败: " + fileResponse.getMessage());
            }

            List<YonPaymentApplyFileResponse.FileData> validFiles =
                fileResponse.getValidFiles();

            log.info("查询成功: 返回文件数={}, 有效文件数={}",
                     fileResponse.getData() != null ? fileResponse.getData().size() : 0,
                     validFiles.size());

            return validFiles;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询付款申请单文件地址异常", e);
            throw new BusinessException("查询付款申请单文件地址异常: " + e.getMessage());
        }
    }

    /**
     * 查询单个文件下载地址
     *
     * @param config ERP 配置
     * @param fileId 文件 ID
     * @return 文件下载信息
     */
    public YonPaymentApplyFileResponse.FileData queryFileUrl(
            ErpConfig config,
            String fileId) {

        List<YonPaymentApplyFileResponse.FileData> results =
            queryFileUrls(config, List.of(fileId));

        return results.isEmpty() ? null : results.get(0);
    }
}
