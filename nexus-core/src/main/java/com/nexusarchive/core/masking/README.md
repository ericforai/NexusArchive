一旦我所属的文件夹有所变化，请更新我。

# 数据脱敏模块 (Masking)

## 目录说明

提供基于规则的动态数据脱敏能力，配合 AOP 实现查询结果的自动脱敏。

## 文件清单

| 文件名 | 类型 | 描述 |
| :--- | :--- | :--- |
| `DataMaskingProperties.java` | 配置 | 脱敏规则配置 (字段匹配、模式) |
| `DataMaskingService.java` | 接口 | 脱敏服务接口 |
| `DefaultDataMaskingService.java` | 服务 | 基于反射与规则的脱敏实现 |
| `Masked.java` | 注解 | 标记需要脱敏的方法 |
| `MaskingAspect.java` | 切面 | AOP 拦截并处理返回值 |
