import {
  LayoutDashboard,
  BookOpen,
  Newspaper,
  BarChart3,
  RefreshCw,
  Settings,
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
  { href: '/', label: 'Dashboard', shortLabel: 'Home', icon: LayoutDashboard },
  { href: '/read', label: 'Daily Reader', shortLabel: 'Daily', icon: Newspaper },
  { href: '/comics', label: 'Comics List', shortLabel: 'Comics', icon: BookOpen },
  { href: '/preferences', label: 'Preferences', icon: Settings },
];

export const operationsNavItems: NavItem[] = [
  { href: '/metrics', label: 'Metrics', icon: BarChart3 },
  { href: '/retrieval-status', label: 'Retrieval Status', icon: RefreshCw },
  { href: '/batch-jobs', label: 'Batch Jobs', icon: Cog },
];

/** Active for the exact route and, except for the root, anything beneath it. */
export function isNavActive(pathname: string, href: string): boolean {
  if (href === '/') return pathname === '/';
  return pathname === href || pathname.startsWith(`${href}/`);
}
