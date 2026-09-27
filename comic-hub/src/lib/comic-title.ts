import 'server-only';
import { getAuthenticatedClient } from '@/lib/auth/graphql-server';
import { GetComicDocument, type GetComicQuery } from '@/generated/graphql';

/**
 * The comic's name for the page title. Titles are best effort: if the lookup fails
 * (an expired token, a bad id) the page falls back to the site name.
 */
export async function comicTitle(id: string): Promise<string | undefined> {
  const comicId = Number.parseInt(id, 10);
  if (Number.isNaN(comicId)) return undefined;
  try {
    const client = await getAuthenticatedClient();
    const data = await client.request<GetComicQuery>(GetComicDocument.toString(), { id: comicId });
    return data.comic?.name ?? undefined;
  } catch {
    return undefined;
  }
}
