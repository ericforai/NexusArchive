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

import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class YonSuiteClient {

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;

    private final YonAuthService yonAuthService;
    private final YonSuiteRequestBuilder requestBuilder;

    public YonSuiteClient(YonAuthService yonAuthService, YonSuiteRequestBuilder requestBuilder) {
        this.yonAuthService = yonAuthService;
        this.requestBuilder = requestBuilder;
    }

    private String getToken(String accessToken) {
        return (accessToken != null && !accessToken.isEmpty()) ? accessToken : yonAuthService.getAccessToken();
    }

    public YonVoucherListResponse queryVouchers(String accessToken, YonVoucherListRequest request) {
        return requestBuilder.post(baseUrl + "/yonbip/fi/ficloud/openapi/voucher/queryVouchers", getToken(accessToken), request, YonVoucherListResponse.class);
    }

    public YonVoucherDetailResponse queryVoucherById(String accessToken, String voucherId) {
        JSONObject body = new JSONObject(); body.putOnce("voucherId", voucherId);
        return requestBuilder.post(baseUrl + "/yonbip/EFI/openapi/voucher/queryVoucherById", getToken(accessToken), body.toString(), YonVoucherDetailResponse.class);
    }

    public YonAttachmentListResponse queryVoucherAttachments(String accessToken, String voucherId) {
        JSONObject body = new JSONObject(); body.putOnce("id", voucherId);
        return requestBuilder.post(baseUrl + "/yonbip/digitalModel/adm/attachmentInfo/query", getToken(accessToken), body.toString(), YonAttachmentListResponse.class);
    }

    public byte[] downloadFile(String url) {
        if (url == null || url.isEmpty() || !isValidDownloadUrl(url)) return null;
        try (var resp = cn.hutool.http.HttpRequest.get(url).timeout(60_000).execute()) { return resp.bodyBytes(); }
        catch (Exception e) { throw new RuntimeException("Download failed"); }
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
        return requestBuilder.post(baseUrl + "/yonbip/EFI/collection/list", getToken(accessToken), request, YonCollectionBillResponse.class);
    }

    public YonCollectionDetailResponse queryCollectionDetail(String accessToken, String collectionId) {
        return requestBuilder.get(baseUrl + "/yonbip/EFI/collection/detail", getToken(accessToken), collectionId, YonCollectionDetailResponse.class);
    }
}
