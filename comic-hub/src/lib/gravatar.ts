/**
 * Generates a Gravatar URL for the given email using SHA-256.
 * Returns `d=404` so that Radix AvatarImage falls through to AvatarFallback
 * when no Gravatar is set.
 */
export async function getGravatarUrl(
  email: string,
  size = 80,
): Promise<string> {
  const normalized = email.trim().toLowerCase();
  const msgBuffer = new TextEncoder().encode(normalized);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  const hashHex = Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
  return `https://www.gravatar.com/avatar/${hashHex}?s=${size}&d=404`;
}
