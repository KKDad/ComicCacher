import { render, screen, fireEvent } from '@testing-library/react';
import { InstallHint, INSTALL_HINT_DISMISSED_KEY } from './install-hint';

const IPHONE_UA =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1';
const ANDROID_UA =
  'Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Mobile Safari/537.36';

const originalUA = navigator.userAgent;

function setUserAgent(ua: string) {
  Object.defineProperty(navigator, 'userAgent', { configurable: true, value: ua });
}

function setStandalone(standalone: boolean) {
  Object.defineProperty(navigator, 'standalone', { configurable: true, value: standalone });
}

const hint = () => screen.queryByText(/add to home screen/i);

describe('InstallHint', () => {
  beforeEach(() => {
    localStorage.clear();
    setUserAgent(IPHONE_UA);
    setStandalone(false);
  });

  afterAll(() => {
    setUserAgent(originalUA);
  });

  it('tells iPhone Safari users how to get full screen', () => {
    render(<InstallHint fullscreenSupported={false} />);
    expect(hint()).toBeInTheDocument();
  });

  it('stays hidden once the app runs from the home screen', () => {
    setStandalone(true);
    render(<InstallHint fullscreenSupported={false} />);
    expect(hint()).not.toBeInTheDocument();
  });

  it('stays hidden where the browser can go full screen', () => {
    render(<InstallHint fullscreenSupported={true} />);
    expect(hint()).not.toBeInTheDocument();
  });

  it('stays hidden off iOS', () => {
    setUserAgent(ANDROID_UA);
    render(<InstallHint fullscreenSupported={false} />);
    expect(hint()).not.toBeInTheDocument();
  });

  it('remembers a dismissal', () => {
    const { unmount } = render(<InstallHint fullscreenSupported={false} />);
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));

    expect(hint()).not.toBeInTheDocument();
    expect(localStorage.getItem(INSTALL_HINT_DISMISSED_KEY)).toBe('1');

    unmount();
    render(<InstallHint fullscreenSupported={false} />);
    expect(hint()).not.toBeInTheDocument();
  });

  it('still shows and dismisses when storage is blocked', () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });

    render(<InstallHint fullscreenSupported={false} />);
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));
    expect(hint()).not.toBeInTheDocument();

    getItem.mockRestore();
    setItem.mockRestore();
  });
});
