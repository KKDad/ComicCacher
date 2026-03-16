import { NextResponse, type NextRequest } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE, COOKIE_MAX_AGE, GRAPHQL_ENDPOINT } from '@/lib/auth/constants';
import { registerSchema } from '@/lib/validations/auth';

export async function POST(request: NextRequest) {
  const body = await request.json();

  const parsed = registerSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0].message },
      { status: 400 },
    );
  }

  const { username, email, displayName, password } = parsed.data;

  const res = await fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: `mutation Register($input: RegisterInput!) {
        register(input: $input) {
          token
          refreshToken
          username
          displayName
        }
      }`,
      variables: { input: { username, email, displayName, password } },
    }),
  });

  const json = await res.json();

  if (json.errors) {
    return NextResponse.json(
      { error: json.errors[0].message },
      { status: 400 },
    );
  }

  const data = json.data?.register;
  if (!data?.token) {
    return NextResponse.json({ error: 'Registration failed' }, { status: 400 });
  }

  const response = NextResponse.json({
    user: { username: data.username, displayName: data.displayName },
  });

  response.cookies.set(JWT_COOKIE, data.token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: COOKIE_MAX_AGE,
  });
  response.cookies.set(REFRESH_COOKIE, data.refreshToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: COOKIE_MAX_AGE,
  });

  return response;
}
