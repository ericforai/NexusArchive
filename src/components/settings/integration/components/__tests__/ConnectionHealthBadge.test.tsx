import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ConnectionHealthBadge } from '../ConnectionHealthBadge';

describe('ConnectionHealthBadge', () => {
  it('should display healthy status', () => {
    render(<ConnectionHealthBadge status="healthy" lastCheckTime="2025-01-06T10:00:00" />);
    expect(screen.getByText('✅ 正常')).toBeInTheDocument();
  });

  it('should display warning status', () => {
    render(<ConnectionHealthBadge status="warning" lastCheckTime="2025-01-06T10:00:00" />);
    expect(screen.getByText('⚠️ 警告')).toBeInTheDocument();
  });

  it('should display error status', () => {
    render(<ConnectionHealthBadge status="error" lastCheckTime="2025-01-06T10:00:00" />);
    expect(screen.getByText('❌ 异常')).toBeInTheDocument();
  });

  it('should display last check time', () => {
    render(<ConnectionHealthBadge status="healthy" lastCheckTime="2025-01-06T10:00:00" />);
    expect(screen.getByText(/检查于/)).toBeInTheDocument();
  });
});
