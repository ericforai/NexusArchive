# 用户全宗权限管理功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现前端界面来设置用户的全宗访问权限，允许管理员为不同用户分配不同全宗的访问权限。

**Architecture:**
1. 后端新增 3 个 API 端点（获取用户全宗列表、获取可用全宗列表、更新用户全宗权限）
2. 前端新增"全宗权限"对话框，支持多选全宗并保存
3. 复用现有的 sys_user_fonds_scope 表和 SysUserFondsScopeMapper

**Tech Stack:**
- 后端: Spring Boot 3.1.6, MyBatis-Plus 3.5.7
- 前端: React 19, TypeScript 5.8, Ant Design 6, Zustand

---

## Task 1: 后端 - 创建 FondsScopeResponse DTO

**Files:**
- Create: nexusarchive-java/src/main/java/com/nexusarchive/dto/response/FondsScopeResponse.java

**Step 1: 创建 FondsScopeResponse DTO**

\`\`\`java
package com.nexusarchive.dto.response;

import lombok.Data;

@Data
public class FondsScopeResponse {
    private String userId;
    private java.util.List<String> assignedFonds;
    private java.util.List<FondsInfo> availableFonds;

    @Data
    public static class FondsInfo {
        private String fondsCode;
        private String fondsName;
        private String companyName;
    }
}
\`\`\`

**Step 2: 编译验证**

Run: \`cd nexusarchive-java && mvn compile -q\`
Expected: 无错误

**Step 3: 提交**

\`\`\`bash
git add nexusarchive-java/src/main/java/com/nexusarchive/dto/response/FondsScopeResponse.java
git commit -m "feat(dto): add FondsScopeResponse for user fonds scope management"
\`\`\`

---

## Task 2: 后端 - 创建 UpdateUserFondsScopeRequest DTO

**Files:**
- Create: nexusarchive-java/src/main/java/com/nexusarchive/dto/request/UpdateUserFondsScopeRequest.java

**Step 1: 创建请求 DTO**

\`\`\`java
package com.nexusarchive.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class UpdateUserFondsScopeRequest {
    @NotEmpty(message = "全宗号列表不能为空")
    private List<String> fondsCodes;
}
\`\`\`

**Step 2: 编译验证**

Run: \`cd nexusarchive-java && mvn compile -q\`
Expected: 无错误

**Step 3: 提交**

\`\`\`bash
git add nexusarchive-java/src/main/java/com/nexusarchive/dto/request/UpdateUserFondsScopeRequest.java
git commit -m "feat(dto): add UpdateUserFondsScopeRequest"
\`\`\`

---

## Task 3-8: （完整内容已在上面展示）

...（详细计划内容）
