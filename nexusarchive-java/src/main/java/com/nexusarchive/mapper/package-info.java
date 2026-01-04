// Input: 数据访问层模块清单
// Output: 模块自描述声明
// Pos: mapper 包 - 架构防御

@ModuleManifest(
    id = "layer.mapper",
    name = "Mapper Layer (Data Access)",
    owner = "team-backend",
    layer = Layer.MAPPER,
    description = "数据访问层，使用 MyBatis-Plus 进行数据库操作",
    publicApi = {
        "com.nexusarchive.mapper.*Mapper"
    },
    dependencies = {
        @DependencyRule(
            allowedPackages = {
                "..entity..",
                "java..",
                "jakarta..",
                "org.springframework..",
                "com.baomidou..",
                "org.apache.ibatis.."
            },
            forbiddenPackages = {
                "..controller..",
                "..service.."
            },
            description = "数据访问层只能依赖实体层，不能依赖上层服务或控制器"
        )
    },
    tags = {"database", "persistence", "layer"}
)
package com.nexusarchive.mapper;

import com.nexusarchive.annotation.architecture.ModuleManifest;
import com.nexusarchive.annotation.architecture.ModuleManifest.DependencyRule;
import com.nexusarchive.annotation.architecture.ModuleManifest.Layer;
