import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { XinchuangPartnersSection } from '../XinchuangPartnersSection';

describe('XinchuangPartnersSection', () => {
  it('renders partner names', () => {
    render(<XinchuangPartnersSection />);
    expect(screen.getByText('KylinSoft')).toBeInTheDocument();
  });
});
