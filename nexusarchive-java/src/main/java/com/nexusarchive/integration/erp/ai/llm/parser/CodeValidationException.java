// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeValidationException.java
package com.nexusarchive.integration.erp.ai.llm.parser;

import lombok.Getter;

import java.util.List;

@Getter
public class CodeValidationException extends Exception {
    private final List<String> errors;

    public CodeValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
}
