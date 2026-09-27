import LoginLayout, { metadata as login } from './login/layout';
import { metadata as register } from './register/layout';
import { metadata as forgot } from './forgot-password/layout';
import { metadata as reset } from './reset-password/layout';

describe('sign-in page titles', () => {
  it.each([
    [login, 'Sign in'],
    [register, 'Create an account'],
    [forgot, 'Forgot password'],
    [reset, 'Set a new password'],
  ])('sets the page title', (metadata, title) => {
    expect(metadata.title).toBe(title);
  });

  it('renders its children unchanged', () => {
    expect(LoginLayout({ children: 'x' })).toBe('x');
  });
});
