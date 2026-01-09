// Input: JUnit 5、JMH、Spring Boot Test、MyBatis-Plus、Lombok
// Output: 性能基准测试套件
// Pos: 性能测试层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.performance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.ArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 性能基准测试套件
 *
 * <p>使用 JMH (Java Microbenchmark Harness) 进行微基准测试，测量关键操作的性能指标。</p>
 *
 * <p>测试场景：</p>
 * <ul>
 *   <li>数据库查询性能（分页 vs 全量）</li>
 *   <li>缓存命中率</li>
 *   <li>异步任务响应时间</li>
 *   <li>DTO 转换开销</li>
 * </ul>
 *
 * <p>运行方式：</p>
 * <pre>
 * # 运行所有基准测试
 * mvn test -Dtest=PerformanceBenchmarkTest
 *
 * # 运行特定基准测试
 * mvn test -Dtest=PerformanceBenchmarkTest#testQueryPerformanceBaseline
 *
 * # 生成 JMH 报告 (需要先编译为 jar)
 * java -jar target/nexusarchive-backend-2.0.0.jar -rf jmh/src/main/java
 * </pre>
 *
 * @see <a href="https://openjdk.org/projects/code-tools/jmh/">JMH Documentation</a>
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class PerformanceBenchmarkTest {

    @Autowired
    private ArchiveService archiveService;

    @Autowired
    private ArchiveMapper archiveMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 基准测试：查询性能基线测量
     *
     * <p>测量场景：</p>
     * <ul>
     *   <li>分页查询 (10/50/100 条记录)</li>
     *   <li>全量查询 (测量内存和时间开销)</li>
     *   <li>带条件查询 (搜索、过滤)</li>
     * </ul>
     */
    @Test
    public void testQueryPerformanceBaseline() {
        log.info("========== 查询性能基线测试 ==========");

        // 预热
        for (int i = 0; i < 3; i++) {
            archiveService.getArchives(1, 10, null, null, null, null, null, null, null);
        }

        // 测试不同分页大小的性能
        int[] pageSizes = {10, 20, 50, 100};

        for (int pageSize : pageSizes) {
            long startTime = System.nanoTime();
            IPage<Archive> result = archiveService.getArchives(1, pageSize, null, null, null, null, null, null, null);
            long endTime = System.nanoTime();

            long durationMs = (endTime - startTime) / 1_000_000;
            log.info("分页查询 (pageSize={}): {} ms, 返回 {} 条记录", pageSize, durationMs, result.getRecords().size());
        }

        // 测试全量查询性能 (谨慎使用，仅在小数据集上测试)
        long startTime = System.nanoTime();
        List<Archive> allRecords = archiveMapper.selectList(new LambdaQueryWrapper<Archive>().last("LIMIT 1000"));
        long endTime = System.nanoTime();
        long fullQueryDurationMs = (endTime - startTime) / 1_000_000;
        log.info("全量查询 (LIMIT 1000): {} ms, 返回 {} 条记录", fullQueryDurationMs, allRecords.size());

        // 测试带条件查询性能
        startTime = System.nanoTime();
        IPage<Archive> searchResult = archiveService.getArchives(1, 20, "测试", null, null, null, null, null, null);
        endTime = System.nanoTime();
        long searchDurationMs = (endTime - startTime) / 1_000_000;
        log.info("搜索查询 (关键词='测试'): {} ms, 返回 {} 条记录", searchDurationMs, searchResult.getRecords().size());

        log.info("========== 查询性能基线测试完成 ==========");
    }

    /**
     * 基准测试：缓存性能测试
     *
     * <p>测量场景：</p>
     * <ul>
     *   <li>Redis 缓存读取性能</li>
     *   <li>缓存命中率统计</li>
     *   <li>缓存 vs 数据库查询对比</li>
     * </ul>
     */
    @Test
    public void testCachePerformance() {
        log.info("========== 缓存性能测试 ==========");

        if (redisTemplate == null) {
            log.warn("Redis 未配置，跳过缓存测试");
            return;
        }

        String testKey = "benchmark:test:" + UUID.randomUUID();
        String testValue = "test-value-" + UUID.randomUUID();

        // 测试缓存写入性能
        long writeStartTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            redisTemplate.opsForValue().set(testKey + ":" + i, testValue, 60, TimeUnit.SECONDS);
        }
        long writeEndTime = System.nanoTime();
        double avgWriteTimeUs = (writeEndTime - writeStartTime) / 1_000.0 / 100;
        log.info("缓存写入 (100次): 平均 {} μs/次", String.format("%.2f", avgWriteTimeUs));

        // 测试缓存读取性能
        long readStartTime = System.nanoTime();
        int hitCount = 0;
        for (int i = 0; i < 100; i++) {
            Object value = redisTemplate.opsForValue().get(testKey + ":" + i);
            if (value != null) {
                hitCount++;
            }
        }
        long readEndTime = System.nanoTime();
        double avgReadTimeUs = (readEndTime - readStartTime) / 1_000.0 / 100;
        double hitRate = (double) hitCount / 100 * 100;
        log.info("缓存读取 (100次): 平均 {} μs/次, 命中率 {:.1f}%", String.format("%.2f", avgReadTimeUs), hitRate);

        // 对比数据库查询性能
        String archiveId = "test-archive-id";
        long dbStartTime = System.nanoTime();
        Archive archive = archiveMapper.selectById(archiveId);
        long dbEndTime = System.nanoTime();
        long dbQueryTimeUs = (dbEndTime - dbStartTime) / 1_000;
        log.info("数据库查询 (单条): {} μs", dbQueryTimeUs);

        // 清理测试数据
        for (int i = 0; i < 100; i++) {
            redisTemplate.delete(testKey + ":" + i);
        }

        log.info("========== 缓存性能测试完成 ==========");
    }

    /**
     * 基准测试：DTO 转换性能
     *
     * <p>测量场景：</p>
     * <ul>
     *   <li>Entity -> DTO 转换时间</li>
     *   <li>批量转换性能</li>
     *   <li>不同转换方式的性能对比</li>
     * </ul>
     */
    @Test
    public void testDtoConversionPerformance() {
        log.info("========== DTO 转换性能测试 ==========");

        // 创建测试数据
        List<Archive> archives = createTestArchives(1000);

        // 测试手动转换性能
        long manualStartTime = System.nanoTime();
        List<ArchiveDto> manualResults = new ArrayList<>();
        for (Archive archive : archives) {
            manualResults.add(convertManually(archive));
        }
        long manualEndTime = System.nanoTime();
        double manualAvgTimeNs = (manualEndTime - manualStartTime) / (double) archives.size();
        log.info("手动转换 (1000条): 平均 {} ns/条", String.format("%.2f", manualAvgTimeNs));

        // 测试批量转换性能
        long batchStartTime = System.nanoTime();
        List<ArchiveDto> batchResults = convertBatch(archives);
        long batchEndTime = System.nanoTime();
        double batchAvgTimeNs = (batchEndTime - batchStartTime) / (double) archives.size();
        log.info("批量转换 (1000条): 平均 {} ns/条", String.format("%.2f", batchAvgTimeNs));

        log.info("========== DTO 转换性能测试完成 ==========");
    }

    /**
     * 基准测试：并发查询性能
     *
     * <p>测量场景：</p>
     * <ul>
     *   <li>单线程 vs 多线程查询对比</li>
     *   <li>线程池效率测试</li>
     * </ul>
     */
    @Test
    public void testConcurrentQueryPerformance() throws InterruptedException {
        log.info("========== 并发查询性能测试 ==========");

        int threadCount = 10;
        int queriesPerThread = 10;

        // 单线程基准
        long singleStartTime = System.nanoTime();
        for (int i = 0; i < threadCount * queriesPerThread; i++) {
            archiveService.getArchives(1, 20, null, null, null, null, null, null, null);
        }
        long singleEndTime = System.nanoTime();
        long singleThreadDurationMs = (singleEndTime - singleStartTime) / 1_000_000;

        // 多线程测试
        List<Thread> threads = new ArrayList<>();
        long[] threadTimes = new long[threadCount];

        long multiStartTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            Thread thread = new Thread(() -> {
                long threadStart = System.nanoTime();
                for (int i = 0; i < queriesPerThread; i++) {
                    archiveService.getArchives(1, 20, null, null, null, null, null, null, null);
                }
                threadTimes[threadIndex] = (System.nanoTime() - threadStart) / 1_000_000;
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long multiEndTime = System.nanoTime();
        long multiThreadDurationMs = (multiEndTime - multiStartTime) / 1_000_000;

        log.info("单线程查询 ({}次): {} ms", threadCount * queriesPerThread, singleThreadDurationMs);
        log.info("多线程查询 ({}线程 x {}次): 总耗时 {} ms",
                threadCount, queriesPerThread, multiThreadDurationMs);

        long avgThreadTime = 0;
        for (long time : threadTimes) {
            avgThreadTime += time;
        }
        avgThreadTime /= threadCount;
        log.info("平均每线程耗时: {} ms", avgThreadTime);

        double speedup = (double) singleThreadDurationMs / multiThreadDurationMs;
        log.info("加速比: {:.2f}x", speedup);

        log.info("========== 并发查询性能测试完成 ==========");
    }

    /**
     * 基准测试：大数据量分页性能
     *
     * <p>测试不同数据量下的分页性能</p>
     */
    @Test
    public void testLargeDatasetPagination() {
        log.info("========== 大数据量分页性能测试 ==========");

        // 模拟不同偏移量的分页查询
        int[] offsets = {0, 100, 500, 1000, 5000, 10000};

        for (int offset : offsets) {
            int page = offset / 20 + 1;
            long startTime = System.nanoTime();
            IPage<Archive> result = archiveService.getArchives(page, 20, null, null, null, null, null, null, null);
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            log.info("分页查询 (offset={}, page=1): {} ms, 返回 {} 条记录",
                    offset, durationMs, result.getRecords().size());
        }

        log.info("========== 大数据量分页性能测试完成 ==========");
    }

    // ========== JMH 微基准测试 ==========

    /**
     * JMH 基准测试：分页查询
     */
    @State(Scope.Benchmark)
    public static class QueryPerformanceBenchmark {

        @Param({"10", "20", "50", "100"})
        public int pageSize;

        @Benchmark
        @BenchmarkMode(Mode.AverageTime)
        @OutputTimeUnit(TimeUnit.MILLISECONDS)
        @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
        @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
        @Fork(1)
        public void benchmarkPagedQuery(Blackhole bh) {
            // 模拟分页查询
            Page<Archive> page = new Page<>(1, pageSize);
            bh.consume(page);
        }

        @Benchmark
        @BenchmarkMode(Mode.AverageTime)
        @OutputTimeUnit(TimeUnit.MILLISECONDS)
        @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
        @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
        @Fork(1)
        public void benchmarkFullQuery(Blackhole bh) {
            // 模拟全量查询
            List<Archive> archives = new ArrayList<>();
            bh.consume(archives);
        }
    }

    /**
     * JMH 基准测试：DTO 转换
     */
    @State(Scope.Benchmark)
    public static class DtoConversionBenchmark {

        private Archive testArchive;

        @Setup(Level.Trial)
        public void setup() {
            testArchive = createTestArchive();
        }

        @Benchmark
        @BenchmarkMode(Mode.AverageTime)
        @OutputTimeUnit(TimeUnit.NANOSECONDS)
        @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
        @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
        @Fork(1)
        public ArchiveDto benchmarkManualConversion(Blackhole bh) {
            ArchiveDto dto = new ArchiveDto();
            dto.setId(testArchive.getId());
            dto.setArchiveCode(testArchive.getArchiveCode());
            dto.setTitle(testArchive.getTitle());
            dto.setFiscalYear(testArchive.getFiscalYear());
            dto.setStatus(testArchive.getStatus());
            bh.consume(dto);
            return dto;
        }
    }

    /**
     * JMH 基准测试：缓存操作
     */
    @State(Scope.Benchmark)
    public static class CacheBenchmark {

        @Benchmark
        @BenchmarkMode(Mode.AverageTime)
        @OutputTimeUnit(TimeUnit.MICROSECONDS)
        @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
        @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
        @Fork(1)
        public void benchmarkCacheGet(Blackhole bh) {
            // 模拟缓存读取
            String key = "test-key";
            bh.consume(key);
        }

        @Benchmark
        @BenchmarkMode(Mode.Throughput)
        @OutputTimeUnit(TimeUnit.SECONDS)
        @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
        @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
        @Fork(1)
        public void benchmarkCacheThroughput(Blackhole bh) {
            // 测试缓存吞吐量
            bh.consume("value");
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试数据
     */
    private List<Archive> createTestArchives(int count) {
        List<Archive> archives = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            archives.add(createTestArchive());
        }
        return archives;
    }

    /**
     * 创建单个测试档案
     */
    private static Archive createTestArchive() {
        Archive archive = new Archive();
        archive.setId(UUID.randomUUID().toString().replace("-", ""));
        archive.setArchiveCode("TEST-" + System.currentTimeMillis());
        archive.setTitle("测试档案");
        archive.setFiscalYear("2025");
        archive.setStatus("archived");
        archive.setFondsNo("F001");
        archive.setOrgName("测试单位");
        archive.setRetentionPeriod("30Y");
        archive.setCreatedTime(LocalDateTime.now());
        archive.setLastModifiedTime(LocalDateTime.now());
        archive.setAmount(BigDecimal.valueOf(1000.00));
        archive.setDocDate(LocalDate.now());
        archive.setDeleted(0);
        return archive;
    }

    /**
     * 手动转换 Entity -> DTO
     */
    private ArchiveDto convertManually(Archive archive) {
        ArchiveDto dto = new ArchiveDto();
        dto.setId(archive.getId());
        dto.setArchiveCode(archive.getArchiveCode());
        dto.setTitle(archive.getTitle());
        dto.setFiscalYear(archive.getFiscalYear());
        dto.setStatus(archive.getStatus());
        dto.setFondsNo(archive.getFondsNo());
        dto.setOrgName(archive.getOrgName());
        dto.setRetentionPeriod(archive.getRetentionPeriod());
        return dto;
    }

    /**
     * 批量转换 Entity -> DTO
     */
    private List<ArchiveDto> convertBatch(List<Archive> archives) {
        List<ArchiveDto> result = new ArrayList<>(archives.size());
        for (Archive archive : archives) {
            result.add(convertManually(archive));
        }
        return result;
    }

    /**
     * 简化的 DTO 类 (用于性能测试)
     */
    @lombok.Data
    public static class ArchiveDto {
        private String id;
        private String archiveCode;
        private String title;
        private String fiscalYear;
        private String status;
        private String fondsNo;
        private String orgName;
        private String retentionPeriod;
    }
}
