// Input: React, Testing Library, Vitest
// Output: BaseModal 单元测试
// Pos: src/components/modals/__tests__/BaseModal.test.tsx

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { BaseModal } from '../BaseModal';

describe('BaseModal', () => {
  it('should not render when isOpen is false', () => {
    render(
      <BaseModal isOpen={false} onClose={vi.fn()}>
        <div>Content</div>
      </BaseModal>
    );
    expect(screen.queryByText('Content')).not.toBeInTheDocument();
  });

  it('should render when isOpen is true', () => {
    render(
      <BaseModal isOpen={true} onClose={vi.fn()}>
        <div>Content</div>
      </BaseModal>
    );
    expect(screen.queryByText('Content')).toBeInTheDocument();
  });

  it('should call onClose when backdrop is clicked', () => {
    const handleClose = vi.fn();
    render(
      <BaseModal isOpen={true} onClose={handleClose}>
        <div>Content</div>
      </BaseModal>
    );

    const backdrop = screen.getByTestId('modal-backdrop');
    fireEvent.click(backdrop);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('should render title when provided', () => {
    render(
      <BaseModal isOpen={true} onClose={vi.fn()} title="Test Title">
        <div>Content</div>
      </BaseModal>
    );
    expect(screen.getByText('Test Title')).toBeInTheDocument();
  });

  it('should render custom header when header prop is provided', () => {
    render(
      <BaseModal
        isOpen={true}
        onClose={vi.fn()}
        header={<div data-testid="custom-header">Custom Header</div>}
      >
        <div>Content</div>
      </BaseModal>
    );
    expect(screen.getByTestId('custom-header')).toBeInTheDocument();
  });

  it('should not call onClose when clicking modal content', () => {
    const handleClose = vi.fn();
    render(
      <BaseModal isOpen={true} onClose={handleClose} title="Test">
        <div>Content</div>
      </BaseModal>
    );

    const backdrop = screen.getByTestId('modal-backdrop');
    // Click on a child element (not the backdrop itself)
    const content = screen.getByText('Content');
    fireEvent.click(content);
    // Should not call onClose because we stop propagation
    expect(handleClose).not.toHaveBeenCalled();
  });

  it('should not close when closeOnBackdropClick is false', () => {
    const handleClose = vi.fn();
    render(
      <BaseModal isOpen={true} onClose={handleClose} closeOnBackdropClick={false}>
        <div>Content</div>
      </BaseModal>
    );

    const backdrop = screen.getByTestId('modal-backdrop');
    fireEvent.click(backdrop);
    expect(handleClose).not.toHaveBeenCalled();
  });

  it('should render footer when provided', () => {
    render(
      <BaseModal
        isOpen={true}
        onClose={vi.fn()}
        footer={<button data-testid="footer-btn">Footer Button</button>}
      >
        <div>Content</div>
      </BaseModal>
    );
    expect(screen.getByTestId('footer-btn')).toBeInTheDocument();
  });
});
