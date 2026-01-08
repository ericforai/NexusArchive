#!/usr/bin/env bash
# Input: 模块名称 (如 borrowing, voucher)
# Output: 标准四层 DDD 模块目录结构
# Pos: scripts/ - 开发工具脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

set -euo pipefail

# 颜色定义
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[0;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# 项目路径
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
readonly MODULES_DIR="$PROJECT_ROOT/nexusarchive-java/src/main/java/com/nexusarchive/modules"
readonly TEMPLATE_DIR="$MODULES_DIR/_template"

# 日志函数
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# 显示使用说明
show_usage() {
    cat << EOF
${BLUE}用法:${NC}
    $(basename "$0") <ModuleName>

${BLUE}示例:${NC}
    $(basename "$0") borrowing
    $(basename "$0") voucher
    $(basename "$0") payment

${BLUE}说明:${NC}
    生成标准的 DDD 四层模块结构 (api/app/domain/infra)
    模块名使用 PascalCase，会自动转换为 kebab-case 目录名
EOF
    exit 1
}

# 转换 PascalCase 到 kebab-case
# 例如: TestDemo -> test-demo
pascal_to_kebab() {
    local input="$1"
    # 使用 Python 进行转换（macOS/Linux 兼容）
    if command -v python3 &> /dev/null; then
        python3 -c "
import re
import sys
name = sys.argv[1]
# 在大写字母前插入连字符（首字母除外）
result = re.sub(r'(?<!^)(?=[A-Z])', '-', name).lower()
print(result, end='')
" "$input"
    else
        # 备用方案：使用 sed
        echo "$input" | sed 's/\([A-Z]\)/-\L\1/g' | sed 's/^-//'
    fi
}

# 转换 PascalCase 到 camelCase
# 例如: TestDemo -> testDemo
pascal_to_camel() {
    local input="$1"
    # 使用 Python 进行转换（macOS/Linux 兼容）
    if command -v python3 &> /dev/null; then
        python3 -c "
name = '$input'
print(name[0].lower() + name[1:], end='')
"
    else
        # 备用方案：使用 tr 和 sed
        echo "$input" | sed 's/^\(.\)/\L\1/'
    fi
}

# 检查模板目录存在
check_template() {
    if [[ ! -d "$TEMPLATE_DIR" ]]; then
        log_error "模板目录不存在: $TEMPLATE_DIR"
        log_info "请先运行架构初始化脚本创建模板"
        exit 1
    fi
}

# 验证模块名
validate_module_name() {
    local module_name="$1"

    if [[ -z "$module_name" ]]; then
        log_error "模块名不能为空"
        show_usage
    fi

    # 检查是否只包含字母
    if [[ ! "$module_name" =~ ^[A-Z][a-zA-Z0-9]*$ ]]; then
        log_error "模块名必须是 PascalCase 格式（首字母大写，只含字母数字）"
        log_info "有效示例: Borrowing, Voucher, Payment"
        exit 1
    fi
}

# 检查模块是否已存在
check_module_exists() {
    local module_dir="$1"

    if [[ -d "$module_dir" ]]; then
        log_error "模块已存在: $module_dir"
        log_info "如需重建，请先删除现有模块目录"
        exit 1
    fi
}

# 替换模板占位符
replace_placeholders() {
    local file="$1"
    local module_name="$2"
    local module_name_camel="$3"
    local module_desc="$4"

    # 使用 sed 替换占位符（macOS 兼容）
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' \
            -e "s/{ModuleName}/$module_name/g" \
            -e "s/{moduleName}/$module_name_camel/g" \
            -e "s/{ModuleDescription}/$module_desc/g" \
            "$file"
    else
        sed -i \
            -e "s/{ModuleName}/$module_name/g" \
            -e "s/{moduleName}/$module_name_camel/g" \
            -e "s/{ModuleDescription}/$module_desc/g" \
            "$file"
    fi
}

# 创建目录结构
create_module_structure() {
    local module_dir="$1"
    local module_name="$2"
    local module_name_camel="$3"

    log_info "创建目录结构..."

    # 创建四层目录
    mkdir -p "$module_dir/api/dto"
    mkdir -p "$module_dir/api/controller"
    mkdir -p "$module_dir/app"
    mkdir -p "$module_dir/domain"
    mkdir -p "$module_dir/infra"

    # 复制并修改 README 文件
    for layer_readme in "$TEMPLATE_DIR"/*/README.md; do
        local layer_dir="$(dirname "$layer_readme")"
        local layer_name="$(basename "$layer_dir")"
        local target_readme="$module_dir/$layer_name/README.md"

        cp "$layer_readme" "$target_readme"
        replace_placeholders "$target_readme" "$module_name" "$module_name_camel" "$module_name 模块"
    done

    # 创建主 README
    cat > "$module_dir/README.md" << EOF
# $module_name 模块

一旦我所属的文件夹有所变化，请更新我。

本目录是 **$module_name 模块** 的主目录，采用 DDD 四层架构。

## 目录结构

\`\`\`
$module_name/
├── api/           # API 层 - 对外接口定义（Controller、DTO）
├── app/           # 应用层 - 业务编排（Facade、ApplicationService）
├── domain/        # 领域层 - 核心业务逻辑（Entity、Repository 接口）
└── infra/         # 基础设施层 - 技术实现（Mapper、RepositoryImpl）
\`\`\`

## 模块职责

<!-- TODO: 描述模块的核心职责 -->

## 对外接口

- **Facade**: \`${module_name_camel}Facade\` - 应用层门面
- **DTO**: \`api.dto\` - 数据传输对象

## 依赖关系

- 本模块可依赖: \`common\`, \`dto\`, 其他模块的 \`api.dto\`
- 本模块被依赖: \`api.dto\`, \`app\` 层

## 创建时间

$(date '+%Y-%m-%d %H:%M:%S')
EOF

    log_success "目录结构创建完成"
}

# 生成基础文件
generate_base_files() {
    local module_dir="$1"
    local module_name="$2"
    local module_name_camel="$3"
    local package_path="com/nexusarchive/modules/$(basename "$module_dir")"

    log_info "生成基础代码文件..."

    # API 层 - Controller 模板
    cat > "$module_dir/api/controller/${module_name}Controller.java" << EOF
package com.nexusarchive.modules.$(basename "$module_dir").api.controller;

import com.nexusarchive.modules.$(basename "$module_dir").app.${module_name}Facade;
import com.nexusarchive.modules.$(basename "$module_dir").api.dto.${module_name}Request;
import com.nexusarchive.modules.$(basename "$module_dir").api.dto.${module_name}Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * $module_name 模块 API 控制器
 *
 * @todo 添加 API 端点
 */
@RestController
@RequestMapping("/api/${module_name_camel}")
@RequiredArgsConstructor
@Tag(name = "$module_name API")
public class ${module_name}Controller {

    private final ${module_name}Facade facade;

    @PostMapping
    @Operation(summary = "创建${module_name}")
    public ${module_name}Response create(@RequestBody ${module_name}Request request) {
        return facade.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询${module_name}")
    public ${module_name}Response getById(@PathVariable String id) {
        return facade.getById(id);
    }
}
EOF

    # API 层 - DTO 模板
    cat > "$module_dir/api/dto/${module_name}Request.java" << EOF
package com.nexusarchive.modules.$(basename "$module_dir").api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * $module_name 请求 DTO
 */
@Data
public class ${module_name}Request {

    @NotBlank(message = "名称不能为空")
    private String name;

    // @todo 添加其他请求字段
}
EOF

    cat > "$module_dir/api/dto/${module_name}Response.java" << EOF
package com.nexusarchive.modules.$(basename "$module_dir").api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * $module_name 响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ${module_name}Response {

    private String id;
    private String name;
    private String status;

    // @todo 添加其他响应字段
}
EOF

    # Application 层 - Facade 接口
    cat > "$module_dir/app/${module_name}Facade.java" << EOF
package com.nexusarchive.modules.$(basename "$module_dir").app;

import com.nexusarchive.modules.$(basename "$module_dir").api.dto.${module_name}Request;
import com.nexusarchive.modules.$(basename "$module_dir").api.dto.${module_name}Response;

/**
 * $module_name 模块门面接口
 *
 * <p>对外暴露的唯一入口，内部协调领域对象完成业务编排。</p>
 */
public interface ${module_name}Facade {

    /**
     * 创建${module_name}
     */
    ${module_name}Response create(${module_name}Request request);

    /**
     * 根据ID查询
     */
    ${module_name}Response getById(String id);
}
EOF

    # Application 层 - ApplicationService
    cat > "$module_dir/app/${module_name}ApplicationService.java" << EOF
package com.nexusarchive.modules.$(basename "$module_dir").app;

import com.nexusarchive.modules.$(basename "$module_dir").api.dto.${module_name}Request;
import com.nexusarchive.modules.$(basename "$module_dir").api.dto.${module_name}Response;
import com.nexusarchive.modules.$(basename "$module_dir").domain.${module_name};
import com.nexusarchive.modules.$(basename "$module_dir").domain.${module_name}Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * $module_name 应用服务
 *
 * <p>实现 Facade 接口，协调领域对象完成业务编排。</p>
 */
@Service
@RequiredArgsConstructor
public class ${module_name}ApplicationService implements ${module_name}Facade {

    private final ${module_name}Repository repository;

    @Override
    @Transactional
    public ${module_name}Response create(${module_name}Request request) {
        // @todo 实现创建逻辑
        ${module_name} domain = ${module_name}.builder()
            .name(request.getName())
            .build();

        ${module_name} saved = repository.save(domain);
        return toResponse(saved);
    }

    @Override
    public ${module_name}Response getById(String id) {
        // @todo 实现查询逻辑
        return repository.findById(id)
            .map(this::toResponse)
            .orElse(null);
    }

    private ${module_name}Response toResponse(${module_name} domain) {
        return ${module_name}Response.builder()
            .id(domain.getId())
            .name(domain.getName())
            .status(domain.getStatus() != null ? domain.getStatus().name() : null)
            .build();
    }
}
EOF

    # Domain 层 - Entity
    cat > "$module_dir/domain/${module_name}.java" << EOF
package com.nexusarchive.modules.$(basename "$module_dir").domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * $module_name 领域实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ${module_name} {

    private String id;
    private String name;
    private ${module_name}Status status;

    // @todo 添加其他领域字段和方法

    /**
     * ${module_name} 状态枚举
     */
    public enum ${module_name}Status {
        ACTIVE, INACTIVE
    }
}
EOF

    # Domain 层 - Repository 接口
    cat > "$module_dir/domain/${module_name}Repository.java" << EOF
package com.nexusarchive.modules.$(basename "$module_dir").domain;

import java.util.Optional;

/**
 * $module_name 仓储接口
 */
public interface ${module_name}Repository {

    /**
     * 保存${module_name}
     */
    ${module_name} save(${module_name} domain);

    /**
     * 根据ID查询
     */
    Optional<${module_name}> findById(String id);

    /**
     * 根据ID删除
     */
    void deleteById(String id);
}
EOF

    # Infra 层 - RepositoryImpl
    cat > "$module_dir/infra/${module_name}RepositoryImpl.java" << EOF
package com.nexusarchive.modules.$(basename "$module_dir").infra;

import com.nexusarchive.modules.$(basename "$module_dir").domain.${module_name};
import com.nexusarchive.modules.$(basename "$module_dir").domain.${module_name}Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * $module_name 仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ${module_name}RepositoryImpl implements ${module_name}Repository {

    // @todo 注入 Mapper（如需要数据库持久化）
    // private final ${module_name}Mapper mapper;

    @Override
    public ${module_name} save(${module_name} domain) {
        // @todo 实现保存逻辑
        return domain;
    }

    @Override
    public java.util.Optional<${module_name}> findById(String id) {
        // @todo 实现查询逻辑
        return java.util.Optional.empty();
    }

    @Override
    public void deleteById(String id) {
        // @todo 实现删除逻辑
    }
}
EOF

    log_success "基础代码文件生成完成"
}

# 显示后续步骤
show_next_steps() {
    local module_name="$1"
    local module_dir="$(basename "$2")"

    cat << EOF

${GREEN}═══════════════════════════════════════════════════════════${NC}
${GREEN}模块创建成功！${NC}
${GREEN}═══════════════════════════════════════════════════════════${NC}

${BLUE}模块位置:${NC}
  $module_dir

${BLUE}下一步:${NC}

  1. ${YELLOW}完善领域逻辑${NC}
     编辑: ${module_dir}/domain/${module_name}.java

  2. ${YELLOW}实现持久化${NC}
     - 创建 Mapper: ${module_dir}/infra/${module_name}Mapper.java
     - 更新 RepositoryImpl

  3. ${YELLOW}编写单元测试${NC}
     创建: src/test/java/.../modules/${module_dir}/

  4. ${YELLOW}运行架构测试${NC}
     cd nexusarchive-java
     mvn test -Dtest=ModuleBoundaryTest

  5. ${YELLOW}更新模块清单${NC}
     编辑: nexusarchive-java/src/main/java/com/nexusarchive/modules/README.md

${BLUE}文档参考:${NC}
  - 后端模块创建 SOP: docs/architecture/backend-module-creation-sop.md
  - 模块边界规则: docs/architecture/module-boundaries.md

EOF
}

# 主函数
main() {
    local module_name="$1"

    # 参数检查
    if [[ -z "$module_name" ]] || [[ "$module_name" == "-h" ]] || [[ "$module_name" == "--help" ]]; then
        show_usage
    fi

    # 验证模块名
    validate_module_name "$module_name"

    # 计算目录名
    local module_dir_name="$(pascal_to_kebab "$module_name")"
    local module_dir="$MODULES_DIR/$module_dir_name"
    local module_name_camel="$(pascal_to_camel "$module_name")"

    log_info "开始创建模块: $module_name"
    log_info "目录名: $module_dir_name"

    # 检查模板
    check_template

    # 检查是否已存在
    check_module_exists "$module_dir"

    # 创建目录结构
    create_module_structure "$module_dir" "$module_name" "$module_name_camel"

    # 生成基础文件
    generate_base_files "$module_dir" "$module_name" "$module_name_camel"

    # 显示后续步骤
    show_next_steps "$module_name" "$module_dir_name"

    log_success "完成！"
}

# 执行主函数
main "$@"
