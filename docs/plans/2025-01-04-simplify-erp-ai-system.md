# Simplify ERP AI System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove all external AI API calling functionality to reduce code complexity, keeping only the essential template-based ERP adapter generation system.

**Architecture:** Remove the entire LLM integration layer (Claude, GLM, DeepSeek clients) and related infrastructure (prompt building, code parsing, metrics, rate limiting). Keep the valuable components: OpenAPI parser, ERP type identifier, scenario namer, business semantic mapper, and template-based code generator. Update the orchestrator and controller to only use template generation.

**Tech Stack:** Spring Boot 3.1.6, Java 17, Maven

---

## Overview

This plan removes ~15 files and simplifies the codebase by eliminating external AI API dependencies. The system will rely on the proven template-based code generator (`ErpAdapterCodeGenerator`) instead of calling external LLM APIs.

**What gets removed:**
- LLM API clients (Claude, GLM, DeepSeek)
- Prompt building infrastructure
- AI code parsing and validation
- Rate limiting for API calls
- AI metrics and monitoring
- AI session management

**What gets kept:**
- OpenAPI document parser
- ERP type identifier (file name/path analysis)
- Scenario namer (path → naming convention)
- Business semantic mapper (API → standard scenario)
- Template-based code generator
- Auto-deployment services

---

## Task 1: Remove LLM API Client Files

**Files:**
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/ClaudeApiClient.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/CompletionRequest.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/CompletionResponse.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/GlmApiClient.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/GlmCompletionRequest.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/GlmCompletionResponse.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/deepseek/DeepSeekApiClient.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/deepseek/DeepSeekCompletionRequest.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/deepseek/DeepSeekCompletionResponse.java`

**Step 1: Delete all LLM client files**

```bash
cd /Users/user/nexusarchive/nexusarchive-java

# Delete Claude client files
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/ClaudeApiClient.java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/CompletionRequest.java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/CompletionResponse.java

# Delete GLM client files
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/GlmApiClient.java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/GlmCompletionRequest.java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/GlmCompletionResponse.java

# Delete DeepSeek client files
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/deepseek/DeepSeekApiClient.java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/deepseek/DeepSeekCompletionRequest.java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/deepseek/DeepSeekCompletionResponse.java

# Delete empty directories if they become empty
rmdir src/main/java/com/nexusarchive/integration/erp/ai/llm/claude 2>/dev/null || true
rmdir src/main/java/com/nexusarchive/integration/erp/ai/llm/glm 2>/dev/null || true
rmdir src/main/java/com/nexusarchive/integration/erp/ai/llm/deepseek 2>/dev/null || true
```

**Step 2: Verify deletion**

```bash
# Verify files are deleted
ls src/main/java/com/nexusarchive/integration/erp/ai/llm/claude/ 2>&1 | grep "No such file"
ls src/main/java/com/nexusarchive/integration/erp/ai/llm/glm/ 2>&1 | grep "No such file"
ls src/main/java/com/nexusarchive/integration/erp/ai/llm/deepseek/ 2>&1 | grep "No such file"
```

Expected: "No such file or directory" for all three directories

**Step 3: Commit**

```bash
git add -A
git commit -m "refactor(ai): remove LLM API client files (Claude, GLM, DeepSeek)"
```

---

## Task 2: Remove AI Prompt Building and Parsing Infrastructure

**Files:**
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/CodeGenerationPromptBuilder.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/PromptContext.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeParser.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/JavaSyntaxValidator.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeValidationException.java`

**Step 1: Delete prompt and parser files**

```bash
cd /Users/user/nexusarchive/nexusarchive-java

# Delete prompt builder files
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/CodeGenerationPromptBuilder.java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/PromptContext.java

# Delete parser files
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeParser.java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/JavaSyntaxValidator.java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/CodeValidationException.java

# Delete empty directories
rmdir src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt 2>/dev/null || true
rmdir src/main/java/com/nexusarchive/integration/erp/ai/llm/parser 2>/dev/null || true
```

**Step 2: Verify deletion**

```bash
# Verify directories are deleted or empty
ls src/main/java/com/nexusarchive/integration/erp/ai/llm/prompt/ 2>&1
ls src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/ 2>&1
```

Expected: Directories deleted or "No such file or directory"

**Step 3: Commit**

```bash
git add -A
git commit -m "refactor(ai): remove prompt building and code parsing infrastructure"
```

---

## Task 3: Remove Rate Limiting Service

**Files:**
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/llm/RateLimitService.java`

**Step 1: Delete rate limit service**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/llm/RateLimitService.java
```

**Step 2: Verify deletion**

```bash
ls src/main/java/com/nexusarchive/integration/erp/ai/llm/RateLimitService.java 2>&1 | grep "No such file"
```

Expected: "No such file or directory"

**Step 3: Delete empty llm directory if empty**

```bash
# Check if llm directory is empty
ls src/main/java/com/nexusarchive/integration/erp/ai/llm/
# If empty, delete it
rmdir src/main/java/com/nexusarchive/integration/erp/ai/llm 2>/dev/null || true
```

**Step 4: Commit**

```bash
git add -A
git commit -m "refactor(ai): remove rate limiting service (no longer needed without API calls)"
```

---

## Task 4: Remove AI Code Generation Service

**Files:**
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationService.java`

**Step 1: Delete AI code generation service**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
rm -f src/main/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationService.java
```

**Step 2: Verify deletion**

```bash
ls src/main/java/com/nexusarchive/integration/erp/ai/generator/AiCodeGenerationService.java 2>&1 | grep "No such file"
```

Expected: "No such file or directory"

**Step 3: Commit**

```bash
git add -A
git commit -m "refactor(ai): remove AI code generation service"
```

---

## Task 5: Remove AI Session Management

**Files:**
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/service/AiGenerationSessionService.java`
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/dto/AiGenerationSession.java`

**Step 1: Delete AI session management files**

```bash
cd /Users/user/nexusarchive/nexusarchive-java

# Delete service
rm -f src/main/java/com/nexusarchive/integration/erp/ai/service/AiGenerationSessionService.java

# Delete DTO
rm -f src/main/java/com/nexusarchive/integration/erp/ai/dto/AiGenerationSession.java

# Delete empty directories if they become empty
rmdir src/main/java/com/nexusarchive/integration/erp/ai/service 2>/dev/null || true
rmdir src/main/java/com/nexusarchive/integration/erp/ai/dto 2>/dev/null || true
```

**Step 2: Verify deletion**

```bash
ls src/main/java/com/nexusarchive/integration/erp/ai/service/ 2>&1 | grep "No such file"
ls src/main/java/com/nexusarchive/integration/erp/ai/dto/ 2>&1 | grep "No such file"
```

Expected: "No such file or directory" for both

**Step 3: Commit**

```bash
git add -A
git commit -m "refactor(ai): remove AI session management (service and DTO)"
```

---

## Task 6: Remove AI Metrics and Monitoring

**Files:**
- Delete: `src/main/java/com/nexusarchive/integration/erp/ai/monitoring/AiGenerationMetrics.java`
- Delete: `src/main/java/com/nexusarchive/controller/AiMetricsController.java`

**Step 1: Delete AI metrics files**

```bash
cd /Users/user/nexusarchive/nexusarchive-java

# Delete metrics class
rm -f src/main/java/com/nexusarchive/integration/erp/ai/monitoring/AiGenerationMetrics.java

# Delete metrics controller
rm -f src/main/java/com/nexusarchive/controller/AiMetricsController.java

# Delete empty monitoring directory
rmdir src/main/java/com/nexusarchive/integration/erp/ai/monitoring 2>/dev/null || true
```

**Step 2: Verify deletion**

```bash
ls src/main/java/com/nexusarchive/integration/erp/ai/monitoring/ 2>&1 | grep "No such file"
ls src/main/java/com/nexusarchive/controller/AiMetricsController.java 2>&1 | grep "No such file"
```

Expected: "No such file or directory" for both

**Step 3: Commit**

```bash
git add -A
git commit -m "refactor(ai): remove AI metrics and monitoring"
```

---

## Task 7: Remove AI Configuration Properties

**Files:**
- Delete: `src/main/java/com/nexusarchive/config/AiProperties.java`
- Modify: `src/main/resources/application-ai.yml` (delete entire file)
- Modify: `src/main/resources/application.yml` (remove ai import)

**Step 1: Delete AI properties class**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
rm -f src/main/java/com/nexusarchive/config/AiProperties.java
```

**Step 2: Delete AI configuration file**

```bash
rm -f src/main/resources/application-ai.yml
```

**Step 3: Update application.yml to remove AI import**

Read the current file:
```bash
cat src/main/resources/application.yml
```

Look for a line like `spring.profiles.include: ai` and remove it.

**Step 4: Verify changes**

```bash
# Verify files are deleted
ls src/main/java/com/nexusarchive/config/AiProperties.java 2>&1 | grep "No such file"
ls src/main/resources/application-ai.yml 2>&1 | grep "No such file"

# Verify application.yml doesn't reference AI
grep -n "ai" src/main/resources/application.yml
```

Expected: "No such file or directory" for deleted files; no AI references in application.yml

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor(ai): remove AI configuration properties"
```

---

## Task 8: Update ErpAdaptationOrchestrator to Remove AI Dependency

**Files:**
- Modify: `src/main/java/com/nexusarchive/integration/erp/ai/agent/ErpAdaptationOrchestrator.java`

**Step 1: Read the orchestrator file**

```bash
cat src/main/java/com/nexusarchive/integration/erp/ai/agent/ErpAdaptationOrchestrator.java
```

**Step 2: Remove AI dependency from ErpAdaptationOrchestrator**

Find and remove these lines:
- Line 44-45: `@Autowired(required = false) private AiCodeGenerationService aiCodeGenerationService;`
- Lines 186-208: The entire `generateCode()` method that checks for AI availability

Replace the `generateCode()` method with a simplified version:

```java
/**
 * 生成代码 - 使用模板生成
 */
private GeneratedCode generateCode(List<OpenApiDefinition> definitions, AdaptationRequest request) {
    log.info("Using template-based code generation");
    List<BusinessSemanticMapper.ScenarioMapping> mappings = mapToScenarios(definitions);
    return codeGenerator.generate(mappings, request.getErpType(), request.getErpName());
}
```

**Step 3: Verify the changes**

```bash
# Build to check for compilation errors
mvn compile -q
```

Expected: Compilation succeeds

**Step 4: Commit**

```bash
git add src/main/java/com/nexusarchive/integration/erp/ai/agent/ErpAdaptationOrchestrator.java
git commit -m "refactor(ai): remove AI dependency from ErpAdaptationOrchestrator"
```

---

## Task 9: Update ErpAdaptationController to Remove AI Endpoints

**Files:**
- Modify: `src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java`

**Step 1: Read the controller file**

```bash
cat src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java
```

**Step 2: Remove AI-specific endpoints and dependencies**

Remove the following:
- Line 15: `import com.nexusarchive.integration.erp.ai.dto.AiGenerationSession;`
- Line 21: `import com.nexusarchive.integration.erp.ai.service.AiGenerationSessionService;`
- Line 50: `private final AiGenerationSessionService aiGenerationSessionService;`
- Line 59: `AiGenerationSessionService aiGenerationSessionService` from constructor
- Lines 372-377: The `/preview/{sessionId}` endpoint
- Lines 382-423: The `/generate-ai` endpoint
- Lines 428-452: The `/regenerate-ai/{sessionId}` endpoint
- Lines 457-466: The `/approve/{sessionId}` endpoint
- Lines 468-471: The `FeedbackRequest` class

**Step 3: Verify the changes**

```bash
# Build to check for compilation errors
mvn compile -q
```

Expected: Compilation succeeds

**Step 4: Commit**

```bash
git add src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java
git commit -m "refactor(ai): remove AI endpoints from ErpAdaptationController"
```

---

## Task 10: Remove Docker Environment Variable for AI API Key

**Files:**
- Modify: `docker-compose.dev.yml`
- Modify: `.env.dev`

**Step 1: Remove CLAUDE_API_KEY from docker-compose.dev.yml**

Read the file:
```bash
cat docker-compose.dev.yml | grep -A2 -B2 CLAUDE_API_KEY
```

Remove the line:
```yaml
CLAUDE_API_KEY: ${CLAUDE_API_KEY:-}  # DeepSeek API Key
```

**Step 2: Remove CLAUDE_API_KEY from .env.dev**

```bash
# From project root
sed -i '' '/^CLAUDE_API_KEY=/d' .env.dev
```

Or manually edit `.env.dev` and remove the line:
```bash
# ========== Claude AI 配置 ==========
# DeepSeek API Key
CLAUDE_API_KEY=sk-a91294a760d6433a87472e1e5047604a
```

**Step 3: Verify changes**

```bash
# Verify docker-compose.dev.yml doesn't reference CLAUDE_API_KEY
grep -n "CLAUDE_API_KEY" docker-compose.dev.yml
# Should return nothing

# Verify .env.dev doesn't contain CLAUDE_API_KEY
grep -n "CLAUDE_API_KEY" .env.dev
# Should return nothing
```

Expected: No results from grep commands

**Step 4: Commit**

```bash
git add docker-compose.dev.yml .env.dev
git commit -m "refactor(ai): remove AI API key from Docker environment"
```

---

## Task 11: Final Verification and Testing

**Files:** All modified files

**Step 1: Clean build**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
mvn clean compile
```

Expected: Build succeeds with no errors

**Step 2: Run tests**

```bash
mvn test
```

Expected: All tests pass (or only expected failures)

**Step 3: Verify no AI code remains**

```bash
# Search for remaining AI references
grep -r "AiCodeGenerationService\|DeepSeek\|GlmApiClient\|ClaudeApiClient" src/main/java/
```

Expected: Only comments or the file deletion itself

**Step 4: Check file count reduction**

```bash
# Count remaining Java files in ai package
find src/main/java/com/nexusarchive/integration/erp/ai -name "*.java" | wc -l
```

Expected: Significantly reduced from original count (~40+ files)

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor(ai): final verification - AI infrastructure removed successfully"
```

---

## Task 12: Update Documentation

**Files:**
- Create/Update: `docs/development/erp-adapter-guide.md`
- Update: `BATCH_DEVELOPMENT_STATUS.md` (if it references AI)

**Step 1: Create simplified ERP adapter guide**

Create `docs/development/erp-adapter-guide.md`:

```markdown
# ERP Adapter Generation Guide

## Overview

The ERP adapter generation system creates Java adapters for integrating with external ERP systems. This system uses a template-based approach to generate clean, maintainable code.

## Architecture

```
OpenAPI Document Upload
         ↓
   Parse & Identify
   (ERP Type, Scenarios)
         ↓
   Template-Based
   Code Generation
         ↓
   Auto-Deploy
   (Compile, Test, Register)
```

## Components

### Core Components
- **OpenApiDocumentParser**: Parses OpenAPI/Swagger documentation
- **ErpTypeIdentifier**: Identifies ERP system from file name/path
- **ScenarioNamer**: Generates scenario names from API paths
- **BusinessSemanticMapper**: Maps APIs to standard scenarios
- **ErpAdapterCodeGenerator**: Template-based code generator
- **ErpAdapterAutoDeployService**: Automated deployment pipeline

### Deployment Services
- **CodeStorageService**: Saves generated code to filesystem
- **CompilationService**: Compiles generated Java code
- **TestExecutionService**: Runs unit tests
- **DatabaseRegistrationService**: Registers adapter in database
- **HotLoadService**: Runtime class reloading

## API Endpoints

### 1. Preview API Scenarios
```
POST /api/erp-ai/preview
```
Upload OpenAPI doc to preview detected ERP type and scenarios.

### 2. Generate and Deploy Adapter
```
POST /api/erp-ai/adapt-deploy
```
Generate adapter code and deploy automatically.

## Usage Example

```bash
# Preview API document
curl -X POST http://localhost:19090/api/erp-ai/preview \
  -F "file=@yonsuite-api.json" \
  -H "Authorization: Bearer $TOKEN"

# Generate and deploy
curl -X POST http://localhost:19090/api/erp-ai/adapt-deploy \
  -F "file=@yonsuite-api.json" \
  -F "erpSystem=YonSuite" \
  -H "Authorization: Bearer $TOKEN"
```

## Generated Code Structure

```
com.nexusarchive.integration.erp.adapter.<erp-type>/
├── <ErpType>ErpAdapter.java       # Main adapter class
├── dto/
│   ├── ApiRequest.java            # Request DTOs
│   └── ApiResponse.java           # Response DTOs
└── <ErpType>ErpAdapterTest.java   # Unit tests
```

## Manual Implementation Steps

After template generation, implement the TODO items:

1. **HTTP Client Setup**: Implement API calls to ERP system
2. **Authentication**: Add required authentication headers
3. **Data Mapping**: Map ERP responses to internal DTOs
4. **Error Handling**: Add proper exception handling
5. **Testing**: Complete unit tests with real API mocks
```

**Step 2: Update BATCH_DEVELOPMENT_STATUS.md**

Search for AI references and update the status:

```bash
# Check for AI references
grep -n "AI\|DeepSeek\|GLM" docs/BATCH_DEVELOPMENT_STATUS.md
```

Update relevant sections to reflect the simplified architecture.

**Step 3: Commit**

```bash
git add docs/
git commit -m "docs(ai): update documentation for simplified ERP adapter system"
```

---

## Summary

After completing all tasks:
- **~15 files deleted** (LLM clients, prompt builders, parsers, metrics, sessions)
- **3 files modified** (orchestrator, controller, configuration)
- **2 documentation files updated**
- **~2000+ lines of code removed**
- **Zero external API dependencies**
- **Simplified, maintainable architecture**

The system now relies on the proven template-based code generator, which produces clean, predictable code that can be manually completed by developers. This approach:
- Reduces complexity
- Eliminates external API costs
- Improves code quality
- Makes the system more maintainable
- Provides developers with full control over implementation details
