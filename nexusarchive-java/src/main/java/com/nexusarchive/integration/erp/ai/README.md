# ERP AI 自适应模块

本模块负责 AI 驱动的 ERP 接口自动适配。

## 子模块

- `agent/` - Agent 编排和协调
- `parser/` - 接口文档解析器
- `generator/` - 代码生成器
- `mapper/` - 业务语义映射器
- `config/` - AI 配置和提示词模板

## MVP 范围

- 支持 OpenAPI JSON 格式
- 处理记账凭证同步场景
- 生成适配器 Java 代码

## 架构

```
用户上传 OpenAPI 文件
    ↓
Parser 解析文档
    ↓
Mapper 映射业务语义
    ↓
Generator 生成代码
    ↓
Agent 编排协调
    ↓
输出适配器代码
```
