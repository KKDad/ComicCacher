import {
  House,
  LibraryBig,
  Newspaper,
  BarChart3,
  Radio,
  RefreshCw,
  SlidersHorizontal,
  Cog,
  type LucideIcon,
} from 'lucide-react';

export interface NavItem {
  href: string;
  label: string;
  /** Shorter label for the mobile bottom bar. */
  shortLabel?: string;
  icon: LucideIcon;
}

export const baseNavItems: NavItem[] = [
  { href: '/', label: 'Home', shortLabel: 'Home', icon: House },
  { href: '/read', label: 'Today', shortLabel: 'Today', icon: Newspaper },
  { href: '/comics', label: 'Library', shortLabel: 'Library', icon: LibraryBig },
  { href: '/preferences', label: 'Preferences', shortLabel: 'Settings', icon: SlidersHorizontal },
];

export const operationsNavItems: NavItem[] = [
  { href: '/metrics', label: 'Metrics', icon: BarChart3 },
  { href: '/retrieval-status', label: 'Retrieval Status', icon: RefreshCw },
  { href: '/batch-jobs', label: 'Batch Jobs', icon: Cog },
  { href: '/sources', label: 'Sources', icon: Radio },
];

/** Nav icon stroke: heavier for the current page, lighter otherwise. */
export function navIconStroke(active: boolean): number {
  return active ? 2.25 : 1.75;
}

/** Active for the exact route and, except for the root, anything beneath it. */
export function isNavActive(pathname: string, href: string): boolean {
  if (href === '/') return pathname === '/';
  return pathname === href || pathname.startsWith(`${href}/`);
}
