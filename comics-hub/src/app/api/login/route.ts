import { NextResponse, type NextRequest } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE, COOKIE_MAX_AGE, GRAPHQL_ENDPOINT } from '@/lib/auth/constants';
import { loginSchema } from '@/lib/validations/auth';

export async function POST(request: NextRequest) {
  const body = await request.json();

  const parsed = loginSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? 'Invalid input' },
      { status: 400 },
    );
  }

  const { username, password, rememberMe } = parsed.data;

  const res = await fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: `mutation Login($input: LoginInput!) {
        login(input: $input) {
          token
          refreshToken
          username
          displayName
        }
      }`,
      variables: { input: { username, password } },
    }),
  });

  const json = await res.json();

  if (json.errors) {
    return NextResponse.json(
      { error: json.errors[0]?.message ?? 'Login failed' },
      { status: 401 },
    );
  }

  const data = json.data?.login;
  if (!data?.token) {
    return NextResponse.json({ error: 'Login failed' }, { status: 401 });
  }

  const cookieOptions = {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax' as const,
    path: '/',
    ...(rememberMe ? { maxAge: COOKIE_MAX_AGE } : {}),
  };

  const response = NextResponse.json({
    user: { username: data.username, displayName: data.displayName },
  });

  response.cookies.set(JWT_COOKIE, data.token, cookieOptions);
  response.cookies.set(REFRESH_COOKIE, data.refreshToken, cookieOptions);

  return response;
}
