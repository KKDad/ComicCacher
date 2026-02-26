import { cookies } from 'next/headers';
import type { User } from '@/types/auth';
import { JWT_COOKIE, GRAPHQL_ENDPOINT } from './constants';

export async function getSession(): Promise<User | null> {
  const cookieStore = await cookies();
  const jwt = cookieStore.get(JWT_COOKIE)?.value;

  if (!jwt) return null;

  try {
    const res = await fetch(GRAPHQL_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${jwt}`,
      },
      body: JSON.stringify({
        query: `query GetMe {
          me {
            username
            email
            displayName
            created
            lastLogin
            roles
          }
        }`,
      }),
      // Don't cache — session should be fresh on each server render
      cache: 'no-store',
    });

    if (!res.ok) return null;

    const json = await res.json();
    return json.data?.me ?? null;
  } catch {
    return null;
  }
}
