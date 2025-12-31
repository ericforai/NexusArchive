// Input: FondsStrictShardingAlgorithm (ShardingSphere 5.4.1 API)
// Output: 分片算法行为验证（Precise 返回单目标）
// Pos: NexusCore Sharding POC 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.sharding;

import java.util.Arrays;
import java.util.List;
import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FondsStrictShardingAlgorithmTests {
    private final FondsStrictShardingAlgorithm algorithm = new FondsStrictShardingAlgorithm();
    private final List<String> targets = Arrays.asList("ds_0", "ds_1");

    @Test
    void shouldRouteToDeterministicTarget() {
        PreciseShardingValue<String> value = newPreciseValue("F001");
        String actual = algorithm.doSharding(targets, value);
        assertEquals(actual, algorithm.doSharding(targets, value));
        assertTrue(targets.contains(actual));
    }

    @Test
    void shouldRejectBlankFondsNo() {
        PreciseShardingValue<String> value = newPreciseValue("");
        assertThrows(IllegalArgumentException.class, () -> algorithm.doSharding(targets, value));
    }

    @Test
    void shouldRejectRangeQueries() {
        assertThrows(UnsupportedOperationException.class,
                () -> algorithm.doSharding(targets, (RangeShardingValue<String>) null));
    }

    private PreciseShardingValue<String> newPreciseValue(String fondsNo) {
        DataNodeInfo dataNodeInfo = new DataNodeInfo("ds_", 0, '0');
        return new PreciseShardingValue<>(
                "arc_account_item",
                "fonds_no",
                dataNodeInfo,
                fondsNo);
    }
}
