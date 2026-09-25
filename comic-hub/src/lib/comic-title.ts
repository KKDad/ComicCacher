import { getAuthenticatedClient } from '@/lib/auth/graphql-server';
import { GetComicDocument, type GetComicQuery } from '@/generated/graphql';

const FALLBACK = 'Comic';

/**
 * The comic's name for a page title, fetched with the caller's session.
 * Titles are best-effort: any failure (bad id, expired token, backend down)
 * falls back to a generic title rather than failing the page.
 */
export async function comicTitle(id: string): Promise<string> {
  const comicId = Number(id);
  if (!Number.isInteger(comicId)) return FALLBACK;

  try {
    const client = await getAuthenticatedClient();
    const data = await client.request<GetComicQuery>(GetComicDocument.toString(), { id: comicId });
    return data.comic?.name ?? FALLBACK;
  } catch {
    return FALLBACK;
  }
}
