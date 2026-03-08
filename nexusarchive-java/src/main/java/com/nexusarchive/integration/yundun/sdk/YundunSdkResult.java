// Input: 云盾 SDK 响应字段
// Output: YundunSdkResult 统一结果模型
// Pos: 云盾 SDK 适配层

package com.nexusarchive.integration.yundun.sdk;

public record YundunSdkResult(int code, String message, Object content) {
}
