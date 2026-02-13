package com.nexusarchive.service.relation;

import com.nexusarchive.dto.relation.RelationEdgeDto;
import com.nexusarchive.dto.relation.RelationGraphDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class RelationDirectionResolverTest {

    private final RelationDirectionResolver resolver = new RelationDirectionResolver();

    @Test
    @DisplayName("应基于有向边识别上下游并保持单节点唯一归属")
    void shouldResolveDirectionalViewByDirectedEdges() {
        String center = "JZ-1";
        List<RelationEdgeDto> edges = List.of(
            RelationEdgeDto.builder().from("FP-1").to(center).relationType("ORIGINAL_VOUCHER").build(),
            RelationEdgeDto.builder().from(center).to("FK-1").relationType("CASH_FLOW").build(),
            RelationEdgeDto.builder().from("FK-1").to("HD-1").relationType("CASH_FLOW").build(),
            RelationEdgeDto.builder().from("SQ-1").to("FP-1").relationType("BASIS").build()
        );

        RelationGraphDto.DirectionalView view = resolver.resolve(center, edges);

        assertTrue(view.getUpstream().contains("FP-1"));
        assertTrue(view.getUpstream().contains("SQ-1"));
        assertTrue(view.getDownstream().contains("FK-1"));
        assertTrue(view.getDownstream().contains("HD-1"));
        assertFalse(view.getUpstream().contains("FK-1"));
        assertFalse(view.getDownstream().contains("FP-1"));
    }

    @Test
    @DisplayName("空边集应返回空 directionalView")
    void shouldReturnEmptyDirectionalViewWhenNoEdges() {
        RelationGraphDto.DirectionalView view = resolver.resolve("JZ-1", List.of());
        assertEquals(0, view.getUpstream().size());
        assertEquals(0, view.getDownstream().size());
        assertEquals(0, view.getLayers().size());
    }
}

