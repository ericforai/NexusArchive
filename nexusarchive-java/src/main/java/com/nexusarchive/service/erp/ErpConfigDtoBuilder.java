// Input: ErpConfig Entity, Spring Framework, Hutool JSON
// Output: ErpConfigDtoBuilder
// Pos: Service Layer
// 负责将 ErpConfig 实体转换为 DTO

package com.nexusarchive.service.erp;

import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.util.SM4Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP 配置 DTO 构建器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>将 ErpConfig 实体转换为集成层使用的 DTO</li>
 *   <li>解析 configJson 字段</li>
 *   <li>解密敏感信息（appSecret）</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ErpConfigDtoBuilder {

    private static final String FIELD_BASE_URL = "baseUrl";
    private static final String FIELD_APP_KEY = "appKey";
    private static final String FIELD_CLIENT_ID = "clientId";
    private static final String FIELD_APP_SECRET = "appSecret";
    private static final String FIELD_CLIENT_SECRET = "clientSecret";
    private static final String FIELD_ACCBOOK_CODE = "accbookCode";
    private static final String FIELD_ACCBOOK_CODES = "accbookCodes";

    /**
     * 构建 ERP 配置 DTO
     *
     * @param entityConfig 数据库实体
     * @return 集成层使用的 DTO
     */
    public com.nexusarchive.integration.erp.dto.ErpConfig buildDtoConfig(ErpConfig entityConfig) {
        com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig =
            new com.nexusarchive.integration.erp.dto.ErpConfig();

        dtoConfig.setId(String.valueOf(entityConfig.getId()));
        dtoConfig.setName(entityConfig.getName());
        dtoConfig.setAdapterType(entityConfig.getErpType());

        if (entityConfig.getConfigJson() != null) {
            parseConfigJson(dtoConfig, entityConfig.getConfigJson());
        }

        return dtoConfig;
    }

    /**
     * 解析 configJson 并填充 DTO
     */
    private void parseConfigJson(com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig,
                                 String configJson) {
        cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(configJson);

        dtoConfig.setBaseUrl(json.getStr(FIELD_BASE_URL));

        // 支持多种 appKey 字段名
        String appKey = json.getStr(FIELD_APP_KEY);
        if (appKey == null || appKey.isEmpty()) {
            appKey = json.getStr(FIELD_CLIENT_ID);
        }
        dtoConfig.setAppKey(appKey);

        // 支持多种 appSecret 字段名，并解密
        String appSecret = json.getStr(FIELD_APP_SECRET);
        if (appSecret == null || appSecret.isEmpty()) {
            appSecret = json.getStr(FIELD_CLIENT_SECRET);
        }
        dtoConfig.setAppSecret(SM4Utils.decrypt(appSecret));

        dtoConfig.setAccbookCode(json.getStr(FIELD_ACCBOOK_CODE));

        // 解析多组织代码列表
        parseAccbookCodes(dtoConfig, json);

        dtoConfig.setExtraConfig(configJson);
    }

    /**
     * 解析账套代码列表
     */
    private void parseAccbookCodes(com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig,
                                   cn.hutool.json.JSONObject json) {
        cn.hutool.json.JSONArray accbookCodesArray = json.getJSONArray(FIELD_ACCBOOK_CODES);
        if (accbookCodesArray != null && !accbookCodesArray.isEmpty()) {
            List<String> codes = new ArrayList<>();
            for (int i = 0; i < accbookCodesArray.size(); i++) {
                codes.add(accbookCodesArray.getStr(i));
            }
            dtoConfig.setAccbookCodes(codes);
        }
    }
}
