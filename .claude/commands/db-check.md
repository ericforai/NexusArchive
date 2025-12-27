# Database Connection Check

Verify PostgreSQL database configuration and connectivity.

## Diagnostic Steps

### 1. Check Configuration

```bash
# View datasource config (mask password)
grep -A 10 "datasource:" nexusarchive-java/src/main/resources/application.yml
grep -A 10 "datasource:" nexusarchive-java/src/main/resources/application-dev.yml
```

### 2. Test Connection

```bash
# If using Docker
docker ps | grep postgres

# Direct connection test
docker exec -it postgres psql -U nexusarchive -d nexusarchive -c "SELECT version();"

# Or with local psql
psql -h localhost -U nexusarchive -d nexusarchive -c "SELECT version();"
```

### 3. Check Common Issues

| Issue | Symptom | Fix |
|-------|---------|-----|
| Container not running | Connection refused | `docker-compose up -d postgres` |
| Wrong credentials | Authentication failed | Check `.env` and `application.yml` |
| Database not created | Database does not exist | Create DB or run init script |
| Port conflict | Address already in use | Change port or stop conflicting service |
| Network issue | Host unreachable | Check Docker network config |

### 4. Verify Flyway Migrations

```bash
# Check migration status
cd nexusarchive-java && mvn flyway:info

# List migration files
ls -la nexusarchive-java/src/main/resources/db/migration/
```

### 5. Redis Connection (if caching enabled)

```bash
# Check Redis container
docker ps | grep redis

# Test connection
docker exec -it redis redis-cli ping
```

## Usage

Run `/db-check` when encountering database connection issues.
