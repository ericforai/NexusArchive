// Input: 批次档案转换服务模块清单
// Output: J1 Self-Description - 模块声明边界和所有者
// Pos: service.impl.batch 包 - 架构防御
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 批次档案转换服务包
 * <p>
 * 负责批量上传文件到档案记录的转换，符合 DA/T 94-2022 元数据同步捕获要求。
 * </p>
 */
@ModuleManifest(
    id = "service.batch.archive",
    name = "Batch to Archive Service",
    owner = "team-backend",
    layer = Layer.SERVICE,
    description = "批量上传文件转换为档案记录，符合 DA/T 94-2022 元数据同步捕获要求",
    publicApi = {
        "com.nexusarchive.service.BatchToArchiveService",
        "com.nexusarchive.service.impl.batch.BatchManager",
        "com.nexusarchive.service.impl.batch.BatchWorkflowService"
    },
    dependencies = {
        @DependencyRule(
            allowedPackages = {
                "..mapper..",
                "..entity..",
                "..dto..",
                "..common..",
                "..config..",
                "..util..",
                "java..",
                "jakarta..",
                "org.springframework..",
                "org.mybatis..",
                "com.baomidou..",
                "lombok..",
                "org.slf4j..",
                "cn.hutool.."
            },
            forbiddenPackages = {
                "..controller..",
                "..service..impl.."
            },
            description = "批次档案服务只能依赖数据访问层和实体层，不能依赖控制器层"
        )
    },
    tags = {"batch", "archive", "dat94-2022"}
)
package com.nexusarchive.service.impl.batch;

import com.nexusarchive.annotation.architecture.ModuleManifest;
import com.nexusarchive.annotation.architecture.ModuleManifest.DependencyRule;
import com.nexusarchive.annotation.architecture.ModuleManifest.Layer;
