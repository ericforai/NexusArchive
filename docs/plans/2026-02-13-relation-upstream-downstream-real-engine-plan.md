# Relation Upstream/Downstream Real Engine Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将“上下游识别”从前端固定主线规则升级为后端关系驱动的真实识别，并保证前端仅做展示，不硬编码业务流程。

**Architecture:** 后端 `RelationController` 在输出图谱时额外返回方向化分层信息（upstream/downstream/layer/mainline candidates），由关系类型与有向边共同推导；前端 `RelationshipQueryView + ThreeColumnLayout` 仅消费该结果渲染。原有接口保持兼容，新增字段向后兼容可选读取，逐步替换前端 `detectPaymentMainline` 固定链路逻辑。

**Tech Stack:** Java Spring Boot + MyBatis-Plus, TypeScript + React + Zustand, Vitest, ESLint, Flyway.

---

### Task 1: 定义后端返回契约（DTO）

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/dto/relation/RelationGraphDto.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/controller/RelationControllerTest.java`

**Step 1: Write the failing test**

```java
@Test
void graph_should_include_directional_layers_when_relations_exist() throws Exception {
    // 调用 /relations/{archiveId}/graph
    // 断言 JSON 含 data.directionalView.upstream/downstream 字段
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive && ./mvnw -pl nexusarchive-java -Dtest=RelationControllerTest#graph_should_include_directional_layers_when_relations_exist test`
Expected: FAIL（缺少 `directionalView` 字段）

**Step 3: Write minimal implementation**

```java
public class RelationGraphDto {
  private DirectionalView directionalView;
  public static class DirectionalView {
    private List<String> upstream;
    private List<String> downstream;
    private Map<String, Integer> layers;
    private List<String> mainline;
  }
}
```

**Step 4: Run test to verify it passes**

Run: 同 Step 2
Expected: PASS

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/dto/relation/RelationGraphDto.java \
  nexusarchive-java/src/test/java/com/nexusarchive/controller/RelationControllerTest.java
git commit -m "feat: add directional view fields to relation graph dto"
```

### Task 2: 后端实现真实上下游识别（关系驱动）

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/controller/RelationController.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/relation/RelationDirectionResolver.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/service/relation/RelationDirectionResolverTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_resolve_upstream_downstream_by_directed_edges_and_relation_types() {
  // 构造有向边 + relationType（BASIS/ORIGINAL_VOUCHER/CASH_FLOW/ARCHIVE）
  // 断言不出现同节点同时上下游
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive && ./mvnw -pl nexusarchive-java -Dtest=RelationDirectionResolverTest test`
Expected: FAIL（Resolver 不存在）

**Step 3: Write minimal implementation**

```java
public final class RelationDirectionResolver {
  public DirectionalView resolve(String centerId, List<RelationEdgeDto> edges) {
    // 1) 一跳方向定根
    // 2) BFS outward 继承方向
    // 3) 全局去重，单节点唯一归属
    // 4) 输出层级 map
  }
}
```

**Step 4: Wire controller output**

```java
DirectionalView view = resolver.resolve(finalCenterArchiveId, edges);
builder.directionalView(view);
```

**Step 5: Run tests**

Run: 
- `cd /Users/user/nexusarchive && ./mvnw -pl nexusarchive-java -Dtest=RelationDirectionResolverTest test`
- `cd /Users/user/nexusarchive && ./mvnw -pl nexusarchive-java -Dtest=RelationControllerTest test`
Expected: PASS

**Step 6: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/RelationController.java \
  nexusarchive-java/src/main/java/com/nexusarchive/service/relation/RelationDirectionResolver.java \
  nexusarchive-java/src/test/java/com/nexusarchive/service/relation/RelationDirectionResolverTest.java \
  nexusarchive-java/src/test/java/com/nexusarchive/controller/RelationControllerTest.java
git commit -m "feat: resolve real upstream/downstream on backend"
```

### Task 3: 前端改为消费后端 directionalView（去固定主线硬编码）

**Files:**
- Modify: `src/api/autoAssociation.ts`
- Modify: `src/pages/utilization/RelationshipQueryView.tsx`
- Modify: `src/components/relation-graph/ThreeColumnLayout.tsx`
- Modify: `src/pages/utilization/relationDrilldown.ts`
- Test: `src/__tests__/pages/utilization/relationDrilldown.test.ts`

**Step 1: Write the failing test**

```ts
it('prefers backend directionalView and does not force fixed payment flow', () => {
  // 给出 directionalView: upstream/downstream
  // 断言渲染不再依赖固定 voucher->payment->application... 链
});
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive && npx vitest run src/__tests__/pages/utilization/relationDrilldown.test.ts`
Expected: FAIL（当前依赖固定主线）

**Step 3: Write minimal implementation**

```ts
// relationDrilldown.ts
export function resolveDirectionalView(graph, fallbackLocalResolver) {
  if (graph.directionalView) return graph.directionalView;
  return fallbackLocalResolver(graph);
}
```

```tsx
// RelationshipQueryView.tsx
// 优先使用后端 directionalView；仅后端缺失时再 fallback
```

```tsx
// ThreeColumnLayout.tsx
// 传入已定向 upstream/downstream，组件只做渲染+展开
```

**Step 4: Run tests**

Run:
- `cd /Users/user/nexusarchive && npx vitest run src/__tests__/pages/utilization/relationDrilldown.test.ts`
- `cd /Users/user/nexusarchive && npx eslint src/pages/utilization/RelationshipQueryView.tsx src/components/relation-graph/ThreeColumnLayout.tsx src/pages/utilization/relationDrilldown.ts`
Expected: PASS

**Step 5: Commit**

```bash
git add src/api/autoAssociation.ts src/pages/utilization/RelationshipQueryView.tsx \
  src/components/relation-graph/ThreeColumnLayout.tsx src/pages/utilization/relationDrilldown.ts \
  src/__tests__/pages/utilization/relationDrilldown.test.ts
git commit -m "refactor: frontend consumes backend directional relations"
```

### Task 4: Mock 与真实数据共存策略（不污染业务）

**Files:**
- Modify: `src/api/autoAssociation.ts`
- Modify: `nexusarchive-java/src/main/resources/db/migration/V2026021301__align_purchase_demo_closed_loop.sql`
- Test: `src/__tests__/pages/utilization/relationDrilldown.test.ts`

**Step 1: Write failing test**

```ts
it('demo fallback should not override real backend directional result', () => {
  // 后端返回 directionalView 时，mock fallback 不生效
});
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive && npx vitest run src/__tests__/pages/utilization/relationDrilldown.test.ts`
Expected: FAIL

**Step 3: Minimal implementation**

```ts
if (response.data?.data?.directionalView) return response.data.data;
// fallback 仅在后端无数据/404 时启用
```

**Step 4: Run tests**

Run: 同 Step 2
Expected: PASS

**Step 5: Commit**

```bash
git add src/api/autoAssociation.ts \
  nexusarchive-java/src/main/resources/db/migration/V2026021301__align_purchase_demo_closed-loop.sql \
  src/__tests__/pages/utilization/relationDrilldown.test.ts
git commit -m "fix: isolate demo fallback from real directional graph"
```

### Task 5: 端到端验证与文档

**Files:**
- Modify: `src/pages/utilization/README.md`
- Modify: `src/components/relation-graph/README.md`
- Modify: `src/store/README.md`
- Create: `docs/plans/2026-02-13-relation-upstream-downstream-real-engine-qa.md`

**Step 1: Write verification checklist**

```md
- [ ] 凭证中心时，同节点不重复出现在上下游
- [ ] 非标准流程（非采购）仍可正确分流
- [ ] 后端 directionalView 优先
- [ ] fallback 仅在后端缺失时启用
```

**Step 2: Run full verification**

Run:
- `cd /Users/user/nexusarchive && npx vitest run`
- `cd /Users/user/nexusarchive && npm run -s typecheck`
- `cd /Users/user/nexusarchive && npx eslint src`
Expected: PASS（若失败，记录风险清单）

**Step 3: Commit docs**

```bash
git add src/pages/utilization/README.md src/components/relation-graph/README.md src/store/README.md \
  docs/plans/2026-02-13-relation-upstream-downstream-real-engine-qa.md
git commit -m "docs: add directional relation architecture and QA checklist"
```

## Skill References
- `@writing-plans`
- `@test-driven-development`
- `@verification-before-completion`
- `@systematic-debugging`

