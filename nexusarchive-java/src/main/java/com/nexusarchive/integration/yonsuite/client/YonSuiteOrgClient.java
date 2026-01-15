// Input: Hutool HTTP, Jackson, Lombok, Spring Framework
// Output: YonSuiteOrgClient 类
// Pos: 外部系统客户端 - YonSuite 组织架构 API
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.client;

import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.integration.yonsuite.dto.YonOrgTreeSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * YonSuite 组织架构 API Client
 * 两步同步：treeversionsync → treemembersync
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class YonSuiteOrgClient {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_SYNC_TIME = "1970-01-01 00:00:00";

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;

    private final ObjectMapper objectMapper;
    private final com.nexusarchive.integration.yonsuite.service.YonAuthService yonAuthService;

    public List<YonOrgTreeSyncResponse.OrgRecord> queryOrgs() {
        return queryOrgs(DEFAULT_SYNC_TIME);
    }

    public List<YonOrgTreeSyncResponse.OrgRecord> queryOrgs(String pubts) {
        log.info("开始从 YonSuite 同步组织架构，pubts: {}", pubts);
        List<YonOrgTreeSyncResponse.TreeVersion> treeVersions = getTreeVersions(pubts);
        if (treeVersions.isEmpty()) {
            log.warn("未找到任何树版本");
            return new ArrayList<>();
        }
        YonOrgTreeSyncResponse.TreeVersion latest = treeVersions.get(0);
        log.info("使用树版本: {} ({})", latest.getName(), latest.getVersionNo());
        return getOrgMembersByTreeVids(List.of(latest.getTreeId()), pubts);
    }

    private List<YonOrgTreeSyncResponse.TreeVersion> getTreeVersions(String pubts) {
        String token = yonAuthService.getAccessToken();
        String url = baseUrl + "/yonbip/digitalModel/openapi/treedatasync/treeversionsync?access_token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String body = String.format("{\"pubts\":\"%s\",\"funcCode\":\"adminorg\",\"pageIndex\":1,\"pageSize\":100}", pubts);

        try {
            log.info("请求 YonSuite 树版本 API: url={}, body={}", url, body);
            String resp = HttpRequest.post(url).header("Content-Type", "application/json")
                    .body(body).timeout(30_000).execute().body();
            log.info("YonSuite 树版本 API 原始响应: {}", resp);

            JsonNode root = objectMapper.readTree(resp);
            if (!"200".equals(root.get("code").asText())) {
                log.error("获取树版本失败: code={}, message={}", root.get("code").asText(), root.get("message").asText());
                throw new RuntimeException("获取树版本失败: " + root.get("message").asText());
            }

            List<YonOrgTreeSyncResponse.TreeVersion> versions = parseRecordList(root.path("data").path("recordList"),
                    node -> convertValue(node, YonOrgTreeSyncResponse.TreeVersion.class));
            log.info("获取到 {} 个树版本", versions.size());
            for (YonOrgTreeSyncResponse.TreeVersion v : versions) {
                log.info("  - 树版本: name={}, treeId={}, versionNo={}", v.getName(), v.getTreeId(), v.getVersionNo());
            }
            return versions;
        } catch (Exception e) {
            log.error("获取树版本失败", e);
            throw new RuntimeException("获取树版本失败: " + e.getMessage(), e);
        }
    }

    private List<YonOrgTreeSyncResponse.OrgRecord> getOrgMembersByTreeVids(List<String> treeVids, String pubts) {
        String token = yonAuthService.getAccessToken();
        String url = baseUrl + "/yonbip/digitalModel/openapi/treedatasync/treemembersync?access_token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        List<YonOrgTreeSyncResponse.OrgRecord> allRecords = new ArrayList<>();
        int page = 1;

        log.info("开始获取组织成员: treeVids={}, pubts={}", treeVids, pubts);

        while (true) {
            String treeVidsJson = toJsonArray(treeVids);
            String body = String.format("{\"pubts\":\"%s\",\"treeVids\":%s,\"pageIndex\":%d,\"pageSize\":100}",
                    pubts, treeVidsJson, page);

            try {
                log.info("请求 YonSuite 组织成员 API (page={}): body={}", page, body);
                String resp = HttpRequest.post(url).header("Content-Type", "application/json")
                        .body(body).timeout(30_000).execute().body();
                log.debug("YonSuite 组织成员 API 响应 (page={}): {}", page, resp);

                JsonNode root = objectMapper.readTree(resp);

                if (!"200".equals(root.get("code").asText())) {
                    log.error("获取组织成员失败: code={}, message={}", root.get("code").asText(), root.get("message").asText());
                    throw new RuntimeException("获取组织成员失败: " + root.get("message").asText());
                }

                JsonNode data = root.path("data");
                log.debug("响应数据: data={}, recordList={}", data.toString(), data.path("recordList").toString());

                if (data.isNull() || data.path("recordList").isNull() || data.path("recordList").isArray() && !data.path("recordList").elements().hasNext()) {
                    log.info("第 {} 页无数据，data={}, recordList={}", page, data.isNull(), data.path("recordList").isNull());
                    break;
                }

                List<YonOrgTreeSyncResponse.OrgRecord> pageRecords = parseRecordList(
                        data.path("recordList"), this::mapMemberRecord);
                allRecords.addAll(pageRecords);
                log.info("第 {} 页获取到 {} 条记录，累计 {} 条", page, pageRecords.size(), allRecords.size());

                if (pageRecords.isEmpty() || page >= data.path("pageCount").asInt(1)) {
                    break;
                }
                page++;
            } catch (Exception e) {
                log.error("获取组织成员失败: page={}", page, e);
                throw new RuntimeException("获取组织成员失败: " + e.getMessage(), e);
            }
        }

        log.info("YonSuite 组织成员获取完成，总计: {} 条", allRecords.size());
        return allRecords;
    }

    private <T> List<T> parseRecordList(JsonNode recordListNode, java.util.function.Function<JsonNode, T> mapper) {
        List<T> result = new ArrayList<>();
        if (recordListNode == null || !recordListNode.isArray()) {
            return result;
        }
        for (JsonNode node : recordListNode) {
            try {
                result.add(mapper.apply(node));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("解析记录失败: " + e.getMessage(), e);
            }
        }
        return result;
    }

    private String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(items.get(i)).append("\"");
        }
        return sb.append("]").toString();
    }

    private YonOrgTreeSyncResponse.OrgRecord mapMemberRecord(JsonNode node) {
        YonOrgTreeSyncResponse.OrgRecord record = new YonOrgTreeSyncResponse.OrgRecord();
        record.setId(node.path("id").asText(null));
        record.setCode(node.path("innercode").asText(null));
        record.setName(node.path("innercode").asText(null));
        record.setParentId(node.path("parentId").asText(null));
        // 映射组织类型（用于过滤部门数据）
        if (node.has("orgType")) {
            record.setOrgType(node.path("orgType").asText(null));
        } else if (node.has("org_type")) {
            record.setOrgType(node.path("org_type").asText(null));
        }
        if (node.has("level")) record.setLevel(node.get("level").asInt());
        if (node.has("displayorder")) record.setOrderNum(node.get("displayorder").asInt());
        if (node.has("enable")) record.setEnableStatus(node.get("enable").asInt());
        record.setCreationtime(node.path("creationtime").asText(null));
        record.setModifiedtime(node.path("modifiedtime").asText(null));
        record.setTenantId(node.path("tenantId").asText(null));
        return record;
    }

    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    /**
     * 安全地转换 JsonNode 为指定类型，处理 JsonProcessingException
     */
    private <T> T convertValue(JsonNode node, Class<T> clazz) {
        try {
            return objectMapper.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 转换失败: " + e.getMessage(), e);
        }
    }
}
