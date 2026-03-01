import { NextResponse } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE } from '@/lib/auth/constants';

export async function POST() {
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

  return response;
}
