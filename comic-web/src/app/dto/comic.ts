/**
 * Represents a comic series with its metadata and current strip
 */
export class Comic {
  id: number;
  name: string;
  author: string;
  oldest: string;
  newest: string;
  enabled?: boolean;
  description: string;
  strip: string;
  avatar: string;
  active?: boolean; // Whether comic is actively publishing (false = inactive/discontinued)
}
