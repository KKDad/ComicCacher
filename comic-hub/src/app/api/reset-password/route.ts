import { NextResponse, type NextRequest } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE, COOKIE_MAX_AGE, GRAPHQL_ENDPOINT } from '@/lib/auth/constants';
import { resetPasswordSchema } from '@/lib/validations/auth';

const INVALID_LINK = 'This reset link is invalid or has expired. Request a new one.';

export async function POST(request: NextRequest) {
  const body = await request.json();

  const parsed = resetPasswordSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0].message },
      { status: 400 },
    );
  }

  const { token, password } = parsed.data;

  const res = await fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: `mutation ResetPassword($token: String!, $newPassword: String!) {
        resetPassword(token: $token, newPassword: $newPassword) {
          token
          refreshToken
          username
          displayName
        }
      }`,
      variables: { token, newPassword: password },
    }),
  });

  const json = await res.json();

  // The backend reports every failure as a generic "Password reset failed";
  // in practice that means the token was wrong, used, or expired.
  const data = json.data?.resetPassword;
  if (json.errors || !data?.token) {
    return NextResponse.json({ error: INVALID_LINK }, { status: 400 });
  }

  const response = NextResponse.json({
    user: { username: data.username, displayName: data.displayName },
  });

  const cookieOptions = {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax' as const,
    path: '/',
    maxAge: COOKIE_MAX_AGE,
  };
  response.cookies.set(JWT_COOKIE, data.token, cookieOptions);
  response.cookies.set(REFRESH_COOKIE, data.refreshToken, cookieOptions);

  return response;
}
