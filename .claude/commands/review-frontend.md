# Review Frontend

Review React/TypeScript frontend code for issues and improvements.

## Checklist

1. **Component Structure**
   - Functional components with hooks
   - Props properly typed with TypeScript
   - No inline styles (use Tailwind/Ant Design)

2. **State Management**
   - Zustand for global state
   - React Query for server state
   - Local state only when necessary

3. **API Integration**
   - Use centralized API client from `src/api/`
   - Proper error handling
   - Loading states handled

4. **Performance**
   - No unnecessary re-renders
   - `useMemo`/`useCallback` where beneficial
   - Lazy loading for routes

5. **Accessibility**
   - Semantic HTML elements
   - ARIA labels where needed
   - Keyboard navigation support

6. **Testing**
   - Components have corresponding tests
   - Testing Library best practices

## Usage

Provide the file path or describe the component to review.
