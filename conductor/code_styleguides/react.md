# React Code Style Guide

## 1. Component Structure
- Use functional components with hooks.
- PascalCase for component names (e.g., `UserProfile.tsx`).
- Colocate related files (component, styles, tests) in the same directory.

## 2. Hooks
- Use custom hooks to abstract logic from UI.
- Follow the rules of hooks (only call at the top level).

## 3. State Management
- Use local state (`useState`) for UI-only state.
- Use Zustand for global client state.
- Use React Query for server state.

## 4. Styling
- Use Tailwind CSS utility classes.
- Use `clsx` or `classnames` for conditional styling.

## 5. Performance
- Use `useMemo` and `useCallback` for expensive calculations or reference stability.
- Use React.lazy for code splitting.
