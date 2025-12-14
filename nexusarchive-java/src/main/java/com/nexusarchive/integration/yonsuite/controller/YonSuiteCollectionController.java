package com.nexusarchive.integration.yonsuite.controller;

import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.YonCollectionFileRequest;
import com.nexusarchive.integration.yonsuite.dto.YonCollectionFileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * YonSuite 收款单文件接口
 */
@RestController
@RequestMapping("/integration/yonsuite/collection")
@RequiredArgsConstructor
@Slf4j
public class YonSuiteCollectionController {

    private final YonSuiteClient yonSuiteClient;

    /**
     * 获取收款单文件下载地址
     * POST /integration/yonsuite/collection/files
     * Query: access_token (可选，如不传则使用默认配置获取)
     */
    @PostMapping("/files")
    public ResponseEntity<YonCollectionFileResponse> getCollectionFiles(
            @RequestParam(value = "access_token", required = false) String accessToken,
            @RequestBody YonCollectionFileRequest request) {
        log.info("YonSuite collection files request: {}", request.getFileId());
        YonCollectionFileResponse response = yonSuiteClient.queryCollectionFiles(accessToken, request);
        return ResponseEntity.ok(response);
    }
}
