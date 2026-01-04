// Input: 实体层模块清单
// Output: 模块自描述声明
// Pos: entity 包 - 架构防御

@ModuleManifest(
    id = "layer.entity",
    name = "Entity Layer (Domain Model)",
    owner = "team-backend",
    layer = Layer.ENTITY,
    description = "领域实体层，定义数据库表映射和领域模型",
    publicApi = {
        "com.nexusarchive.entity.*"
    },
    dependencies = {
        @DependencyRule(
            allowedPackages = {
                "java..",
                "jakarta..",
                "org.springframework..",
                "com.baomidou..",
                "lombok.."
            },
            forbiddenPackages = {
                "..controller..",
                "..service..",
                "..mapper.."
            },
            description = "实体层应该是独立的，不依赖上层业务逻辑"
        )
    },
    tags = {"domain", "model", "layer"}
)
package com.nexusarchive.entity;

import com.nexusarchive.annotation.architecture.ModuleManifest;
import com.nexusarchive.annotation.architecture.ModuleManifest.DependencyRule;
import com.nexusarchive.annotation.architecture.ModuleManifest.Layer;
