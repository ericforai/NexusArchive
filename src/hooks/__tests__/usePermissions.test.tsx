// Input: React hooks testing library, React Query, auth store
// Output: usePermissionsQuery hook tests
// Pos: Unit tests for usePermissions hook

import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { usePermissionsQuery } from '../usePermissions';
import { useAuthStore } from '@/store/useAuthStore';
import { client } from '@/api/client';

vi.mock('@/api/client', () => ({
  client: {
    get: vi.fn(),
  },
}));

describe('usePermissionsQuery', () => {
  let queryClient: QueryClient;
  let wrapper: React.FC<{ children: React.ReactNode }>;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    useAuthStore.getState().logout();
  });

  it('should accept unwrapped permissions payloads and update store', async () => {
    useAuthStore.getState().login('token', {
      id: 'user-1',
      username: 'admin',
      roles: [],
      permissions: [],
    });

    vi.mocked(client.get).mockResolvedValue({
      data: {
        permissions: ['nav:all', 'nav:portal'],
        roles: [{ code: 'super_admin' }],
      },
    } as any);

    renderHook(() => usePermissionsQuery(), { wrapper });

    await waitFor(() => {
      const { user } = useAuthStore.getState();
      expect(user?.permissions).toEqual(['nav:all', 'nav:portal']);
    });

    expect(useAuthStore.getState().user?.roles).toEqual(['super_admin']);
  });
});
