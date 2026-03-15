// Input: Spring, Lombok, SLF4J
// Output: DefaultErpMapper 类
// Pos: 集成模块 - ERP 默认映射实现

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.common.constants.DateFormat;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherEntryDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 默认 ERP 映射器
 * 基于配置文件进行字段映射
 *
 * <p>该映射器实现了统一的 ErpMapper 接口，提供基于配置的 ERP 数据转换能力。</p>
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>简单字段映射：通过 field 属性直接映射源字段到目标字段</li>
 *   <li>脚本转换：通过 script 属性执行 Groovy 脚本进行复杂转换</li>
 *   <li>类型转换：自动处理常见类型转换（String -> LocalDate, String -> BigDecimal 等）</li>
 *   <li>数组映射：支持 entries、attachments 等数组的批量映射</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ErpMapper mapper = new DefaultErpMapper(configLoader, scriptEngine);
 * AccountingSipDto dto = mapper.mapToSipDto(erpResponse, "yonsuite", erpConfig);
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultErpMapper implements ErpMapper {

    private final MappingConfigLoader configLoader;
    private final GroovyMappingEngine scriptEngine;

    /**
     * 将 ERP 响应转换为 AccountingSipDto
     *
     * @param erpResponse ERP 原始响应对象
     * @param sourceSystem ERP 标识
     * @param config ERP 配置
     * @return 标准化的 SIP DTO
     * @throws MappingException 当映射失败时抛出
     */
    @Override
    public AccountingSipDto mapToSipDto(Object erpResponse, String sourceSystem, ErpConfig config) {
        log.debug("Mapping ERP response to SIP DTO, sourceSystem: {}", sourceSystem);

        MappingConfig mappingConfig;
        try {
            mappingConfig = configLoader.loadMapping(sourceSystem);
        } catch (Exception e) {
            throw new MappingException("Failed to load mapping config for source system: " + sourceSystem, e);
        }

        AccountingSipDto dto = new AccountingSipDto();
        dto.setRequestId(UUID.randomUUID().toString());
        dto.setSourceSystem(sourceSystem);

        // 映射 header
        VoucherHeadDto header = mapHeader(erpResponse, mappingConfig, config);
        dto.setHeader(header);

        // 映射 entries
        List<VoucherEntryDto> entries = mapEntries(erpResponse, mappingConfig, config);
        dto.setEntries(entries);

        // 映射 attachments
        List<AttachmentDto> attachments = mapAttachments(erpResponse, mappingConfig, config);
        dto.setAttachments(attachments);

        log.debug("Successfully mapped ERP response to SIP DTO, requestId: {}", dto.getRequestId());
        return dto;
    }

    /**
     * 映射凭证头信息
     */
    private VoucherHeadDto mapHeader(Object response, MappingConfig mappingConfig, ErpConfig erpConfig) {
        VoucherHeadDto header = new VoucherHeadDto();
        ScriptContext scriptContext = ScriptContext.builder()
                .ctx(response)
                .config(erpConfig)
                .build();

        Map<String, FieldMapping> headerMappings = mappingConfig.getHeaderMappings();
        if (headerMappings == null || headerMappings.isEmpty()) {
            log.warn("No header mappings defined for source system: {}", mappingConfig.getSourceSystem());
            return header;
        }

        for (Map.Entry<String, FieldMapping> entry : headerMappings.entrySet()) {
            String targetField = entry.getKey();
            FieldMapping mapping = entry.getValue();

            Object value = extractValue(response, mapping, scriptContext);
            setFieldValue(header, targetField, value);
        }

        return header;
    }

    /**
     * 映射分录列表
     */
    private List<VoucherEntryDto> mapEntries(Object response, MappingConfig mappingConfig, ErpConfig erpConfig) {
        List<VoucherEntryDto> entries = new ArrayList<>();
        ObjectMapping entryMapping = mappingConfig.getEntries();

        if (entryMapping == null || entryMapping.getSource() == null) {
            log.debug("No entries mapping defined, returning empty list");
            return entries;
        }

        // 提取源数组
        List<?> sourceArray = extractArray(response, entryMapping.getSource());
        if (sourceArray == null || sourceArray.isEmpty()) {
            log.debug("Source entries array is null or empty");
            return entries;
        }

        Map<String, FieldMapping> itemMapping = entryMapping.getItem();
        if (itemMapping == null || itemMapping.isEmpty()) {
            log.warn("Entries item mapping is null or empty, returning empty list");
            return entries;
        }

        ScriptContext scriptContext = ScriptContext.builder()
                .config(erpConfig)
                .build();

        int lineNo = 1;
        for (Object item : sourceArray) {
            scriptContext.setCtx(item);
            VoucherEntryDto entry = mapObject(item, itemMapping, scriptContext, VoucherEntryDto.class);
            // 自动设置分录行号
            if (entry.getLineNo() == null) {
                entry.setLineNo(lineNo++);
            }
            entries.add(entry);
        }

        log.debug("Mapped {} voucher entries", entries.size());
        return entries;
    }

    /**
     * 映射附件列表
     */
    private List<AttachmentDto> mapAttachments(Object response, MappingConfig mappingConfig, ErpConfig erpConfig) {
        List<AttachmentDto> attachments = new ArrayList<>();
        ObjectMapping attachmentMapping = mappingConfig.getAttachments();

        if (attachmentMapping == null || attachmentMapping.getSource() == null) {
            log.debug("No attachments mapping defined, returning empty list");
            return attachments;
        }

        List<?> sourceArray = extractArray(response, attachmentMapping.getSource());
        if (sourceArray == null || sourceArray.isEmpty()) {
            log.debug("Source attachments array is null or empty");
            return attachments;
        }

        Map<String, FieldMapping> itemMapping = attachmentMapping.getItem();
        if (itemMapping == null || itemMapping.isEmpty()) {
            log.warn("Attachments item mapping is null or empty, returning empty list");
            return attachments;
        }

        ScriptContext scriptContext = ScriptContext.builder()
                .config(erpConfig)
                .build();

        for (Object item : sourceArray) {
            scriptContext.setCtx(item);
            AttachmentDto attachment = mapObject(item, itemMapping, scriptContext, AttachmentDto.class);
            attachments.add(attachment);
        }

        log.debug("Mapped {} attachments", attachments.size());
        return attachments;
    }

    /**
     * 通用对象映射方法
     *
     * @param source 源对象
     * @param mappings 字段映射配置
     * @param context 脚本执行上下文
     * @param targetClass 目标类
     * @param <T> 目标类型
     * @return 映射后的目标对象
     */
    private <T> T mapObject(Object source, Map<String, FieldMapping> mappings,
                            ScriptContext context, Class<T> targetClass) {
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, FieldMapping> entry : mappings.entrySet()) {
                String targetField = entry.getKey();
                FieldMapping mapping = entry.getValue();

                Object value = extractValue(source, mapping, context);
                setFieldValue(target, targetField, value);
            }

            return target;
        } catch (NoSuchMethodException e) {
            log.error("Target class {} missing no-arg constructor", targetClass.getSimpleName(), e);
            throw new MappingException("Failed to create instance of " + targetClass.getSimpleName() +
                    ": no-arg constructor not found", e);
        } catch (Exception e) {
            log.error("Failed to map object to {}", targetClass.getSimpleName(), e);
            throw new MappingException("Failed to map object: " + e.getMessage(), e);
        }
    }

    /**
     * 提取字段值
     * 支持简单字段映射和脚本执行
     *
     * @param source 源对象
     * @param mapping 字段映射配置
     * @param context 脚本执行上下文
     * @return 提取的值
     */
    private Object extractValue(Object source, FieldMapping mapping, ScriptContext context) {
        if (mapping.hasScript()) {
            try {
                return scriptEngine.execute(mapping.getScript(), context);
            } catch (MappingScriptException e) {
                log.warn("Script execution failed, returning null: {}", e.getMessage());
                return null;
            }
        }

        if (mapping.getField() != null) {
            return getFieldValue(source, mapping.getField());
        }

        return null;
    }

    /**
     * 通过字段路径获取字段值
     * 支持嵌套字段访问（如 "user.address.city"）
     *
     * @param obj 源对象
     * @param fieldPath 字段路径（支持点号分隔的嵌套路径）
     * @return 字段值，获取失败返回 null
     */
    private Object getFieldValue(Object obj, String fieldPath) {
        if (obj == null || fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }

        try {
            String[] parts = fieldPath.split("\\.");
            Object current = obj;

            for (String part : parts) {
                if (current == null) {
                    return null;
                }

                // 尝试 getter 方法
                String methodName = "get" + Character.toUpperCase(part.charAt(0)) + part.substring(1);

                try {
                    java.lang.reflect.Method method = current.getClass().getMethod(methodName);
                    current = method.invoke(current);
                } catch (NoSuchMethodException e) {
                    // 尝试 is 开头的 getter (for boolean)
                    String isMethodName = "is" + Character.toUpperCase(part.charAt(0)) + part.substring(1);
                    try {
                        java.lang.reflect.Method method = current.getClass().getMethod(isMethodName);
                        current = method.invoke(current);
                    } catch (NoSuchMethodException e2) {
                        // 尝试直接访问字段
                        Field field = findField(current.getClass(), part);
                        if (field != null) {
                            field.setAccessible(true);
                            current = field.get(current);
                        } else {
                            log.warn("Field not found: {} in class {}", part, current.getClass().getSimpleName());
                            return null;
                        }
                    }
                }
            }

            return current;
        } catch (Exception e) {
            log.warn("Failed to get field value: {}", fieldPath);
            return null;
        }
    }

    /**
     * 在类层次结构中查找字段（包括父类）
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 设置目标对象的字段值
     * 支持类型转换
     *
     * @param target 目标对象
     * @param fieldPath 字段路径
     * @param value 要设置的值
     */
    private void setFieldValue(Object target, String fieldPath, Object value) {
        if (value == null || target == null || fieldPath == null || fieldPath.isEmpty()) {
            return;
        }

        try {
            String[] parts = fieldPath.split("\\.");
            if (parts.length == 1) {
                // 直接字段
                String fieldName = parts[0];
                // 尝试 setter
                String methodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

                for (java.lang.reflect.Method method : target.getClass().getMethods()) {
                    if (method.getName().equals(methodName) &&
                            method.getParameterCount() == 1) {
                        Object convertedValue = convertValue(value, method.getParameterTypes()[0]);
                        if (convertedValue != null) {
                            method.invoke(target, convertedValue);
                        }
                        return;
                    }
                }
            } else {
                // 嵌套字段（暂不支持）
                log.debug("Nested field setting not supported: {}", fieldPath);
            }
        } catch (Exception e) {
            log.debug("Failed to set field value: {} = {}", fieldPath, value);
        }
    }

    /**
     * 类型转换
     * 支持常见类型之间的转换
     *
     * @param value 原始值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Failed to convert {} to Integer", value);
                return null;
            }
        }

        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Failed to convert {} to Long", value);
                return null;
            }
        }

        if (targetType == BigDecimal.class) {
            if (value instanceof Number) {
                return new BigDecimal(value.toString());
            }
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Failed to convert {} to BigDecimal", value);
                return null;
            }
        }

        if (targetType == LocalDate.class) {
            if (value instanceof String) {
                String dateStr = (String) value;
                // 尝试常见日期格式
                String[] patterns = {
                        DateFormat.DATE,
                        "yyyy/MM/dd",
                        "yyyyMMdd",
                        DateFormat.DATETIME,
                        "yyyy/MM/dd HH:mm:ss"
                };

                for (String pattern : patterns) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                        return LocalDate.parse(dateStr, formatter);
                    } catch (DateTimeParseException e) {
                        // 尝试下一个格式
                    }
                }
                log.warn("Failed to parse date: {}", dateStr);
                return null;
            }
        }

        if (targetType.isEnum()) {
            try {
                @SuppressWarnings("rawtypes")
                Class enumType = targetType;
                if (value instanceof String) {
                    return Enum.valueOf(enumType, (String) value);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Failed to convert {} to enum {}", value, targetType.getSimpleName());
                return null;
            }
        }

        log.debug("Unsupported type conversion: {} -> {}", value.getClass(), targetType);
        return value;
    }

    /**
     * 从源对象中提取数组/列表
     *
     * @param obj 源对象
     * @param fieldPath 字段路径
     * @return 提取的列表，非列表类型返回 null
     */
    @SuppressWarnings("unchecked")
    private List<?> extractArray(Object obj, String fieldPath) {
        if (obj == null || fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }

        Object value = getFieldValue(obj, fieldPath);
        if (value instanceof List) {
            return (List<?>) value;
        }

        // 支持数组类型
        if (value != null && value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            return List.of(array);
        }

        return null;
    }
}
