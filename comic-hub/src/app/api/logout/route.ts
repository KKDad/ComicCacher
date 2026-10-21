import { NextResponse } from 'next/server';

import { JWT_COOKIE, REFRESH_COOKIE, REMEMBER_COOKIE } from '@/lib/auth/constants';
import { serverGraphql } from '@/lib/auth/graphql-server';
import { LogoutDocument } from '@/generated/graphql';

export async function POST() {
  // Best-effort backend revocation. If this fails (network, expired token,
  // backend unreachable) we still clear client cookies — the alternative is
  // leaving the client logged in, which is worse.
  try {
    await serverGraphql(LogoutDocument.toString());
  } catch (error) {
    console.warn('Logout mutation failed; clearing client cookies anyway:', error);
  }

  const response = NextResponse.json({ success: true });

  response.cookies.set(JWT_COOKIE, '', {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: 0,
  });
  response.cookies.set(REFRESH_COOKIE, '', {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: 0,
  });
  response.cookies.set(REMEMBER_COOKIE, '', {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: 0,
  });

  return response;
}
