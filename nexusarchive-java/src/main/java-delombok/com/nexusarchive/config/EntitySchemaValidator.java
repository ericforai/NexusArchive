// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库
// Output: EntitySchemaValidator 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entity-Database Schema 一致性验证器
 * 
 * 在应用启动时自动检测所有 @TableName 标注的 Entity，
 * 验证其字段是否在数据库中存在对应的列。
 * 
 * 这是为了避免 "Entity 有字段但 Flyway 迁移脚本遗漏" 的问题反复发生。
 * 
 * 配置项:
 * - schema.validation.enabled: 是否启用验证（默认 true，生产环境可关闭）
 * - schema.validation.fail-on-error: 发现不一致时是否阻止启动（默认 false，只警告）
 * 
 * @author NexusArchive Team
 * @since 2025-12-09
 */
@Component
public class EntitySchemaValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EntitySchemaValidator.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${schema.validation.enabled:true}")
    private boolean enabled;

    @Value("${schema.validation.fail-on-error:false}")
    private boolean failOnError;

    @Value("${schema.validation.entity-package:com.nexusarchive.entity}")
    private String entityPackage;

    public EntitySchemaValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!enabled) {
            log.info("[Schema Validator] Disabled by configuration");
            return;
        }

        log.info("[Schema Validator] Starting entity-database consistency check...");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int checkedEntities = 0;

        try {
            // 扫描所有带 @TableName 注解的 Entity 类
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                    false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(TableName.class));

            var beanDefinitions = scanner.findCandidateComponents(entityPackage);

            for (var beanDef : beanDefinitions) {
                try {
                    Class<?> entityClass = Class.forName(beanDef.getBeanClassName());
                    validateEntity(entityClass, errors, warnings);
                    checkedEntities++;
                } catch (ClassNotFoundException e) {
                    log.warn("[Schema Validator] Could not load class: {}", beanDef.getBeanClassName());
                }
            }

            // 输出验证结果
            log.info("[Schema Validator] Checked {} entities", checkedEntities);

            if (!warnings.isEmpty()) {
                log.warn("[Schema Validator] Found {} warnings:", warnings.size());
                warnings.forEach(w -> log.warn("  - {}", w));
            }

            if (!errors.isEmpty()) {
                String errorMessage = String.format(
                        "[Schema Validator] Found %d MISSING COLUMNS!\n%s\n" +
                                ">>> Please create a Flyway migration to add these columns. <<<",
                        errors.size(),
                        errors.stream().collect(Collectors.joining("\n")));

                if (failOnError) {
                    throw new RuntimeException(errorMessage);
                } else {
                    log.error(errorMessage);
                    log.error("=".repeat(60));
                    log.error(">>> Set schema.validation.fail-on-error=true to block startup");
                    log.error("=".repeat(60));
                }
            } else {
                log.info("[Schema Validator] ✓ All entity fields have matching database columns");
            }

        } catch (RuntimeException e) {
            if (failOnError) {
                throw e;
            }
            log.error("[Schema Validator] Error during validation: {}", e.getMessage());
        }
    }

    private void validateEntity(Class<?> entityClass, List<String> errors, List<String> warnings) {
        TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
        if (tableNameAnnotation == null) {
            return;
        }

        String tableName = tableNameAnnotation.value();
        if (tableName.isEmpty()) {
            tableName = camelToSnake(entityClass.getSimpleName());
        }

        // 获取数据库中的实际列
        Set<String> dbColumns = getTableColumns(tableName);
        if (dbColumns.isEmpty()) {
            warnings.add(String.format("Table '%s' not found in database (Entity: %s)",
                    tableName, entityClass.getSimpleName()));
            return;
        }

        // 检查每个 Entity 字段
        for (Field field : getAllFields(entityClass)) {
            // 跳过静态字段和 transient 字段
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            // 检查 @TableField(exist = false)
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                continue; // 明确标记为不存在于数据库的字段，跳过
            }

            // 确定数据库列名
            String columnName;
            if (tableField != null && !tableField.value().isEmpty()) {
                columnName = tableField.value().toLowerCase();
            } else {
                columnName = camelToSnake(field.getName());
            }

            // 检查列是否存在
            if (!dbColumns.contains(columnName)) {
                errors.add(String.format(
                        "MISSING: Entity '%s' field '%s' -> Column '%s' not found in table '%s'",
                        entityClass.getSimpleName(),
                        field.getName(),
                        columnName,
                        tableName));
            }
        }
    }

    private Set<String> getTableColumns(String tableName) {
        try {
            String sql = """
                    SELECT column_name
                    FROM information_schema.columns
                    WHERE table_name = ? AND table_schema = 'public'
                    """;
            List<String> columns = jdbcTemplate.queryForList(sql, String.class, tableName.toLowerCase());
            return columns.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("[Schema Validator] Could not query columns for table '{}': {}", tableName, e.getMessage());
            return Collections.emptySet();
        }
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
