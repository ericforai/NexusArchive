// Input: 服务层模块清单
// Output: 模块自描述声明
// Pos: service 包 - 架构防御

@ModuleManifest(
    id = "layer.service",
    name = "Service Layer",
    owner = "team-backend",
    layer = Layer.SERVICE,
    description = "业务逻辑层，实现核心业务规则和用例",
    publicApi = {
        "com.nexusarchive.service.*Service"
    },
    dependencies = {
        @DependencyRule(
            allowedPackages = {
                "..mapper..",
                "..entity..",
                "..dto..",
                "..common..",
                "..integration..",
                "java..",
                "jakarta..",
                "org.springframework..",
                "org.slf4j..",
                "lombok..",
                "cn.hutool.."
            },
            forbiddenPackages = {
                "..controller..",
                "..service.impl.."
            },
            description = "服务层可以依赖数据访问层和集成层，但不能依赖控制器层"
        )
    },
    tags = {"business", "layer"}
)
package com.nexusarchive.service;

import com.nexusarchive.annotation.architecture.ModuleManifest;
import com.nexusarchive.annotation.architecture.ModuleManifest.DependencyRule;
import com.nexusarchive.annotation.architecture.ModuleManifest.Layer;
