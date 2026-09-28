import { NextResponse, type NextRequest } from 'next/server';
import { REMEMBER_COOKIE, GRAPHQL_ENDPOINT } from '@/lib/auth/constants';
import { authCookieOptions, setAuthCookies } from '@/lib/auth/tokens';
import { loginSchema } from '@/lib/validations/auth';
import { LoginDocument } from '@/generated/graphql';

export async function POST(request: NextRequest) {
  const body = await request.json();

  const parsed = loginSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0].message },
      { status: 400 },
    );
  }

  const { username, password, rememberMe } = parsed.data;

  const res = await fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: LoginDocument.toString(),
      variables: { input: { username, password } },
    }),
  });

  const json = await res.json();

  if (json.errors) {
    return NextResponse.json(
      { error: json.errors[0].message },
      { status: 401 },
    );
  }

  const data = json.data?.login;
  if (!data?.token) {
    return NextResponse.json({ error: 'Login failed' }, { status: 401 });
  }

  const response = NextResponse.json({
    user: { username: data.username, displayName: data.displayName },
  });

  setAuthCookies(response.cookies, data.token, data.refreshToken, rememberMe ?? false);

  // Persist the rememberMe preference so token refresh can respect it
  if (rememberMe) {
    response.cookies.set(REMEMBER_COOKIE, '1', authCookieOptions(true));
  } else {
    // Drop one left from an earlier "remember me" login, or token refresh would keep persisting the session
    response.cookies.delete(REMEMBER_COOKIE);
  }

  return response;
}
