// Input: 云盾 SDK IDPInvoker
// Output: DefaultYundunSdkFacade 默认实现
// Pos: 云盾 SDK 适配层

package com.nexusarchive.integration.yundun.sdk;

import com.dbappsecurity.aitrust.appSecSso.IDPInvoker;
import com.dbappsecurity.aitrust.appSecSso.InvokeResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class DefaultYundunSdkFacade implements YundunSdkFacade {

    @Override
    public InvokeResult applyAppToken(String privateKey, String idpBaseUrl) {
        if (StringUtils.isBlank(idpBaseUrl)) {
            IDPInvoker.setConfigPriKey(privateKey);
        } else {
            IDPInvoker.setConfigPriKey(privateKey, idpBaseUrl);
        }
        return IDPInvoker.applyAppToken();
    }
}
