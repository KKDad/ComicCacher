import { metadata as login } from './(auth)/login/layout';
import { metadata as register } from './(auth)/register/layout';
import { metadata as forgot } from './(auth)/forgot-password/layout';
import { metadata as reset } from './(auth)/reset-password/layout';
import { metadata as comics } from './(dashboard)/comics/(list)/layout';
import { metadata as preferences } from './(dashboard)/preferences/layout';
import { metadata as daily } from './(reader)/read/layout';
import { metadata as dashboard } from './(dashboard)/page';
import LoginLayout from './(auth)/login/layout';
import { generateMetadata as comicDetailMetadata } from './(dashboard)/comics/[id]/layout';
import ComicDetailLayout from './(dashboard)/comics/[id]/layout';
import { generateMetadata as readerMetadata } from './(reader)/comics/[id]/read/layout';
import { comicTitle } from '@/lib/comic-title';

vi.mock('@/lib/comic-title', () => ({
  comicTitle: vi.fn(),
}));

describe('page titles', () => {
  it.each([
    [login, 'Sign in'],
    [register, 'Create an account'],
    [forgot, 'Reset password'],
    [reset, 'Reset password'],
    [comics, 'Comics'],
    [preferences, 'Preferences'],
    [daily, 'Daily reader'],
    [dashboard, 'Dashboard'],
  ])('sets a static title', (metadata, title) => {
    expect(metadata.title).toBe(title);
  });

  it('titles comic pages with the comic name', async () => {
    vi.mocked(comicTitle).mockResolvedValue('Garfield');
    const params = Promise.resolve({ id: '7' });
    expect((await comicDetailMetadata({ params })).title).toBe('Garfield');
    expect((await readerMetadata({ params })).title).toBe('Garfield');
    expect(comicTitle).toHaveBeenCalledWith('7');
  });

  it('title layouts render their children unchanged', () => {
    expect(LoginLayout({ children: 'x' })).toBe('x');
    expect(ComicDetailLayout({ children: 'y' })).toBe('y');
  });
});
