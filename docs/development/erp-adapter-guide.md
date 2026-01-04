# ERP Adapter Integration Guide

> **Version**: 2.0.0
> **Updated**: 2026-01-04
> **Module**: `integration.erp`

## Overview

NexusArchive provides a simplified, template-based ERP adapter system that automatically generates integration code from OpenAPI specifications. This system removes the complexity of AI-driven generation while maintaining powerful code generation capabilities.

**Key Features:**
- ✅ Template-based code generation (no AI required)
- ✅ Automatic scenario mapping from API patterns
- ✅ One-command deployment workflow
- ✅ Database auto-registration
- ✅ Built-in testing framework

## Architecture

### Components

```
integration/erp/
├── adapter/              # Core adapter interfaces and implementations
│   ├── ErpAdapter.java           # Core adapter interface
│   └── impl/                     # Specific ERP implementations
├── ai/                   # Code generation system
│   ├── controller/               # REST API endpoints
│   ├── parser/                   # OpenAPI document parser
│   ├── mapper/                   # Business scenario mapper
│   ├── generator/                # Template-based code generator
│   ├── deploy/                   # Auto-deployment service
│   └── agent/                    # Legacy AI stubs (deprecated)
├── annotation/           # Metadata annotations
├── registry/             # Runtime metadata registry
└── dto/                  # Data transfer objects
```

### Workflow

```
┌─────────────────┐
│ Upload OpenAPI  │  JSON/YAML specification
└────────┬────────┘
         ▼
┌─────────────────┐
│ Parse Document  │  Extract API definitions
└────────┬────────┘
         ▼
┌─────────────────┐
│ Map Scenarios   │  Match to standard scenarios
└────────┬────────┘
         ▼
┌─────────────────┐
│ Generate Code   │  Template-based generation
└────────┬────────┘
         ▼
┌─────────────────┐
│ Auto Deploy     │  Save, compile, test, register
└─────────────────┘
```

## Quick Start

### Prerequisites

- Backend service running at `http://localhost:19090`
- Valid JWT authentication token
- OpenAPI 3.0 specification (JSON or YAML format)

### Simple Example

```bash
# 1. Create OpenAPI specification
cat > openapi.json << 'EOF'
{
  "openapi": "3.0.0",
  "info": {
    "title": "Kingdee Cloud API",
    "version": "1.0.0"
  },
  "paths": {
    "/api/v1/vouchers": {
      "get": {
        "operationId": "listVouchers",
        "summary": "Get voucher list",
        "tags": ["vouchers"]
      }
    }
  }
}
EOF

# 2. Generate and deploy adapter
curl -X POST "http://localhost:19090/api/erp-ai/deploy" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "files=@openapi.json" \
  -F "erpType=kingdee" \
  -F "erpName=Kingdee Cloud"

# 3. Adapter is automatically deployed and ready to use
```

## REST API Reference

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/erp-ai/adapt` | POST | Generate adapter code from OpenAPI |
| `/api/erp-ai/deploy` | POST | Generate and auto-deploy adapter |

### 1. Generate Adapter Code

**Endpoint**: `POST /api/erp-ai/adapt`

**Parameters**:
- `files` (required): OpenAPI specification file(s)
- `erpType` (required): ERP identifier (e.g., `kingdee`, `yonsuite`)
- `erpName` (required): ERP system display name

**Example Request**:

```bash
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "files=@kingdee-api.json" \
  -F "erpType=kingdee" \
  -F "erpName=Kingdee Cloud"
```

**Success Response**:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "success": true,
    "code": {
      "adapterClass": "// Generated adapter code...",
      "className": "KingdeeErpAdapter",
      "packageName": "com.nexusarchive.integration.erp.adapter.kingdee",
      "erpType": "kingdee",
      "erpName": "Kingdee Cloud",
      "dtoClasses": [
        {
          "className": "VoucherDto",
          "packageName": "com.nexusarchive.integration.erp.dto",
          "code": "// DTO class code..."
        }
      ],
      "testClass": "// Test class code...",
      "configSql": "-- SQL configuration script..."
    },
    "mappings": [
      {
        "scenario": "VOUCHER_SYNC",
        "apiPath": "/api/v1/vouchers",
        "method": "GET",
        "confidence": "HIGH"
      }
    ],
    "adapterId": "kingdee",
    "message": "ERP adaptation completed"
  }
}
```

### 2. Generate and Auto-Deploy

**Endpoint**: `POST /api/erp-ai/deploy`

**Features**:
1. ✅ Saves code to source directory
2. ✅ Compiles and verifies
3. ✅ Runs automated tests
4. ✅ Registers in database
5. ⚠️ Hot-reload (requires manual restart in MVP)

**Parameters**: Same as `/api/erp-ai/adapt`

**Example Request**:

```bash
curl -X POST "http://localhost:19090/api/erp-ai/deploy" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "files=@kingdee-api.json" \
  -F "erpType=kingdee" \
  -F "erpName=Kingdee Cloud"
```

**Success Response**:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "success": true,
    "code": {
      "adapterClass": "// Generated adapter code...",
      "className": "KingdeeErpAdapter",
      "packageName": "com.nexusarchive.integration.erp.adapter.kingdee"
    },
    "mappings": [...],
    "adapterId": "kingdee",
    "deploymentResult": {
      "success": true,
      "stepsCompleted": [
        "✅ Code saved to: .../KingdeeErpAdapter.java",
        "✅ Compilation successful",
        "✅ Tests passed: 2 tests",
        "✅ Database registration successful",
        "⚠️ Hot-reload failed (manual restart required)"
      ],
      "errors": [],
      "adapterPath": "/path/to/KingdeeErpAdapter.java",
      "className": "KingdeeErpAdapter"
    },
    "message": "ERP adapter generated and deployed successfully"
  }
}
```

## How It Works

### Step 1: Document Parsing

**Input**: OpenAPI JSON/YAML file

**Processing**:
- Parse using Swagger Parser
- Extract API endpoint definitions
- Parse paths, HTTP methods, operation IDs, summaries, parameters, request/response schemas

**Output**: List of `OpenApiDefinition` objects

**Example**:

```json
// Input OpenAPI
{
  "paths": {
    "/api/v1/vouchers": {
      "get": {
        "operationId": "listVouchers",
        "summary": "Get voucher list",
        "parameters": [...]
      }
    }
  }
}

// Output OpenApiDefinition
{
  "path": "/api/v1/vouchers",
  "method": "GET",
  "operationId": "listVouchers",
  "summary": "Get voucher list",
  "parameters": [...]
}
```

### Step 2: Scenario Mapping

**Input**: `OpenApiDefinition` list

**Processing**:
- Analyze API paths, operation IDs, summaries, and tags
- Match to predefined standard scenarios using pattern matching
- Calculate confidence scores

**Supported Dimensions**:

| Dimension | Values |
|-----------|--------|
| **Operation Type** | QUERY, SYNC, SUBMIT, CALLBACK, NOTIFY |
| **Business Object** | ACCOUNTING_VOUCHER, INVOICE, RECEIPT, CONTRACT, ATTACHMENT, ACCOUNT_BALANCE |
| **Trigger Timing** | REALTIME, BATCH, SCHEDULED |
| **Data Flow** | INBOUND (ERP → Archive), OUTBOUND (Archive → ERP) |

**Output**: `ScenarioMapping` list

**Example**:

```java
// API: GET /api/v1/vouchers
// Mapping result:
{
  "scenario": "VOUCHER_SYNC",
  "apiPath": "/api/v1/vouchers",
  "method": "GET",
  "confidence": "HIGH",
  "intent": {
    "operationType": "QUERY",
    "businessObject": "ACCOUNTING_VOUCHER",
    "triggerTiming": "REALTIME",
    "dataFlowDirection": "INBOUND"
  }
}
```

### Step 3: Code Generation

**Input**: `ScenarioMapping` list + ERP type/name

**Generated Components**:

1. **Adapter Class**: Main adapter implementing `ErpAdapter` interface
2. **DTO Classes**: Data transfer objects
3. **Test Class**: JUnit test framework
4. **SQL Config**: Database configuration script

**Example Output**:

```java
@ErpAdapter(
    identifier = "kingdee",
    name = "Kingdee Cloud",
    supportedScenarios = {"VOUCHER_SYNC", "INVOICE_SYNC"}
)
public class KingdeeErpAdapter implements ErpAdapter {
    @Override
    public List<VoucherDto> fetchVouchers(LocalDateTime start, LocalDateTime end) {
        // Generated implementation code
    }
}
```

## Supported Scenarios

The system automatically detects and maps APIs to standard scenarios:

| Scenario Code | Description | Typical API Pattern |
|---------------|-------------|---------------------|
| `VOUCHER_SYNC` | Accounting voucher sync | `GET /api/vouchers` |
| `INVOICE_SYNC` | Invoice sync | `GET /api/invoices` |
| `RECEIPT_SYNC` | Receipt sync | `GET /api/receipts` |
| `CONTRACT_SYNC` | Contract sync | `GET /api/contracts` |
| `ATTACHMENT_SYNC` | Attachment sync | `GET /api/attachments` |
| `BALANCE_QUERY` | Account balance query | `GET /api/balances` |
| `VOUCHER_SUBMIT` | Voucher submission | `POST /api/vouchers` |
| `INVOICE_SUBMIT` | Invoice submission | `POST /api/invoices` |
| `VOUCHER_CALLBACK` | Voucher callback | `POST /api/callbacks/voucher` |
| `NOTIFY_STATUS` | Status notification | `POST /api/notifications` |

## Deployment Process

The auto-deployment workflow performs these steps:

### 1. Code Generation
- Generates adapter class based on templates
- Creates supporting DTOs
- Generates test class

### 2. File Persistence
- Saves to `src/main/java/com/nexusarchive/integration/erp/adapter/{erpType}/`
- Creates package directories if needed

### 3. Compilation
- Runs `mvn compile -q`
- Timeout: 120 seconds
- Failures block deployment

### 4. Testing
- Runs `mvn test -Dtest={ClassName}Test -q`
- Failures generate warnings but don't block deployment

### 5. Database Registration
- Inserts into `sys_erp_config` table
- Failures block deployment

### 6. Hot-Reload (MVP Limitation)
- Attempts Spring DevTools restart
- May require manual server restart

## Troubleshooting

### Common Issues

#### Issue 1: File Format Error

**Symptom**: `400 Bad Request` - "File processing failed"

**Causes**:
- Invalid JSON/YAML format
- Non-compliant OpenAPI 3.0 specification

**Solutions**:

```bash
# Validate JSON format
cat openapi.json | jq .

# Use online validator
# https://validator.swagger.io/
```

#### Issue 2: Unrecognized Scenarios

**Symptom**: Empty `mappings` or `UNKNOWN` scenarios

**Causes**:
- API paths/IDs/summaries lack keywords
- Business object not in supported list

**Solutions**:

```json
// Ensure clear naming in OpenAPI spec
{
  "operationId": "syncVouchers",     // ✅ Contains "Voucher"
  "summary": "Sync accounting vouchers", // ✅ Clear description
  "tags": ["voucher"]                // ✅ Relevant tags
}
```

#### Issue 3: Authentication Failure

**Symptom**: `401 Unauthorized`

**Solution**:

```bash
# 1. Login to get token
TOKEN=$(curl -X POST "http://localhost:19090/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.token')

# 2. Use token in API calls
curl -X POST "http://localhost:19090/api/erp-ai/adapt" \
  -H "Authorization: Bearer $TOKEN" \
  ...
```

#### Issue 4: Compilation Failure

**Symptom**: Deployment fails at compilation step

**Solutions**:
- Check generated code for syntax errors
- Verify all dependencies are available
- Review compilation logs in response

#### Issue 5: Database Registration Failure

**Symptom**: Deployment fails at database step

**Solutions**:
- Verify database connection
- Check if `erpType` already exists
- Review SQL error messages

### Debugging

#### Enable Detailed Logging

**application.yml**:

```yaml
logging:
  level:
    com.nexusarchive.integration.erp: DEBUG
```

**View Logs**:

```bash
# Backend logs
tail -f nexusarchive-java/logs/application.log | grep "ErpAdaptation"

# Example output:
# DEBUG ErpAdaptationOrchestrator - Starting ERP adaptation: erpType=kingdee
# DEBUG OpenApiDocumentParser - Parsing file: kingdee-api.json
# DEBUG BusinessSemanticMapper - Mapping scenario: /api/v1/vouchers → VOUCHER_SYNC
```

#### Test Individual Components

```bash
# Test parser only
mvn test -Dtest=OpenApiDocumentParserTest

# Test mapper only
mvn test -Dtest=BusinessSemanticMapperTest

# Test generator only
mvn test -Dtest=ErpAdapterCodeGeneratorTest

# Test deployment
mvn test -Dtest=ErpAutoDeploymentServiceTest
```

## Manual Adapter Development

If automatic generation doesn't meet your needs, you can manually create adapters:

### 1. Implement ErpAdapter Interface

```java
package com.nexusarchive.integration.erp.adapter.impl;

import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import com.nexusarchive.integration.erp.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@ErpAdapterAnnotation(
    identifier = "custom",
    name = "Custom ERP",
    description = "Custom ERP adapter",
    version = "1.0.0",
    erpType = "CUSTOM",
    supportedScenarios = {"VOUCHER_SYNC"},
    supportsWebhook = false,
    priority = 50
)
@Service("custom")
public class CustomErpAdapter implements ErpAdapter {

    @Override
    public String getIdentifier() {
        return "custom";
    }

    @Override
    public String getName() {
        return "Custom ERP";
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        // Implement connection test
        return ConnectionTestResult.success();
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        // Implement voucher sync logic
        return List.of();
    }

    // Implement other required methods...
}
```

### 2. Write Tests

```java
@SpringBootTest
class CustomErpAdapterTest {

    @Autowired
    private ErpAdapterFactory factory;

    @Test
    void shouldRegisterCustomErpAdapter() {
        ErpAdapter adapter = factory.getAdapter("custom");
        ErpMetadata metadata = factory.getMetadata("custom");

        assertNotNull(adapter);
        assertEquals("Custom ERP", metadata.getName());
        assertTrue(metadata.getSupportedScenarios().contains("VOUCHER_SYNC"));
    }
}
```

### 3. Run Tests

```bash
cd nexusarchive-java
mvn test -Dtest=CustomErpAdapterTest
mvn test -Dtest=ArchitectureTest  # Verify architecture rules
```

## Best Practices

### 1. OpenAPI Specification

```yaml
# ✅ Good: Clear naming
openapi: 3.0.0
info:
  title: Kingdee Cloud API
  version: 1.0.0
paths:
  /api/v1/vouchers:
    get:
      operationId: listVouchers  # Descriptive
      summary: List accounting vouchers  # Clear
      tags:
        - vouchers  # Relevant

# ❌ Bad: Vague naming
paths:
  /api/v1/data:
    get:
      operationId: getData  # Not descriptive
      summary: Get data  # Unclear
      tags:
        - api  # Too generic
```

### 2. Error Handling

```java
@Override
public ConnectionTestResult testConnection(ErpConfig config) {
    try {
        callErpApi(config);
        return ConnectionTestResult.success();
    } catch (ConnectException e) {
        return ConnectionTestResult.failure("Connection failed: " + e.getMessage());
    } catch (AuthenticationException e) {
        return ConnectionTestResult.failure("Auth failed: " + e.getMessage());
    }
}
```

### 3. Logging

```java
private static final Logger log = LoggerFactory.getLogger(CustomErpAdapter.class);

@Override
public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
    log.info("Starting voucher sync: ERP={}, startDate={}, endDate={}",
        config.getErpType(), startDate, endDate);

    try {
        List<VoucherDTO> vouchers = doSync(config, startDate, endDate);
        log.info("Voucher sync completed: {} vouchers", vouchers.size());
        return vouchers;
    } catch (Exception e) {
        log.error("Voucher sync failed", e);
        throw new ErpSyncException("Voucher sync failed", e);
    }
}
```

## Architecture Tests

ArchUnit automatically verifies:

1. ✅ All adapters have `@ErpAdapterAnnotation` annotation
2. ✅ `@ErpAdapterAnnotation.identifier()` values are unique
3. ✅ Adapter layer doesn't directly depend on service layer
4. ✅ Metadata registry only depends on annotations and DTOs

Violations will cause CI/CD failures.

## Migration from AI-Based System

If you were using the previous AI-based system:

1. **API Endpoints**: The `/api/erp-ai/deploy` endpoint remains the same
2. **Code Generation**: Now uses templates instead of AI
3. **Deployment**: Auto-deployment process is unchanged
4. **Database**: No schema changes required

**Breaking Changes**:
- Removed AI-specific configuration (API keys, model selection)
- Removed iterative refinement workflow
- Removed PDF/Markdown parsing (scheduled for future)

## Technical Details

- **Module Location**: `com.nexusarchive.integration.erp`
- **Test Coverage**: Comprehensive unit and integration tests
- **Documentation Updated**: 2026-01-04
- **Version**: 2.0.0 (Simplified)

## Additional Resources

- [Manual Adapter Development Guide](/docs/architecture/erp-adapter-development-guide.md)
- [ERP Integration Configuration](/docs/guides/ERP集成配置.md)
- [Module Manifest](/nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/README.md)
