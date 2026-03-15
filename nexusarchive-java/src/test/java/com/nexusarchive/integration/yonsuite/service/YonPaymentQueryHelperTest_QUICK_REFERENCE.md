# YonPaymentQueryHelperTest 快速参考

## 测试方法索引

### queryPaymentApplyIdsParallel 测试
| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `testQueryPaymentApplyIdsParallel_NullConfig` | 配置为 null | 返回空列表 |
| `testQueryPaymentApplyIdsParallel_NullBaseUrl` | baseUrl 为 null | 返回空列表 |
| `testQueryPaymentApplyIdsParallel_EmptyBaseUrl` | baseUrl 为空字符串 | 返回空列表 |
| `testQueryPaymentApplyIdsParallel_SinglePage_Success` | 单页查询成功 | 返回 ID 列表 |
| `testQueryPaymentApplyIdsParallel_MultiplePages_ParallelQuery` | 多页并行查询 | 返回合并后的 ID 列表 |
| `testQueryPaymentApplyIdsParallel_FirstPageFailed` | 第一页查询失败 | 返回空列表 |
| `testQueryPaymentApplyIdsParallel_InvalidResponse` | 无效 JSON | 返回空列表 |
| `testQueryPaymentApplyIdsParallel_NullResponseData` | 响应数据为 null | 返回空列表 |
| `testQueryPaymentApplyIdsParallel_EmptyRecordList` | 记录列表为空 | 返回空列表 |
| `testQueryPaymentApplyIdsParallel_ResponseWithNullIds` | 响应包含 null ID | 过滤 null ID |
| `testQueryPaymentApplyIdsParallel_MaxPageLimit` | 超过 100 页 | 只查询前 100 页 |

### queryPaymentApplyListParallel 测试
| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `testQueryPaymentApplyListParallel_NullConfig` | 配置为 null | 返回空列表 |
| `testQueryPaymentApplyListParallel_SinglePage_Success` | 单页查询成功 | 返回记录列表 |
| `testQueryPaymentApplyListParallel_MultiplePages_ParallelQuery` | 多页并行查询 | 返回合并后的记录列表 |
| `testQueryPaymentApplyListParallel_FirstPageFailed` | 第一页查询失败 | 返回空列表 |
| `testQueryPaymentApplyListParallel_InvalidResponse` | 无效 JSON | 返回空列表 |
| `testQueryPaymentApplyListParallel_NullStartDate` | 开始日期为 null | 仍然发送请求 |
| `testQueryPaymentApplyListParallel_NullEndDate` | 结束日期为 null | 仍然发送请求 |

### 异常处理测试
| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `testQueryPaymentApplyIdsParallel_ExceptionHandling` | 抛出异常 | 返回空列表 |
| `testQueryPaymentApplyListParallel_ExceptionHandling` | 抛出异常 | 返回空列表 |

## Mock 配置模式

### 静态 Mock HttpRequest
```java
try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
    HttpRequest request = mock(HttpRequest.class);
    HttpResponse response = mock(HttpResponse.class);

    mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
    when(request.header(any(), any())).thenReturn(request);
    when(request.body(anyString())).thenReturn(request);
    when(request.timeout(anyInt())).thenReturn(request);
    when(request.execute()).thenReturn(response);

    // 配置响应
    when(response.isOk()).thenReturn(true);
    when(response.body()).thenReturn(createSinglePageJsonResponse());

    // 执行测试
    // ...
}
```

### Mock YonAuthService
```java
when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
    .thenReturn("test-access-token");
```

### Mock ApplicationContext
```java
Executor mockExecutor = mock(Executor.class);
when(applicationContext.containsBean("erpSyncExecutor")).thenReturn(true);
when(applicationContext.getBean("erpSyncExecutor")).thenReturn(mockExecutor);
```

## 测试数据构建方法

### createSinglePageJsonResponse()
返回包含 2 条记录的单页响应 JSON

### createMultiPageJsonResponse(int currentPage, int totalPages)
返回指定总页数的多页响应 JSON

### createSingleRecordPageJsonResponse(int pageIndex)
返回包含 1 条记录的指定页响应 JSON

### createNullDataResponse()
返回 data 为 null 的响应 JSON

### createEmptyRecordListResponse()
返回 recordList 为空的响应 JSON

### createResponseWithNullIds()
返回 ID 字段为 null 的响应 JSON

## 测试断言模式

### 验证返回值
```java
assertNotNull(result);
assertEquals(expectedSize, result.size());
assertTrue(result.contains("expected-id"));
```

### 验证方法调用
```java
verify(yonAuthService, never()).getAccessToken(any(), any());
verify(request, atLeastOnce()).execute();
verify(request, times(100)).execute();
```

### 验证异常处理
```java
when(service.method()).thenThrow(new RuntimeException("error"));
// 执行后验证返回空列表而非抛出异常
assertNotNull(result);
assertTrue(result.isEmpty());
```

## 运行命令

```bash
# 编译测试
mvn test-compile

# 运行单个测试类
mvn surefire:test -Dtest=YonPaymentQueryHelperTest

# 运行单个测试方法
mvn surefire:test -Dtest=YonPaymentQueryHelperTest#testQueryPaymentApplyIdsParallel_SinglePage_Success

# 生成覆盖率报告
mvn jacoco:report
```

## 测试覆盖率检查点

- [ ] 所有 public 方法都有测试
- [ ] 所有分支条件都有测试 (if/else、三元运算符)
- [ ] 所有异常路径都有测试
- [ ] 所有边界条件都有测试 (null、empty、max)
- [ ] 所有 Mock 都正确配置
- [ ] 所有断言都有意义

## 常见问题排查

### 问题: Mock 静态方法不生效
**解决**: 确保使用 try-with-resources 模式:
```java
try (MockedStatic<HttpRequest> mocked = mockStatic(HttpRequest.class)) {
    // 测试代码
}
```

### 问题: 测试找不到
**解决**: 确保测试类命名规范:
- 测试类名: `XxxTest`
- 测试方法名: `testXxx`

### 问题: Mockito 版本冲突
**解决**: 确认 pom.xml 中的 Mockito 版本:
```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
```

---
**更新时间**: 2026-03-15
