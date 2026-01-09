# Service Layer Caching Strategy

## Overview

This document describes the caching strategy applied to the Service layer to reduce database load and improve response times for frequently accessed data.

## Cache Infrastructure

- **Cache Provider**: Redis
- **Cache Manager**: `RedisCacheManager` with transaction-aware caching
- **Configuration**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/config/RedisConfig.java`
- **Serialization**: JSON (GenericJackson2JsonRedisSerializer)
- **Null Values**: Disabled (null values are not cached)

## Cache Namespaces

| Namespace | TTL | Purpose |
|-----------|-----|---------|
| `permissions` | 1 hour | User permissions cache |
| `roles` | 1 hour | Role data cache |
| `stats` | 5 minutes | Statistical data cache |
| `fonds` | 30 minutes | Fonds (archive) data cache |
| `users` | 15 minutes | User info cache |
| `loginAttempts` | 15 minutes | Login attempt records |
| `rateLimit` | 1 minute | Rate limiting counters |
| `archiveStats` | 10 minutes | Archive statistics |
| `menuCache` | 1 hour | Menu data |
| `dictCache` | 2 hours | Data dictionary |
| `systemConfig` | 30 minutes | System configuration |
| `orgTree` | 30 minutes | Organization tree |
| `fondsScope` | 30 minutes | Fonds permission scope |
| `erpConfig` | 30 minutes | ERP configuration |
| `entityConfig` | 30 minutes | Entity configuration |
| `default` | 30 minutes | Default cache |

## Cached Services

### 1. RoleService

**File**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/RoleService.java`

| Method | Cache Type | Key Pattern | Description |
|--------|------------|-------------|-------------|
| `getAllRoles()` | `@Cacheable` | `roles:all` | Get all roles |
| `getRoleById(String id)` | `@Cacheable` | `roles:id:{id}` | Get role by ID |
| `getRoleByCode(String code)` | `@Cacheable` | `roles:code:{code}` | Get role by code |
| `createRole(Role)` | `@CacheEvict` | `allEntries=true` | Clear all roles cache on create |
| `updateRole(String, Role)` | `@CacheEvict` | `allEntries=true` | Clear all roles cache on update |
| `deleteRole(String)` | `@CacheEvict` | `allEntries=true` | Clear all roles cache on delete |
| `getPermissions()` | `@Cacheable` | `permissions:all` | Get all permissions |

### 2. PermissionService

**File**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/PermissionService.java`

| Method | Cache Type | Key Pattern | Description |
|--------|------------|-------------|-------------|
| `listAll()` | `@Cacheable` | `permissions:all` | Get all permissions |
| `create(Permission)` | `@CacheEvict` | `allEntries=true` | Clear cache on create |
| `update(String, Permission)` | `@CacheEvict` | `allEntries=true` | Clear cache on update |
| `delete(String)` | `@CacheEvict` | `allEntries=true` | Clear cache on delete |

### 3. SystemSettingService

**File**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/SystemSettingService.java`

| Method | Cache Type | Key Pattern | Description |
|--------|------------|-------------|-------------|
| `listAll()` | `@Cacheable` | `systemConfig:all` | Get all system settings |
| `saveAll(List)` | `@CacheEvict` | `allEntries=true` | Clear cache on save |

### 4. OrgService

**File**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/OrgService.java`

| Method | Cache Type | Key Pattern | Description |
|--------|------------|-------------|-------------|
| `getTree()` | `@Cacheable` | `orgTree:tree` | Get organization tree |
| `create(Org)` | `@CacheEvict` | `allEntries=true` | Clear cache on create |
| `update(String, Org)` | `@CacheEvict` | `allEntries=true` | Clear cache on update |
| `delete(String)` | `@CacheEvict` | `allEntries=true` | Clear cache on delete |
| `createBatch(List)` | `@CacheEvict` | `allEntries=true` | Clear cache on batch create |
| `importFromFile(MultipartFile)` | `@CacheEvict` | `allEntries=true` | Clear cache on import |

### 5. FondsScopeService

**File**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/FondsScopeService.java`

| Method | Cache Type | Key Pattern | Description |
|--------|------------|-------------|-------------|
| `getAllowedFonds(String userId)` | `@Cacheable` | `fondsScope:user:{userId}` | Get user's allowed fonds |

### 6. ErpConfigServiceImpl

**File**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ErpConfigServiceImpl.java`

| Method | Cache Type | Key Pattern | Description |
|--------|------------|-------------|-------------|
| `findConfigsByErpType(String)` | `@Cacheable` | `erpConfig:type:{type}` | Get configs by ERP type |
| `findById(Long)` | `@Cacheable` | `erpConfig:id:{id}` | Get config by ID |
| `getAllConfigs()` | `@Cacheable` | `erpConfig:all` | Get all configs |
| `saveConfig(ErpConfig)` | `@CacheEvict` | `allEntries=true` | Clear cache on save |
| `deleteConfig(Long)` | `@CacheEvict` | `allEntries=true` | Clear cache on delete |

### 7. EntityConfigServiceImpl

**File**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/EntityConfigServiceImpl.java`

| Method | Cache Type | Key Pattern | Description |
|--------|------------|-------------|-------------|
| `getConfigsByEntityId(String)` | `@Cacheable` | `entityConfig:entity:{id}` | Get configs by entity |
| `getConfigsGroupedByType(String)` | `@Cacheable` | `entityConfig:grouped:{id}` | Get grouped configs |
| `saveOrUpdateConfig(...)` | `@CacheEvict` | `entityConfig:entity:{id}` | Clear cache on save |
| `deleteConfigsByEntityId(String, String)` | `@CacheEvict` | `entityConfig:entity:{id}` | Clear cache on delete |

### 8. BasFondsServiceImpl

**File**: `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/BasFondsServiceImpl.java`

| Method | Cache Type | Key Pattern | Description |
|--------|------------|-------------|-------------|
| `canModifyFondsCode(String)` | `@Cacheable` | `fonds:modifiable:{code}` | Check if fonds can be modified |
| `updateFonds(BasFonds)` | `@CacheEvict` | `fonds:modifiable:{code}` | Clear cache on update |

## Cache Key Design

### Key Pattern Conventions

1. **Static keys**: Use single quotes for literal strings: `key = "'all'"`
2. **Parameter-based keys**: Use SpEL: `key = "'id:' + #id"`
3. **Composite keys**: Use SpEL: `key = "'user:' + #userId + ':' + #fondsCode"`

### Examples

```java
// Static key
@Cacheable(value = "roles", key = "'all'")
public List<Role> getAllRoles() { ... }

// Single parameter
@Cacheable(value = "roles", key = "'id:' + #id")
public Role getRoleById(String id) { ... }

// Multiple parameters (hypothetical)
@Cacheable(value = "archive", key = "'fonds:' + #fondsCode + ':user:' + #userId")
public List<Archive> getArchives(String fondsCode, String userId) { ... }
```

## Cache Eviction Strategies

1. **All-entries eviction**: Used when data changes affect the entire cache namespace
   ```java
   @CacheEvict(value = "roles", allEntries = true)
   public Role createRole(Role role) { ... }
   ```

2. **Key-based eviction**: Used when only specific entries need to be cleared
   ```java
   @CacheEvict(value = "fondsScope", key = "'entity:' + #entityId")
   public void deleteConfigsByEntityId(String entityId, String configType) { ... }
   ```

## Cache Error Handling

The `CacheErrorHandler` in `RedisConfig` is configured to fail gracefully:

- **Get errors**: Returns `null` (fallback to database)
- **Put/Evict errors**: Silently ignored (business logic continues)
- **Clear errors**: Silently ignored

This ensures that cache failures never break application functionality.

## Usage Guidelines

### When to Use Caching

- **Read-heavy operations**: Data that is read far more often than written
- **Expensive queries**: Queries that involve joins or complex calculations
- **Reference data**: Lookup tables, configuration data, permissions
- **Hierarchical data**: Organization trees, menus

### When NOT to Use Caching

- **Security-sensitive data**: User passwords, tokens (use `users` cache with caution)
- **Real-time data**: Statistics that must always be current
- **Frequently changing data**: Data that changes more often than it's read
- **Validation results**: Business rule validations must always be fresh

### Best Practices

1. **Always pair read caching with write eviction**: If you cache reads, clear cache on writes
2. **Use appropriate TTL**: Balance performance with data freshness
3. **Avoid caching null values**: Prevents cache pollution (already configured)
4. **Use descriptive cache keys**: Makes debugging easier
5. **Consider cache warming**: For critical data, preload caches on startup

## Monitoring and Troubleshooting

### Check Cache Hit Rate

```bash
# Redis CLI
redis-cli -h localhost -p 16379
> INFO stats
> KEYS roles:*
> GET roles:all
```

### Clear Specific Cache Namespace

```bash
# Redis CLI
> KEYS systemConfig:*
> DEL systemConfig:all
```

## Future Enhancements

1. **Cache warming on startup**: Pre-populate critical caches
2. **Conditional caching**: Use `@Cacheable` with `condition` or `unless`
3. **Multi-layer caching**: Consider Caffeine (local) + Redis (distributed)
4. **Cache metrics**: Integrate with Micrometer for monitoring

---

**Last Updated**: 2026-01-09
**Related Files**:
- `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/config/RedisConfig.java`
- `/Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/`
