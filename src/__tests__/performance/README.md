# Performance Tests

Performance testing suite for verifying virtualization and rendering efficiency of data-heavy components.

## Overview

This directory contains performance tests for validating that components handle large datasets efficiently using virtualization techniques.

## Test Files

| Test File | Component | Purpose |
|-----------|-----------|---------|
| `BatchTable.performance.test.ts` | BatchTable | Validates pagination-based virtualization for batch list rendering |

## Performance Benchmarks

### BatchTable Performance Targets

| Dataset Size | Target Render Time | Max DOM Nodes | Notes |
|--------------|-------------------|---------------|-------|
| 10 records | < 500ms | < 1000 | Small dataset, no pagination needed |
| 100 records | < 1000ms | < 1500 | Medium dataset with pagination |
| 1000 records | < 1000ms | < 1500 | Large dataset with pagination |
| 10000 records | < 2000ms | < 1500 | Extra large dataset with pagination |

### Key Performance Indicators

1. **First Render Time**: Time to initially render the component
2. **Re-render Time**: Time to update after state changes
3. **DOM Node Count**: Total DOM nodes (should stay constant with pagination)
4. **Memory Usage**: Estimated memory footprint
5. **Render Efficiency**: Records rendered per millisecond

## Running Tests

### Run all performance tests
```bash
npm run test src/__tests__/performance
```

### Run a specific test file
```bash
npm run test src/__tests__/performance/BatchTable.performance.test.ts
```

### Run with coverage (not recommended for perf tests)
```bash
npm run test:coverage src/__tests__/performance
```

### Run in watch mode (development)
```bash
npm run test -- src/__tests__/performance --watch
```

## Mock Data Generation

The test suite includes utilities for generating mock data:

```typescript
import { generateMockBatches, createMockProps } from './BatchTable.performance.test';

// Generate 100 mock ArchiveBatch records
const batches = generateMockBatches(100);

// Generate complete mock props for BatchTable
const props = createMockProps({ batches, total: 100 });
```

## Writing New Performance Tests

When adding performance tests for new components:

1. **Use describe blocks for dataset sizes**: Small, Medium, Large, Extra Large
2. **Measure render time**: Use `performance.now()` before and after render
3. **Count DOM nodes**: Use `container.querySelectorAll('*').length`
4. **Test state changes**: Re-renders, page changes, filters
5. **Validate virtualization**: Ensure DOM nodes don't grow with data size
6. **Add console logs**: Output metrics for analysis

### Template

```typescript
describe('ComponentName Performance Tests', () => {
    describe('Small Dataset (10 records)', () => {
        it('should render efficiently', () => {
            const startTime = performance.now();
            render(<ComponentName data={generateData(10)} />);
            const endTime = performance.now();

            expect(endTime - startTime).toBeLessThan(500);
        });
    });

    describe('Virtualization Validation', () => {
        it('should only render visible rows', () => {
            const { container } = render(<ComponentName data={generateData(10000)} />);
            const domNodes = container.querySelectorAll('*').length;

            // Should be constant regardless of data size
            expect(domNodes).toBeLessThan(2000);
        });
    });
});
```

## Performance Optimization Guidelines

If tests fail, consider these optimizations:

1. **Implement Pagination**: Only render current page data
2. **Use React.memo**: Prevent unnecessary re-renders
3. **Use useMemo/useCallback**: Memoize expensive computations
4. **Virtual Scrolling**: For very large lists, use react-window or react-virtual
5. **Lazy Loading**: Load data on demand
6. **Debounce Input**: Delay expensive operations

## CI/CD Integration

Performance tests can be integrated into CI/CD pipelines:

```yaml
# .github/workflows/performance.yml
- name: Run performance tests
  run: npm run test:run src/__tests__/performance

- name: Upload performance results
  uses: actions/upload-artifact@v3
  with:
    name: performance-results
    path: test-results/
```

## Troubleshooting

### Test fails due to slow render time
- Check if component is re-rendering unnecessarily
- Verify pagination is working correctly
- Use React DevTools Profiler to identify bottlenecks

### Test fails due to high DOM node count
- Verify virtualization is implemented
- Check if all rows are being rendered instead of just visible ones
- Ensure pagination is limiting rendered items

### Memory leaks detected
- Check for event listeners not being cleaned up
- Verify useEffect cleanup functions
- Check for stale closures in event handlers

## References

- [Vitest Performance Testing](https://vitest.dev/guide/features.html#benchmark)
- [React Performance Optimization](https://react.dev/learn/render-and-commit)
- [Web Performance APIs](https://developer.mozilla.org/en-US/docs/Web/API/Performance)
