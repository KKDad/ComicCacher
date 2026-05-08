import { gql } from 'graphql-request';
import { NextResponse } from 'next/server';

import { JWT_COOKIE, REFRESH_COOKIE } from '@/lib/auth/constants';
import { getAuthenticatedClient } from '@/lib/auth/graphql-server';

const LOGOUT_MUTATION = gql`
  mutation Logout {
    logout
  }
`;

export async function POST() {
  // Best-effort backend revocation. If this fails (network, expired token,
  // backend unreachable) we still clear client cookies — the alternative is
  // leaving the client logged in, which is worse.
  try {
    const client = await getAuthenticatedClient();
    await client.request(LOGOUT_MUTATION);
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

  return response;
}
