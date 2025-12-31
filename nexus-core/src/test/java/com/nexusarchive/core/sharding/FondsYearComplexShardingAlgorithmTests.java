// Input: FondsYearComplexShardingAlgorithm
// Output: 双键分片算法回归验证
// Pos: NexusCore Sharding POC 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.sharding;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FondsYearComplexShardingAlgorithmTests {
    private final FondsYearComplexShardingAlgorithm algorithm = new FondsYearComplexShardingAlgorithm();
    private final List<String> targets = Arrays.asList("ds_0", "ds_1");

    @Test
    void shouldRouteWithFondsAndYear() {
        ComplexKeysShardingValue<Comparable<?>> value = newValue("F001", 2024);
        Collection<String> actual = algorithm.doSharding(targets, value);
        assertEquals(1, actual.size());
        assertTrue(targets.contains(actual.iterator().next()));
    }

    @Test
    void shouldRejectMissingFondsNo() {
        ComplexKeysShardingValue<Comparable<?>> value = newValue(null, 2024);
        assertThrows(IllegalArgumentException.class, () -> algorithm.doSharding(targets, value));
    }

    @Test
    void shouldRejectMissingFiscalYear() {
        ComplexKeysShardingValue<Comparable<?>> value = newValue("F001", null);
        assertThrows(IllegalArgumentException.class, () -> algorithm.doSharding(targets, value));
    }

    private ComplexKeysShardingValue<Comparable<?>> newValue(String fondsNo, Integer fiscalYear) {
        Map<String, Collection<Comparable<?>>> values = new HashMap<>();
        if (fondsNo != null) {
            List<Comparable<?>> fondsValues = new java.util.ArrayList<>();
            fondsValues.add(fondsNo);
            values.put("fonds_no", fondsValues);
        }
        if (fiscalYear != null) {
            List<Comparable<?>> yearValues = new java.util.ArrayList<>();
            yearValues.add(fiscalYear);
            values.put("fiscal_year", yearValues);
        }
        return new ComplexKeysShardingValue<>("arc_account_item", values, Map.of());
    }
}
