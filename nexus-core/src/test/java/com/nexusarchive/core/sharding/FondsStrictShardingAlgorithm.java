// Input: ShardingSphere 标准分片接口 (5.4.1 StandardShardingAlgorithm)
// Output: 全宗隔离强制分片算法（Precise 返回单目标）
// Pos: NexusCore Sharding POC
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.sharding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

public final class FondsStrictShardingAlgorithm implements StandardShardingAlgorithm<String> {
    @Override
    public String getType() {
        return "FONDS_STRICT";
    }

    @Override
    public String doSharding(
            Collection<String> availableTargetNames,
            PreciseShardingValue<String> shardingValue) {
        if (availableTargetNames == null || availableTargetNames.isEmpty()) {
            throw new IllegalArgumentException("availableTargetNames must not be empty");
        }
        if (shardingValue == null || shardingValue.getValue() == null) {
            throw new IllegalArgumentException("fonds_no must not be null");
        }
        String fondsNo = shardingValue.getValue();
        if (fondsNo.isBlank()) {
            throw new IllegalArgumentException("fonds_no must not be blank");
        }
        List<String> targets = new ArrayList<>(availableTargetNames);
        Collections.sort(targets);
        int index = Math.floorMod(fondsNo.hashCode(), targets.size());
        return targets.get(index);
    }

    @Override
    public Collection<String> doSharding(
            Collection<String> availableTargetNames,
            RangeShardingValue<String> shardingValue) {
        throw new UnsupportedOperationException("Range sharding is blocked for fonds isolation");
    }
}
