// Input: ShardingSphere 复合分片接口（fonds_no + fiscal_year）
// Output: 双键分片算法（单目标路由）
// Pos: NexusCore Sharding POC 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.sharding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

public final class FondsYearComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm<Comparable<?>> {
    @Override
    public String getType() {
        return "FONDS_YEAR_STRICT";
    }

    @Override
    public Collection<String> doSharding(
            Collection<String> availableTargetNames,
            ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        if (availableTargetNames == null || availableTargetNames.isEmpty()) {
            throw new IllegalArgumentException("availableTargetNames must not be empty");
        }
        if (shardingValue == null) {
            throw new IllegalArgumentException("shardingValue must not be null");
        }
        if (!shardingValue.getColumnNameAndRangeValuesMap().isEmpty()) {
            throw new UnsupportedOperationException("Range sharding is blocked for fonds/year isolation");
        }
        String fondsNo = extractSingleValue(shardingValue, "fonds_no");
        String fiscalYear = extractSingleValue(shardingValue, "fiscal_year");
        String routingKey = fondsNo + "_" + fiscalYear;
        List<String> targets = new ArrayList<>(availableTargetNames);
        Collections.sort(targets);
        int index = Math.floorMod(routingKey.hashCode(), targets.size());
        return Collections.singleton(targets.get(index));
    }

    private String extractSingleValue(ComplexKeysShardingValue<Comparable<?>> shardingValue, String columnName) {
        Map<String, Collection<Comparable<?>>> valuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        Collection<Comparable<?>> values = valuesMap.get(columnName);
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Missing sharding column: " + columnName);
        }
        if (values.size() > 1) {
            throw new IllegalArgumentException("Multiple values for sharding column: " + columnName);
        }
        Comparable<?> value = values.iterator().next();
        if (value == null) {
            throw new IllegalArgumentException(columnName + " must not be null");
        }
        String text = Objects.toString(value, "").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(columnName + " must not be blank");
        }
        return text;
    }
}
