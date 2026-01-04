// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeParser.java
package com.nexusarchive.integration.erp.ai.llm.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CodeParser {

    private static final Pattern JAVA_CODE_BLOCK = Pattern.compile("```java\\s*([\\s\\S]*?)\\s*```");
    private static final Pattern CLASS_DECLARATION = Pattern.compile("public\\s+class\\s+(\\w+)");
    private static final Pattern PACKAGE_DECLARATION = Pattern.compile("package\\s+([\\w.]+);");

    /**
     * 从 AI 响应中提取 Java 代码
     */
    public String extractJavaCode(String aiResponse) throws CodeValidationException {
        log.info("Extracting Java code from AI response ({} chars)", aiResponse.length());

        // 尝试提取 ```java 代码块
        Matcher matcher = JAVA_CODE_BLOCK.matcher(aiResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 如果没有代码块标记，检查是否直接是 Java 代码
        if (containsJavaKeywords(aiResponse)) {
            return aiResponse.trim();
        }

        throw new CodeValidationException("No Java code found in AI response", List.of(
            "Response does not contain Java code block",
            "Expected format: ```java\\ncode```"
        ));
    }

    /**
     * 解析生成的代码元信息
     */
    public ParsedCodeMetadata parseMetadata(String javaCode) throws CodeValidationException {
        ParsedCodeMetadata metadata = new ParsedCodeMetadata();

        // 提取包名
        Matcher pkgMatcher = PACKAGE_DECLARATION.matcher(javaCode);
        if (pkgMatcher.find()) {
            metadata.setPackageName(pkgMatcher.group(1));
        }

        // 提取类名
        Matcher classMatcher = CLASS_DECLARATION.matcher(javaCode);
        if (classMatcher.find()) {
            metadata.setClassName(classMatcher.group(1));
        }

        if (metadata.getPackageName() == null || metadata.getClassName() == null) {
            throw new CodeValidationException("Invalid Java code: missing package or class declaration",
                List.of("package: " + metadata.getPackageName(), "class: " + metadata.getClassName()));
        }

        return metadata;
    }

    private boolean containsJavaKeywords(String text) {
        return text.contains("package ") &&
               text.contains("class ") &&
               text.contains("public ") &&
               text.contains("return ");
    }

    public static class ParsedCodeMetadata {
        private String packageName;
        private String className;

        public String getPackageName() { return packageName; }
        public String getClassName() { return className; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        public void setClassName(String className) { this.className = className; }
    }
}
