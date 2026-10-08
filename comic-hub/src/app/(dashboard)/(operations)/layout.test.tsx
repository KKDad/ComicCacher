import { render, screen } from '@testing-library/react';
import { notFound } from 'next/navigation';
import { getSession } from '@/lib/auth/session';
import { createMockUser } from '@/test/test-utils';
import OperationsLayout from './layout';

vi.mock('@/lib/auth/session', () => ({
  getSession: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  notFound: vi.fn(() => {
    throw new Error('NEXT_NOT_FOUND');
  }),
}));

describe('OperationsLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it.each([['OPERATOR'], ['ADMIN']])('renders the page for %s', async (role) => {
    vi.mocked(getSession).mockResolvedValue(createMockUser({ roles: [role] }));

    render(await OperationsLayout({ children: <p>operations</p> }));

    expect(screen.getByText('operations')).toBeInTheDocument();
    expect(notFound).not.toHaveBeenCalled();
  });

  it('is a 404 for readers', async () => {
    vi.mocked(getSession).mockResolvedValue(createMockUser({ roles: ['USER'] }));

    await expect(OperationsLayout({ children: <p>operations</p> })).rejects.toThrow('NEXT_NOT_FOUND');
    expect(notFound).toHaveBeenCalled();
  });

  it('is a 404 without a session', async () => {
    vi.mocked(getSession).mockResolvedValue(null);

    await expect(OperationsLayout({ children: <p>operations</p> })).rejects.toThrow('NEXT_NOT_FOUND');
  });
});
