import { render, screen } from '@testing-library/react';
import { notFound } from 'next/navigation';
import { getSession } from '@/lib/auth/session';
import { createMockUser } from '@/test/test-utils';
import SourcesLayout, { metadata } from './layout';

vi.mock('@/lib/auth/session', () => ({
  getSession: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  notFound: vi.fn(() => {
    throw new Error('NEXT_NOT_FOUND');
  }),
}));

describe('SourcesLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('is titled Sources', () => {
    expect(metadata.title).toBe('Sources');
  });

  it.each([['OPERATOR'], ['ADMIN']])('renders the page for %s', async (role) => {
    vi.mocked(getSession).mockResolvedValue(createMockUser({ roles: [role] }));

    render(await SourcesLayout({ children: <p>catalog</p> }));

    expect(screen.getByText('catalog')).toBeInTheDocument();
    expect(notFound).not.toHaveBeenCalled();
  });

  it('is a 404 for readers', async () => {
    vi.mocked(getSession).mockResolvedValue(createMockUser({ roles: ['USER'] }));

    await expect(SourcesLayout({ children: <p>catalog</p> })).rejects.toThrow('NEXT_NOT_FOUND');
    expect(notFound).toHaveBeenCalled();
  });

  it('is a 404 without a session', async () => {
    vi.mocked(getSession).mockResolvedValue(null);

    await expect(SourcesLayout({ children: <p>catalog</p> })).rejects.toThrow('NEXT_NOT_FOUND');
  });
});
