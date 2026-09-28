import 'server-only';
import { cookies } from 'next/headers';
import type { User } from '@/types/auth';
import { GetMeDocument } from '@/generated/graphql';
import { JWT_COOKIE, GRAPHQL_ENDPOINT } from './constants';
import { getRequestId } from '@/lib/request-id';
import { timedGraphqlFetch } from '@/lib/server-log';

export async function getSession(): Promise<User | null> {
  const cookieStore = await cookies();
  const jwt = cookieStore.get(JWT_COOKIE)?.value;

  if (!jwt) return null;

  try {
    const res = await timedGraphqlFetch(GRAPHQL_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${jwt}`,
      },
      body: JSON.stringify({
        query: GetMeDocument.toString(),
      }),
      // Don't cache — session should be fresh on each server render
      cache: 'no-store',
    }, await getRequestId());

    if (!res.ok) return null;

    const json = await res.json();
    return json.data?.me ?? null;
  } catch {
    return null;
  }
}
