// Input: 架构防御模块清单
// Output: 批量上传模块清单
// Pos: 领域层 - 模块自描述

/**
 * 批量上传 (Collection Batch) 模块清单
 *
 * J1 Self-Description: 模块自我声明边界
 *
 * 此模块负责电子会计档案的批量收集上传功能，符合 GB/T 39362-2020 标准
 */
export const moduleManifest = {
  // 模块身份
  id: "nexusarchive.collection-batch",
  owner: "team-backend",
  version: "1.0.0",
  created: "2025-01-05",

  // 公共API入口
  publicApi: [
    "com.nexusarchive.controller.CollectionBatchController",  // REST API
    "com.nexusarchive.service.CollectionBatchService",        // 服务接口
    "com.nexusarchive.dto.BatchUploadRequest",               // 请求DTO
    "com.nexusarchive.dto.BatchUploadResponse",              // 响应DTO
    "com.nexusarchive.entity.CollectionBatch",               // 实体 (只读)
    "com.nexusarchive.entity.CollectionBatchFile",           // 实体 (只读)
  ],

  // 可导入的模块白名单
  canImportFrom: [
    "java.util.*",              // JDK 标准库
    "java.time.*",             // JDK 时间库
    "java.lang.*",             // JDK 基础库
    "org.springframework.*",    // Spring 框架
    "org.springframework.web.*",
    "org.springframework.transaction.*",
    "jakarta.*",               // Jakarta EE (验证)
    "lombok.*",                // Lombok
    "com.baomidou.*",          // MyBatis-Plus
    "org.apache.ibatis.*",      // MyBatis
    "org.slf4j.*",              // 日志
    "com.nexusarchive.common.*",         // 公共模块
    "com.nexusarchive.config.*",          // 配置
    "com.nexusarchive.security.*",       // 安全
    "com.nexusarchive.service.PoolService",               // 池服务
    "com.nexusarchive.service.PreArchiveCheckService",   // 预归档检测
    "com.nexusarchive.service.AuditLogService",           // 审计日志
    "com.nexusarchive.entity.ArcFileContent",            // 文件内容实体
    "com.nexusarchive.util.FileHashUtil",                 // 文件哈希工具
  ],

  // 禁止导入的模块
  cannotImportFrom: [
    "com.nexusarchive.controller.*",  // 不能依赖其他控制器
  ],

  // 架构约束
  restrictions: {
    // 控制器只能依赖服务接口，不能依赖实现
    controllerMustOnlyUseServiceInterface: true,

    // 服务实现不能被外部直接依赖
    serviceImplIsPrivate: true,

    // Mapper 只能被服务层访问
    mapperIsPrivateToService: true,

    // DTO 只能被控制器和服务层使用
    dtoOnlyInControllerOrService: true,

    // 实体可以被跨层使用（只读）
    entityIsReadOnly: true,
  },

  // 分层规则
  layers: {
    controller: ["com.nexusarchive.controller.CollectionBatchController"],
    service: ["com.nexusarchive.service.CollectionBatchService"],
    serviceImpl: ["com.nexusarchive.service.impl.CollectionBatchServiceImpl"],
    mapper: [
      "com.nexusarchive.mapper.CollectionBatchMapper",
      "com.nexusarchive.mapper.CollectionBatchFileMapper",
    ],
    entity: [
      "com.nexusarchive.entity.CollectionBatch",
      "com.nexusarchive.entity.CollectionBatchFile",
    ],
    dto: [
      "com.nexusarchive.dto.BatchUploadRequest",
      "com.nexusarchive.dto.BatchUploadResponse",
    ],
  },

  // 依赖流规则: Controller → Service → Mapper
  dependencyFlow: [
    "controller -> service",
    "controller -> dto",
    "service -> entity",
    "serviceImpl -> service",
    "serviceImpl -> mapper",
    "serviceImpl -> entity",
    "serviceImpl -> util",
    "mapper -> entity",
  ],

  // 标签
  tags: ["batch-upload", "collection", "gb-t-39362-2020"],

  // 合规性声明
  compliance: {
    standard: "GB/T 39362-2020",
    fourNatureCheck: true,  // 支持四性检测
    auditLogging: true,      // 支持审计日志
    idempotency: true,       // 支持幂等性控制
  },
};

// TypeScript 语法在 Java 项目中用于文档化
// 实际验证通过 ArchUnit 测试实现
