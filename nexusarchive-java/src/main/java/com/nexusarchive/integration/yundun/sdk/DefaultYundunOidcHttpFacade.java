// Input: OIDC 接口参数
// Output: DefaultYundunOidcHttpFacade 默认实现
// Pos: 云盾 OIDC HTTP 适配层

package com.nexusarchive.integration.yundun.sdk;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DefaultYundunOidcHttpFacade implements YundunOidcHttpFacade {

    @Override
    public String requestAccessToken(String accessTokenUrl, String code, String clientId, String clientSecret,
                                     String redirectUri) {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));
        if (StringUtils.isNotBlank(redirectUri)) {
            params.add(new BasicNameValuePair("redirect_uri", redirectUri));
        }
        return doPostForm(accessTokenUrl, params);
    }

    @Override
    public String requestUserInfo(String userInfoUrl, String authorizationHeaderValue) {
        return doGet(userInfoUrl, List.of(new BasicNameValuePair("Authorization", authorizationHeaderValue)));
    }

    private String doPostForm(String url, List<NameValuePair> params) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(url, "POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            connection.setDoOutput(true);
            byte[] body = encodeForm(params).getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }
            return readResponse(connection);
        } catch (Exception e) {
            throw new IllegalStateException("调用云盾 OIDC token 接口失败", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String doGet(String url, List<NameValuePair> headers) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(url, "GET");
            for (NameValuePair header : headers) {
                connection.setRequestProperty(header.getName(), header.getValue());
            }
            return readResponse(connection);
        } catch (Exception e) {
            throw new IllegalStateException("调用云盾 OIDC userInfo 接口失败", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection openConnection(String url, String method) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setUseCaches(false);
        return connection;
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String response;
        try (InputStream inputStream = stream) {
            response = inputStream == null ? "" : new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (status >= 400) {
            throw new IllegalStateException("HTTP " + status + ": " + response);
        }
        return response;
    }

    private String encodeForm(List<NameValuePair> params) {
        return params.stream()
                .map(param -> encodeValue(param.getName()) + "=" + encodeValue(param.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encodeValue(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }
}
