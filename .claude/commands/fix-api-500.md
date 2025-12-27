# Fix API 500 Error

Trace and fix HTTP 500 errors from backend API.

## Diagnostic Steps

### 1. Collect Error Context

- What endpoint returned 500? (URL, method, request body)
- When did it start happening?
- Is it consistent or intermittent?

### 2. Check Backend Logs

```bash
# Recent errors in backend log
tail -200 nexusarchive-java/backend.log | grep -A 20 "ERROR\|Exception"

# Or if running with docker
docker logs nexusarchive-backend 2>&1 | tail -200 | grep -A 20 "ERROR"
```

### 3. Trace the Request Path

1. **Controller** (`controller/`): Find the endpoint handler
2. **Service** (`service/impl/`): Check business logic
3. **Mapper** (`mapper/`): Check database queries
4. **Entity** (`entity/`): Check data model

### 4. Common Causes

| Symptom | Likely Cause | Check |
|---------|--------------|-------|
| NullPointerException | Missing null check | Service/mapper return values |
| DataAccessException | DB query failed | Mapper SQL, connection config |
| ValidationException | Invalid input | DTO validation annotations |
| AuthenticationException | Token issue | JWT config, token expiry |
| IOException | File operation failed | Upload paths, permissions |

### 5. Database Check

```bash
# Check PostgreSQL connection
docker exec -it postgres psql -U nexusarchive -c "SELECT 1"

# Check recent queries (if logging enabled)
tail -50 nexusarchive-java/logs/sql.log
```

## Usage

Run `/fix-api-500` with the failing endpoint URL and any error details.
