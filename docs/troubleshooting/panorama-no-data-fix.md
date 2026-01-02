# 全景视图看不到数据问题修复

## 问题状态

1. ✅ **数据库权限修复完成**
   - 已为6个用户添加了7个全宗号的权限（共42条记录）
   - AC01类别数据存在：48条数据分布在6个全宗号中

2. ✅ **FondsContextFilter修复完成**
   - 已修复多全宗权限用户的处理逻辑
   - 代码已修改并重新编译
   - 后端镜像已重新构建并启动

## 验证步骤

### 1. 确认登录正常
- 重新登录系统（清除浏览器缓存后）
- 确认不再出现闪退

### 2. 检查数据查询
全景视图查询条件：
- API: `GET /api/archives?page=1&limit=100&categoryCode=AC01`
- 数据过滤：基于用户的 `allowedFonds` 进行过滤

### 3. 浏览器控制台检查
打开浏览器开发者工具，查看：
- Network标签：检查 `/api/archives` 请求的响应
- Console标签：检查是否有JavaScript错误

### 4. 后端日志检查
```bash
docker logs nexus-backend-dev --tail 100 | grep -E "archives|fonds|403"
```

## 可能的问题

如果仍然看不到数据，可能原因：

1. **前端缓存问题**
   - 硬刷新浏览器（Ctrl+Shift+R 或 Cmd+Shift+R）
   - 清除浏览器缓存

2. **用户权限未生效**
   - 需要重新登录才能加载新的权限
   - 检查用户是否有对应的全宗权限

3. **数据过滤逻辑**
   - 确认数据查询时正确应用了 `applyArchiveScope`
   - 检查 `DataScopeContext` 是否正确解析

4. **前端请求头**
   - 确认前端是否正确发送 `X-Fonds-No` 请求头（如果有多全宗）

## 相关文件

- 权限修复脚本: `nexusarchive-java/src/main/resources/db/migration/V84__fix_user_fonds_scope_for_existing_data.sql`
- 过滤器修复: `nexusarchive-java/src/main/java/com/nexusarchive/config/FondsContextFilter.java`
- 数据服务: `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveService.java`

