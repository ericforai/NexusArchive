// Input: 云盾 SDK 调用参数
// Output: YundunSdkFacade 接口
// Pos: 云盾 SDK 适配层

package com.nexusarchive.integration.yundun.sdk;

public interface YundunSdkFacade {

    YundunSdkResult applyAppToken(String privateKey, String idpBaseUrl);
}
