// Input: 云盾 SDK InvokeResult
// Output: YundunSdkFacade 接口
// Pos: 云盾 SDK 适配层

package com.nexusarchive.integration.yundun.sdk;

import com.dbappsecurity.aitrust.appSecSso.InvokeResult;

public interface YundunSdkFacade {

    InvokeResult applyAppToken(String privateKey, String idpBaseUrl);
}
