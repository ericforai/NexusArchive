// Input: 云盾 SDK 调用参数
// Output: DefaultYundunSdkFacade 默认实现
// Pos: 云盾 SDK 适配层

package com.nexusarchive.integration.yundun.sdk;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class DefaultYundunSdkFacade implements YundunSdkFacade {

    private static final String IDP_INVOKER_CLASS = "com.dbappsecurity.aitrust.appSecSso.IDPInvoker";

    @Override
    public YundunSdkResult applyAppToken(String privateKey, String idpBaseUrl) {
        try {
            Class<?> invokerClass = Class.forName(IDP_INVOKER_CLASS);
            configurePrivateKey(invokerClass, privateKey, idpBaseUrl);

            Object invokeResult = invokerClass.getMethod("applyAppToken").invoke(null);
            if (invokeResult == null) {
                return new YundunSdkResult(-1, "SDK 未返回结果", null);
            }

            return new YundunSdkResult(
                    readInt(invokeResult, "getCode"),
                    readString(invokeResult, "getMsg"),
                    readObject(invokeResult, "getContent"));
        } catch (ClassNotFoundException e) {
            return new YundunSdkResult(-1, "云盾 SDK 未安装", null);
        } catch (Exception e) {
            throw new IllegalStateException("调用云盾 SDK 失败", e);
        }
    }

    private void configurePrivateKey(Class<?> invokerClass, String privateKey, String idpBaseUrl) throws Exception {
        if (StringUtils.isBlank(idpBaseUrl)) {
            invokerClass.getMethod("setConfigPriKey", String.class).invoke(null, privateKey);
            return;
        }
        invokerClass.getMethod("setConfigPriKey", String.class, String.class).invoke(null, privateKey, idpBaseUrl);
    }

    private int readInt(Object target, String methodName) throws Exception {
        Object value = readObject(target, methodName);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? -1 : Integer.parseInt(String.valueOf(value));
    }

    private String readString(Object target, String methodName) throws Exception {
        Object value = readObject(target, methodName);
        return value == null ? "" : String.valueOf(value);
    }

    private Object readObject(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }
}
