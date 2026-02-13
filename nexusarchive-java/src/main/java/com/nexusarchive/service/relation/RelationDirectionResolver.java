// Input: 关系图中心节点与边
// Output: DirectionalView 上下游分层结果
// Pos: service/relation 关系方向解析器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.relation;

import com.nexusarchive.dto.relation.RelationEdgeDto;
import com.nexusarchive.dto.relation.RelationGraphDto;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Component
public class RelationDirectionResolver {

    public RelationGraphDto.DirectionalView resolve(String centerId, List<RelationEdgeDto> edges) {
        if (centerId == null || centerId.isBlank() || edges == null || edges.isEmpty()) {
            return RelationGraphDto.DirectionalView.builder()
                    .upstream(List.of())
                    .downstream(List.of())
                    .layers(Map.of())
                    .mainline(List.of())
                    .build();
        }

        Map<String, List<String>> outgoing = new HashMap<>();
        Map<String, List<String>> incoming = new HashMap<>();
        for (RelationEdgeDto edge : edges) {
            if (edge == null || edge.getFrom() == null || edge.getTo() == null) continue;
            outgoing.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>()).add(edge.getTo());
            incoming.computeIfAbsent(edge.getTo(), k -> new ArrayList<>()).add(edge.getFrom());
        }

        Map<String, Integer> upstreamDepths = bfs(incoming, centerId);
        Map<String, Integer> downstreamDepths = bfs(outgoing, centerId);

        Set<String> upstream = new LinkedHashSet<>();
        Set<String> downstream = new LinkedHashSet<>();
        Map<String, Integer> layers = new HashMap<>();

        Set<String> allIds = new HashSet<>();
        allIds.addAll(upstreamDepths.keySet());
        allIds.addAll(downstreamDepths.keySet());
        allIds.remove(centerId);

        for (String id : allIds) {
            Integer up = upstreamDepths.get(id);
            Integer down = downstreamDepths.get(id);

            if (up != null && down != null) {
                if (up <= down) {
                    upstream.add(id);
                    layers.put(id, up);
                } else {
                    downstream.add(id);
                    layers.put(id, down);
                }
                continue;
            }
            if (up != null) {
                upstream.add(id);
                layers.put(id, up);
                continue;
            }
            if (down != null) {
                downstream.add(id);
                layers.put(id, down);
            }
        }

        return RelationGraphDto.DirectionalView.builder()
                .upstream(sortByLayerThenId(upstream, layers))
                .downstream(sortByLayerThenId(downstream, layers))
                .layers(layers)
                .mainline(List.of())
                .build();
    }

    private Map<String, Integer> bfs(Map<String, List<String>> adjacency, String centerId) {
        Map<String, Integer> depths = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.offer(centerId);
        depths.put(centerId, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depths.getOrDefault(current, 0);
            for (String next : adjacency.getOrDefault(current, List.of())) {
                if (depths.containsKey(next)) continue;
                depths.put(next, currentDepth + 1);
                queue.offer(next);
            }
        }
        return depths;
    }

    private List<String> sortByLayerThenId(Set<String> ids, Map<String, Integer> layers) {
        return ids.stream()
                .sorted((a, b) -> {
                    int layerCmp = Integer.compare(layers.getOrDefault(a, 1), layers.getOrDefault(b, 1));
                    if (layerCmp != 0) return layerCmp;
                    return a.compareTo(b);
                })
                .toList();
    }
}

