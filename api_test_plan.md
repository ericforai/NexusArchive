# API 测试计划

## 1. 登录接口
### 请求
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
### 响应示例
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": "user_admin",
      "username": "admin",
      "fullName": "系统管理员",
      "email": null,
      "avatar": null,
      "departmentId": null
    }
  },
  "timestamp": 1763721298825
}
```

## 2. 获取当前用户信息
### 请求
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```
### 响应示例
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "id": "user_admin",
    "username": "admin",
    "fullName": "系统管理员",
    "email": null,
    "avatar": null,
    "departmentId": null,
    "roleIds": ["role_system_admin"]
  },
  "timestamp": 1763721300000
}
```

## 3. 用户管理接口（管理员）
### 3.1 创建用户
```bash
curl -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "username": "john",
    "password": "Password123!",
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "departmentId": "dept_01",
    "roleIds": ["role_business_user"]
}'
```
### 响应示例
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": "user_abc123",
    "username": "john",
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "avatar": null,
    "departmentId": "dept_01",
    "status": "active",
    "roleIds": ["role_business_user"]
  },
  "timestamp": 1763721310000
}
```

### 3.2 更新用户
```bash
curl -X PUT http://localhost:8080/api/admin/users/{id} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "fullName": "John Updated",
    "email": "john.updated@example.com",
    "phone": "0987654321",
    "departmentId": "dept_02",
    "roleIds": ["role_business_user", "role_audit_admin"]
}'
```
### 响应示例
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": "user_abc123",
    "username": "john",
    "fullName": "John Updated",
    "email": "john.updated@example.com",
    "phone": "0987654321",
    "avatar": null,
    "departmentId": "dept_02",
    "status": "active",
    "roleIds": ["role_business_user", "role_audit_admin"]
  },
  "timestamp": 1763721320000
}
```

### 3.3 删除用户
```bash
curl -X DELETE http://localhost:8080/api/admin/users/{id} \
  -H "Authorization: Bearer <TOKEN>"
```
### 响应示例
```json
{
  "code": 204,
  "message": "删除成功",
  "data": null,
  "timestamp": 1763721330000
}
```

### 3.4 查询用户列表
```bash
curl -X GET "http://localhost:8080/api/admin/users?page=1&size=20" \
  -H "Authorization: Bearer <TOKEN>"
```
### 响应示例
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": "user_admin",
      "username": "admin",
      "fullName": "系统管理员",
      "email": null,
      "phone": null,
      "avatar": null,
      "departmentId": null,
      "status": "active",
      "roleIds": ["role_system_admin"]
    },
    {
      "id": "user_abc123",
      "username": "john",
      "fullName": "John Updated",
      "email": "john.updated@example.com",
      "phone": "0987654321",
      "avatar": null,
      "departmentId": "dept_02",
      "status": "active",
      "roleIds": ["role_business_user", "role_audit_admin"]
    }
  ],
  "timestamp": 1763721340000
}
```

## 4. 角色管理接口（管理员）
### 4.1 创建角色
```bash
curl -X POST http://localhost:8080/api/admin/roles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "审计员",
    "code": "role_audit_admin",
    "roleCategory": "AUDIT_ADMIN",
    "description": "负责审计日志检查",
    "permissions": "[\"audit_logs\", \"view_logs\"]"
}'
```
### 响应示例
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": "role_audit_admin",
    "name": "审计员",
    "code": "role_audit_admin",
    "roleCategory": "AUDIT_ADMIN",
    "isExclusive": true,
    "description": "负责审计日志检查",
    "permissions": "[\"audit_logs\", \"view_logs\"]",
    "dataScope": "self",
    "type": "custom"
  },
  "timestamp": 1763721350000
}
```

### 4.2 更新角色
```bash
curl -X PUT http://localhost:8080/api/admin/roles/{id} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "审计员（更新）",
    "description": "更新后的描述",
    "permissions": "[\"audit_logs\", \"export_logs\"]"
}'
```
### 响应示例
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null,
  "timestamp": 1763721360000
}
```

### 4.3 删除角色
```bash
curl -X DELETE http://localhost:8080/api/admin/roles/{id} \
  -H "Authorization: Bearer <TOKEN>"
```
### 响应示例
```json
{
  "code": 204,
  "message": "删除成功",
  "data": null,
  "timestamp": 1763721370000
}
```

### 4.4 查询角色列表
```bash
curl -X GET "http://localhost:8080/api/admin/roles?page=1&size=20" \
  -H "Authorization: Bearer <TOKEN>"
```
### 响应示例
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": "role_system_admin",
      "name": "系统管理员",
      "code": "role_system_admin",
      "roleCategory": "SYSTEM_ADMIN",
      "isExclusive": true,
      "description": "系统最高权限",
      "permissions": "[\"manage_users\", \"manage_roles\"]",
      "dataScope": "all",
      "type": "system"
    },
    {
      "id": "role_audit_admin",
      "name": "审计员",
      "code": "role_audit_admin",
      "roleCategory": "AUDIT_ADMIN",
      "isExclusive": true,
      "description": "负责审计日志检查",
      "permissions": "[\"audit_logs\", \"view_logs\"]",
      "dataScope": "self",
      "type": "custom"
    }
  ],
  "timestamp": 1763721380000
}
```

## 5. 错误码说明
| 状态码 | 含义 | 说明 |
|--------|------|------|
| 200 | 成功 | 请求成功并返回数据 |
| 201 | 已创建 | POST 创建成功返回的资源 ID |
| 204 | 无内容 | DELETE 成功，无返回体 |
| 400 | 参数错误 | 请求参数缺失或格式错误 |
| 401 | 未授权 | JWT 缺失或无效 |
| 403 | 禁止访问 | 权限不足，三员互斥校验失败 |
| 404 | 未找到 | 资源不存在 |
| 500 | 服务器错误 | 未捕获的异常 |

---
**使用说明**：
1. 将 `<TOKEN>` 替换为登录接口返回的 JWT。 
2. 根据实际需求调整分页参数 `page` 与 `size`。 
3. 所有请求均需在 `Authorization` 头中携带 `Bearer <TOKEN>`（除登录接口外）。

祝您测试顺利！
