// Input: JUnit + BouncyCastle
// Output: SM3 vs SHA256 性能基准测试
// Pos: NexusCore tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class Sm3BenchmarkTests {
    private static final int WARM_UP_ROUNDS = 3;
    private static final int BENCHMARK_ROUNDS = 10;

    private final Sm3HashService hashService = new Sm3HashService();

    @Test
    void benchmarkSm3VsSha256() {
        System.out.println("\n=== SM3 vs SHA256 Performance Benchmark ===\n");
        System.out.printf("%-12s | %-15s | %-15s | %-10s%n", "Size", "SM3 (ms)", "SHA256 (ms)", "Ratio");
        System.out.println("-".repeat(60));

        benchmarkSize("1 KB", 1024);
        benchmarkSize("10 KB", 10 * 1024);
        benchmarkSize("100 KB", 100 * 1024);
        benchmarkSize("1 MB", 1024 * 1024);
        benchmarkSize("10 MB", 10 * 1024 * 1024);

        System.out.println("\n=== Benchmark Complete ===\n");
    }

    private void benchmarkSize(String label, int sizeBytes) {
        byte[] data = generateRandomData(sizeBytes);

        // Warm up
        for (int i = 0; i < WARM_UP_ROUNDS; i++) {
            hashService.hashSm3(data);
            hashService.hashSha256(data);
        }

        // Benchmark SM3
        long sm3TotalNanos = 0;
        for (int i = 0; i < BENCHMARK_ROUNDS; i++) {
            long start = System.nanoTime();
            String hash = hashService.hashSm3(data);
            sm3TotalNanos += System.nanoTime() - start;
            assertNotNull(hash);
        }
        double sm3AvgMs = TimeUnit.NANOSECONDS.toMicros(sm3TotalNanos / BENCHMARK_ROUNDS) / 1000.0;

        // Benchmark SHA256
        long sha256TotalNanos = 0;
        for (int i = 0; i < BENCHMARK_ROUNDS; i++) {
            long start = System.nanoTime();
            String hash = hashService.hashSha256(data);
            sha256TotalNanos += System.nanoTime() - start;
            assertNotNull(hash);
        }
        double sha256AvgMs = TimeUnit.NANOSECONDS.toMicros(sha256TotalNanos / BENCHMARK_ROUNDS) / 1000.0;

        double ratio = sm3AvgMs / sha256AvgMs;
        System.out.printf("%-12s | %-15.3f | %-15.3f | %-10.2fx%n", label, sm3AvgMs, sha256AvgMs, ratio);
    }

    private byte[] generateRandomData(int size) {
        byte[] data = new byte[size];
        new SecureRandom().nextBytes(data);
        return data;
    }
}
