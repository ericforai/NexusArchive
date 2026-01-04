// Input: 控制器层模块清单
// Output: 模块自描述声明
// Pos: controller 包 - 架构防御

@ModuleManifest(
    id = "layer.controller",
    name = "Controller Layer",
    owner = "team-backend",
    layer = Layer.CONTROLLER,
    description = "REST API 控制器层，处理 HTTP 请求和响应",
    publicApi = {
        "com.nexusarchive.controller.*Controller"
    },
    dependencies = {
        @DependencyRule(
            allowedPackages = {
                "..service..",
                "..dto..",
                "..common..",
                "java..",
                "jakarta..",
                "org.springframework..",
                "org.slf4j..",
                "lombok.."
            },
            forbiddenPackages = {
                "..mapper..",
                "..entity..",
                "..service.impl.."
            },
            description = "控制器只能依赖服务层和 DTO，不能直接依赖数据访问层或实体"
        )
    },
    tags = {"api", "rest", "layer"}
)
package com.nexusarchive.controller;

import com.nexusarchive.annotation.architecture.ModuleManifest;
import com.nexusarchive.annotation.architecture.ModuleManifest.DependencyRule;
import com.nexusarchive.annotation.architecture.ModuleManifest.Layer;
