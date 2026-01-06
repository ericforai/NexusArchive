# 个人资料功能设计文档

**日期**: 2025-01-06
**功能**: 用户个人资料展示（只读）
**状态**: 待实施

## 1. 功能概述

纯展示型个人资料功能，用户点击 Header 头像区域打开右侧抽屉，查看个人信息并执行退出登录。

## 2. UI 设计

### 2.1 交互流程
1. 用户点击 Header 右上角头像区域
2. 右侧滑出抽屉（宽度 400px）
3. 显示用户信息卡片
4. 点击"退出登录"按钮退出系统

### 2.2 布局结构

```
┌─────────────────────────────┐
│  [关闭]       个人资料        │  ← 标题栏
├─────────────────────────────┤
│         [头像 80x80]         │
│       张三                   │  ← 姓名
│     系统管理员 · EMP001      │  ← 角色 + 工号
├─────────────────────────────┤
│  姓名    张三                │
│  工号    EMP001              │  ← 信息列表
│  邮箱    zhang@company.com   │
│  手机    138****1234         │
├─────────────────────────────┤
│    [退出登录]                │  ← 红色按钮
└─────────────────────────────┘
```

### 2.3 样式规范
- 使用 Ant Design `Drawer` 组件
- 头像圆角，尺寸 80x80
- 信息采用键值对布局（左侧灰色标签，右侧深色值）
- 退出按钮使用 `danger` 属性

## 3. 数据字段

### 3.1 展示字段
| 位置 | 字段 | 来源 |
|------|------|------|
| 头像区域 | avatar | User.avatar |
| 头像区域 | fullName | User.fullName |
| 头像区域 | 主角色 | roleNames[0] |
| 头像区域 | 工号 | employeeId |
| 信息列表 | 姓名 | fullName |
| 信息列表 | 工号 | employeeId |
| 信息列表 | 邮箱 | email |
| 信息列表 | 手机 | phone |

### 3.2 后端返回字段（GET /auth/me）

| 分类 | 字段 | 类型 | 说明 |
|------|------|------|------|
| 现有 | id | String | 用户ID |
| 现有 | username | String | 登录账号 |
| 现有 | fullName | String | 真实姓名（DA/T 94 M84） |
| 现有 | email | String | 邮箱 |
| 现有 | avatar | String | 头像URL |
| 现有 | departmentId | String | 部门ID |
| 现有 | status | String | 账号状态 |
| 现有 | roles | List\<String\> | 角色代码列表 |
| 现有 | permissions | List\<String\> | 权限列表 |
| **新增** | phone | String | 手机号 |
| **新增** | employeeId | String | 工号 |
| **新增** | jobTitle | String | 职位 |
| **新增** | orgCode | String | 组织机构代码（DA/T 94 M85） |
| **新增** | lastLoginAt | LocalDateTime | 最后登录时间 |
| **新增** | createdTime | LocalDateTime | 创建时间 |
| **新增** | roleNames | List\<String\> | 角色名称列表 |

## 4. 后端改动

### 4.1 LoginResponse.UserInfo 新增字段

文件: `nexusarchive-java/src/main/java/com/nexusarchive/dto/response/LoginResponse.java`

```java
public static class UserInfo {
    // ... 现有字段

    // 新增字段
    private String phone;
    private String employeeId;
    private String jobTitle;
    private String orgCode;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdTime;
    private List<String> roleNames;
}
```

### 4.2 AuthService.buildUserInfo() 修改

文件: `nexusarchive-java/src/main/java/com/nexusarchive/service/AuthService.java`

```java
private LoginResponse.UserInfo buildUserInfo(User user) {
    List<Role> roles = roleMapper.findByUserId(user.getId());
    List<String> roleCodes = new ArrayList<>();
    List<String> roleNames = new ArrayList<>();  // 新增
    Set<String> permissions = new HashSet<>();

    for (Role role : roles) {
        roleCodes.add(role.getCode());
        roleNames.add(role.getName());  // 新增
        // ... 权限处理
    }

    LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
    // ... 现有字段设置

    // 新增字段设置
    userInfo.setPhone(user.getPhone());
    userInfo.setEmployeeId(user.getEmployeeId());
    userInfo.setJobTitle(user.getJobTitle());
    userInfo.setOrgCode(user.getOrgCode());
    userInfo.setLastLoginAt(user.getLastLoginAt());
    userInfo.setCreatedTime(user.getCreatedTime());
    userInfo.setRoleNames(roleNames);

    return userInfo;
}
```

## 5. 前端改动

### 5.1 新增组件

文件: `src/components/layout/ProfileDrawer.tsx`

```tsx
import React from 'react';
import { Drawer, Avatar, Button } from 'antd';
import { useAuthStore } from '@/store';
import { authApi } from '@/api/auth';
import { useNavigate } from 'react-router-dom';

interface ProfileDrawerProps {
    open: boolean;
    onClose: () => void;
}

const InfoRow: React.FC<{ label: string; value?: string }> = ({ label, value }) => (
    <div className="flex">
        <span className="w-16 text-slate-500">{label}</span>
        <span className="flex-1 text-slate-800">{value || '-'}</span>
    </div>
);

export const ProfileDrawer: React.FC<ProfileDrawerProps> = ({ open, onClose }) => {
    const { user } = useAuthStore();
    const navigate = useNavigate();
    const mainRole = user?.roleNames?.[0] || '-';

    const handleLogout = async () => {
        try {
            await authApi.logout();
        } catch (error) {
            console.error('登出失败:', error);
        } finally {
            useAuthStore.getState().logout();
            navigate('/login');
        }
    };

    return (
        <Drawer open={open} onClose={onClose} placement="right" width={400}>
            <div className="flex flex-col h-full">
                {/* 标题栏 */}
                <div className="flex items-center justify-between px-6 py-4 border-b">
                    <h2 className="text-lg font-semibold">个人资料</h2>
                    <Button type="text" onClick={onClose}>✕</Button>
                </div>

                {/* 头像区域 */}
                <div className="flex flex-col items-center py-8 border-b">
                    <Avatar size={80} src={user?.avatar}>{user?.fullName?.[0]}</Avatar>
                    <div className="mt-3 text-lg font-medium">{user?.fullName}</div>
                    <div className="text-sm text-slate-500">
                        {mainRole} · {user?.employeeId || '-'}
                    </div>
                </div>

                {/* 信息列表 */}
                <div className="flex-1 px-6 py-4 space-y-4">
                    <InfoRow label="姓名" value={user?.fullName} />
                    <InfoRow label="工号" value={user?.employeeId} />
                    <InfoRow label="邮箱" value={user?.email} />
                    <InfoRow label="手机" value={user?.phone} />
                </div>

                {/* 底部按钮 */}
                <div className="p-6 border-t">
                    <Button danger block onClick={handleLogout}>
                        退出登录
                    </Button>
                </div>
            </div>
        </Drawer>
    );
};
```

### 5.2 Header 集成

文件: `src/components/layout/Header.tsx`

```tsx
import { useState } from 'react';
import { ProfileDrawer } from './ProfileDrawer';

export const Header = () => {
    const [profileOpen, setProfileOpen] = useState(false);

    return (
        <>
            {/* 现有 Header 内容 */}
            {/* 头像区域添加 onClick */}
            <div onClick={() => setProfileOpen(true)} className="cursor-pointer">
                {/* 现有头像、角色、部门显示 */}
            </div>

            <ProfileDrawer open={profileOpen} onClose={() => setProfileOpen(false)} />
        </>
    );
};
```

### 5.3 类型更新

文件: `src/api/auth.ts`

```typescript
export interface UserInfo {
    id: string;
    username: string;
    fullName?: string;
    email?: string;
    avatar?: string;
    departmentId?: string;
    status?: string;
    roles: string[];
    permissions: string[];
    // 新增字段
    phone?: string;
    employeeId?: string;
    jobTitle?: string;
    orgCode?: string;
    lastLoginAt?: string;
    createdTime?: string;
    roleNames?: string[];
}
```

## 6. 边界情况处理

| 场景 | 处理 |
|------|------|
| user 为 null | 显示占位符，字段显示 "-" |
| 字段为空/undefined | 显示 "-" |
| 头像加载失败 | 显示姓名首字 |
| roleNames 为空 | 主角色显示 "-" |
| 退出登录失败 | 强制清除本地状态后跳转登录页 |

## 7. 改动文件清单

### 后端
- `nexusarchive-java/src/main/java/com/nexusarchive/dto/response/LoginResponse.java`
- `nexusarchive-java/src/main/java/com/nexusarchive/service/AuthService.java`

### 前端
- `src/components/layout/ProfileDrawer.tsx`（新建）
- `src/components/layout/Header.tsx`（修改）
- `src/api/auth.ts`（类型更新）
- `src/store/useAuthStore.ts`（类型可能需要同步）
