一旦我所属的文件夹有所变化，请更新我。

# Performance Testing Guide

This directory contains performance benchmark documentation and reports for NexusArchive.

## Overview

Performance benchmarks measure critical system operations including:
- Database query performance (pagination vs full query)
- Cache hit rates and latency
- DTO conversion overhead
- Concurrent access patterns

## Running Benchmarks

### Quick Start

```bash
cd /Users/user/nexusarchive/nexusarchive-java

# Run all performance tests
mvn test -Dtest=PerformanceBenchmarkTest

# Run specific benchmark test
mvn test -Dtest=PerformanceBenchmarkTest#testQueryPerformanceBaseline
mvn test -Dtest=PerformanceBenchmarkTest#testCachePerformance
mvn test -Dtest=PerformanceBenchmarkTest#testDtoConversionPerformance
mvn test -Dtest=PerformanceBenchmarkTest#testConcurrentQueryPerformance
mvn test -Dtest=PerformanceBenchmarkTest#testLargeDatasetPagination
```

### Running with Profile

```bash
# With test profile (default)
mvn test -Dtest=PerformanceBenchmarkTest -Dspring.profiles.active=test

# With specific database
mvn test -Dtest=PerformanceBenchmarkTest -Dspring.profiles.active=dev
```

### JMH Microbenchmark (Advanced)

For true microbenchmarking with JVM warmup and optimization:

```bash
# Create a separate JMH project structure
mkdir -p jmh/src/main/java
cp src/test/java/com/nexusarchive/performance/*.java jmh/src/main/java/

# Build JMH jar
mvn clean package -DskipTests

# Run with JMH options
java -jar target/benchmarks.jar -wi 3 -i 5 -f 1
```

## Benchmark Tests

### 1. Query Performance Baseline

Measures pagination and search query performance.

```bash
mvn test -Dtest=PerformanceBenchmarkTest#testQueryPerformanceBaseline
```

**Output metrics:**
- Page query time for different page sizes (10/20/50/100)
- Full query time (with limit)
- Search query time with keyword filter

### 2. Cache Performance

Measures Redis cache read/write performance and hit rates.

```bash
mvn test -Dtest=PerformanceBenchmarkTest#testCachePerformance
```

**Output metrics:**
- Average write time per operation
- Average read time per operation
- Cache hit rate percentage
- Database query vs cache read comparison

### 3. DTO Conversion Performance

Measures Entity-to-DTO transformation overhead.

```bash
mvn test -Dtest=PerformanceBenchmarkTest#testDtoConversionPerformance
```

**Output metrics:**
- Manual field mapping time
- Batch conversion time
- Average time per record

### 4. Concurrent Query Performance

Measures multi-threaded query performance and scaling.

```bash
mvn test -Dtest=PerformanceBenchmarkTest#testConcurrentQueryPerformance
```

**Output metrics:**
- Single thread baseline time
- Multi-thread total time
- Speedup ratio
- Average per-thread time

### 5. Large Dataset Pagination

Measures pagination performance at different offsets.

```bash
mvn test -Dtest=PerformanceBenchmarkTest#testLargeDatasetPagination
```

**Output metrics:**
- Query time for various offsets (0/100/500/1000/5000/10000)
- Records returned per query

## Performance Targets

| Operation | P50 Target | P95 Target | P99 Target |
|-----------|------------|------------|------------|
| API Response (single record) | <50 ms | <100 ms | <200 ms |
| API Response (paginated list) | <100 ms | <200 ms | <500 ms |
| Search Query | <200 ms | <500 ms | <1000 ms |
| Cache Read | <200 μs | <500 μs | <1 ms |

## Reports

- **[2026-01-09 Benchmark Report](./2026-01-09-benchmark-report.md)** - Initial baseline and optimization strategies

## Best Practices

1. **Run in isolation** - Close other applications to get accurate measurements
2. **Warm up the JVM** - Run tests multiple times; first run is usually slower
3. **Use consistent data** - Ensure database has similar data volume across runs
4. **Monitor system resources** - Check CPU, memory, and disk I/O during tests
5. **Compare across environments** - Run on dev, staging, and production-like setups

## Troubleshooting

### Test fails with "Redis not configured"

Redis is optional for performance tests. Cache tests will be skipped if Redis is not available.

```bash
# Start Redis for full test coverage
docker-compose -f docker-compose.infra.yml up -d redis
```

### Slow test execution

- Reduce data volume in test database
- Adjust page sizes and iteration counts in the test
- Use `-DskipTests` for compilation-only verification

### OutOfMemoryError

```bash
# Increase Maven JVM memory
export MAVEN_OPTS="-Xmx2G -Xms1G"
mvn test -Dtest=PerformanceBenchmarkTest
```

## Contributing

When adding new benchmarks:

1. Create a new test method following the existing pattern
2. Add appropriate logging for metrics
3. Document expected performance targets
4. Update this README with new test information

## References

- [JMH Documentation](https://openjdk.org/projects/code-tools/jmh/)
- [Spring Boot Performance Tuning](https://spring.io/guides/gs/performance/)
- [PostgreSQL Performance Tips](https://www.postgresql.org/docs/current/performance-tips.html)
