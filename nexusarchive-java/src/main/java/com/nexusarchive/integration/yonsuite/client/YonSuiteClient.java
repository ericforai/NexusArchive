// Input: Hutool HTTP, Jackson, Lombok, Spring Framework
// Output: YonSuiteClient (外部 ERP 客户端)
// Pos: 集成层 - 客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.client;

import cn.hutool.json.JSONObject;
import com.nexusarchive.integration.yonsuite.dto.*;
import com.nexusarchive.integration.yonsuite.service.YonAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class YonSuiteClient {

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;

    private final YonAuthService yonAuthService;
    private final YonSuiteRequestBuilder requestBuilder;
    private final YonSuiteVoucherClient yonSuiteVoucherClient;
    private final YonSuiteCollectionClient yonSuiteCollectionClient;
    private final YonSuitePaymentClient yonSuitePaymentClient;

    public YonSuiteClient(
            YonAuthService yonAuthService,
            YonSuiteRequestBuilder requestBuilder,
            YonSuiteVoucherClient yonSuiteVoucherClient,
            YonSuiteCollectionClient yonSuiteCollectionClient,
            YonSuitePaymentClient yonSuitePaymentClient
    ) {
        this.yonAuthService = yonAuthService;
        this.requestBuilder = requestBuilder;
        this.yonSuiteVoucherClient = yonSuiteVoucherClient;
        this.yonSuiteCollectionClient = yonSuiteCollectionClient;
        this.yonSuitePaymentClient = yonSuitePaymentClient;
    }

    private String getToken(String accessToken) {
        return (accessToken != null && !accessToken.isEmpty()) ? accessToken : yonAuthService.getAccessToken();
    }

    public YonVoucherListResponse queryVouchers(String accessToken, YonVoucherListRequest request) {
        return yonSuiteVoucherClient.queryVouchers(accessToken, request);
    }

    public YonVoucherDetailResponse queryVoucherById(String accessToken, String voucherId) {
        return yonSuiteVoucherClient.queryVoucherById(accessToken, voucherId);
    }

    public YonAttachmentListResponse queryVoucherAttachments(String accessToken, String voucherId) {
        return yonSuiteVoucherClient.queryVoucherAttachments(accessToken, voucherId);
    }

    public YonAttachmentListResponse queryAttachments(String accessToken, String businessId) {
        return queryVoucherAttachments(accessToken, businessId);
    }

    public YonPaymentDetailResponse queryPaymentDetail(String accessToken, String paymentId) {
        return yonSuitePaymentClient.queryPaymentDetail(accessToken, paymentId);
    }

    public YonRefundFileResponse queryRefundFileUrls(String accessToken, YonRefundFileRequest request) {
        return yonSuitePaymentClient.queryRefundFileUrls(accessToken, request);
    }

    public YonCollectionFileResponse queryCollectionFiles(String accessToken, YonCollectionFileRequest request) {
        return yonSuiteCollectionClient.queryCollectionFiles(accessToken, request);
    }

    public YonPaymentApplyFileResponse queryPaymentApplyFileUrls(String accessToken, YonPaymentApplyFileRequest request) {
        return requestBuilder.post(
                baseUrl + "/yonbip/EFI/paymentApply/file/url",
                getToken(accessToken),
                request,
                YonPaymentApplyFileResponse.class
        );
    }

    public String getTokenWithCredentials(String appKey, String appSecret) {
        return yonAuthService.getAccessToken(appKey, appSecret);
    }

    public byte[] downloadFile(String url) {
        if (url == null || url.isEmpty() || !isValidDownloadUrl(url)) return null;
        try (var resp = cn.hutool.http.HttpRequest.get(url).timeout(60_000).execute()) { return resp.bodyBytes(); }
        catch (Exception e) { throw new RuntimeException("Download failed"); }
    }

    public InputStream downloadStream(String url) {
        byte[] bytes = downloadFile(url);
        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }

    public <T> T downloadFileWithCallback(String url, Function<java.io.InputStream, T> callback) {
        if (url == null || url.isEmpty() || !isValidDownloadUrl(url)) return null;
        try (var resp = cn.hutool.http.HttpRequest.get(url).timeout(60_000).execute(); var is = resp.bodyStream()) { return callback.apply(is); }
        catch (Exception e) { throw new RuntimeException("Download failed"); }
    }

    private boolean isValidDownloadUrl(String url) {
        try {
            String host = new java.net.URL(url).getHost();
            return host != null && (host.endsWith("yonyoucloud.com") || host.endsWith("yonyou.com") || host.endsWith("diwork.com"));
        } catch (Exception e) { return false; }
    }

    public com.nexusarchive.integration.erp.dto.FeedbackResult feedbackArchivalStatus(String accessToken, String voucherId, String archivalCode) {
        return com.nexusarchive.integration.erp.dto.FeedbackResult.success(voucherId, archivalCode, "YONSUITE", true);
    }
    
    public YonCollectionBillResponse queryCollectionBills(String accessToken, YonCollectionBillRequest request) {
        return yonSuiteCollectionClient.queryCollectionBills(accessToken, request);
    }

    public YonCollectionDetailResponse queryCollectionDetail(String accessToken, String collectionId) {
        return yonSuiteCollectionClient.queryCollectionDetail(accessToken, collectionId);
    }
}
