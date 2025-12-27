## Bug 修复报告

### 1. Bug 类型判定
> 逻辑错误 (JSX 语法错误)。在之前的 UX 优化重构中，由于合并冲突或手误，导致在 `Header` 区域多出了一个闭合标签 `</div>`。

### 2. 根因分析（因果链）
> 1. `ArchiveListView.tsx` 第 1638 行存在多余的 `</div>`。
> 2. 该标签在 Babel 解析时被视为闭合了整个页面的主容器 `div`。
> 3. 后续的 `{subTitle === '凭证关联' && ...}` 及其子元素因此暴露在顶级作用域，超出了单根 JSX 元素的限制，导致报错 `Unexpected token, expected ","`。

### 3. 修复思路说明
> 精准删除 1638 行的多余闭合标签，使 JSX 树恢复正确的嵌套层级。

### 4. 修改后的代码
```diff
-    </div>
-
-      {
-    subTitle === '凭证关联' && activeLinkTab === 'report' ? (
+    </div>
+
+      {subTitle === '凭证关联' && activeLinkTab === 'report' ? (
```

### 5. 潜在副作用与建议测试
> 该修改仅恢复页面结构，理论上无副作用。建议刷新页面确认凭证列表和合规报告 Tab 切换正常。

---

### 6. 相关故障排除文档
- [开发环境问题排查](file:///Users/user/nexusarchive/docs/troubleshooting/开发环境问题排查.md)
> [!NOTE]
> 该 Bug 表现为 JSX 编译错误，属于典型的开发环境构建问题，故链接至此进行归档。
