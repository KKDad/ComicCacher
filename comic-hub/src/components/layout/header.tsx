'use client';

import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { Search, Bell, Menu, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useSidebarStore } from '@/stores/sidebar-store';
import { useUser } from '@/contexts/user-context';
import { useLogout } from '@/hooks/use-auth';
import { getGravatarUrl } from '@/lib/gravatar';

interface HeaderProps {
  showMenuButton?: boolean;
}

export function Header({ showMenuButton = false }: HeaderProps) {
  const { toggle } = useSidebarStore();
  const user = useUser();
  const { logout } = useLogout();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [searchValue, setSearchValue] = useState(
    pathname === '/comics' ? (searchParams.get('q') ?? '') : '',
  );
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);
  const [gravatarUrl, setGravatarUrl] = useState<string | null>(null);

  // Sync search input when URL changes (e.g. clearing via back button)
  useEffect(() => {
    if (pathname === '/comics') {
      setSearchValue(searchParams.get('q') ?? '');
    } else {
      setSearchValue('');
    }
  }, [pathname, searchParams]);

  const navigateSearch = useCallback((value: string) => {
    const trimmed = value.trim();
    if (trimmed) {
      router.push(`/comics?q=${encodeURIComponent(trimmed)}`);
    } else if (pathname === '/comics') {
      router.push('/comics');
    }
  }, [router, pathname]);

  // Debounced live search — pushes URL 300ms after the user stops typing
  useEffect(() => {
    const currentQuery = (pathname === '/comics' ? searchParams.get('q') : null) ?? '';
    if (searchValue.trim() === currentQuery) return;
    const timer = setTimeout(() => navigateSearch(searchValue), 300);
    return () => clearTimeout(timer);
  }, [searchValue, navigateSearch, pathname, searchParams]);

  useEffect(() => {
    if (!user?.email) return;
    let cancelled = false;
    getGravatarUrl(user.email).then((url) => {
      if (!cancelled) setGravatarUrl(url);
    });
    return () => { cancelled = true; };
  }, [user?.email]);

  const initials = user?.displayName
    ? user.displayName.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : 'U';

  return (
    <header className="fixed top-0 left-0 right-0 z-sticky h-[var(--header-height)] bg-surface border-b border-border">
      <div className="flex items-center justify-between h-full px-4 lg:px-6">
        {/* Left section */}
        <div className="flex items-center gap-4 flex-1">
          {showMenuButton && (
            <Button
              variant="ghost"
              size="sm"
              onClick={toggle}
              className="lg:hidden"
            >
              <Menu className="h-5 w-5" />
            </Button>
          )}

          <div className="flex items-center gap-3">
            <h1
              className="text-xl font-bold text-primary hidden sm:block"
              style={{ fontFamily: 'var(--font-display)' }}
            >
              Comics Hub
            </h1>
          </div>

          {/* Search bar - hidden on mobile */}
          <div className="hidden md:flex items-center flex-1 max-w-md">
            <div className="relative w-full">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-muted" />
              <Input
                type="search"
                placeholder="Search comics..."
                className="pl-9 bg-canvas"
                value={searchValue}
                onChange={(e) => setSearchValue(e.target.value)}
              />
              {searchValue && (
                <button
                  type="button"
                  onClick={() => setSearchValue('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-muted hover:text-ink"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Right section */}
        <div className="flex items-center gap-2">
          {/* Search button - mobile only */}
          <Button
            variant="ghost"
            size="sm"
            className="md:hidden"
            onClick={() => setMobileSearchOpen((v) => !v)}
          >
            {mobileSearchOpen ? <X className="h-5 w-5" /> : <Search className="h-5 w-5" />}
          </Button>

          {/* Notifications */}
          <Button variant="ghost" size="sm" className="relative">
            <Bell className="h-5 w-5" />
            <span className="absolute top-1.5 right-1.5 h-2 w-2 bg-error rounded-full" />
          </Button>

          {/* User menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="relative h-8 w-8 rounded-full">
                <Avatar className="h-8 w-8">
                  {gravatarUrl && (
                    <AvatarImage src={gravatarUrl} alt={user?.displayName ?? 'User avatar'} />
                  )}
                  <AvatarFallback className="bg-primary text-primary-foreground">
                    {initials}
                  </AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium">{user?.displayName ?? '—'}</p>
                  <p className="text-xs text-ink-muted">{user?.email ?? '—'}</p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild>
                <Link href="/preferences">Preferences</Link>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={logout} className="text-error">
                Sign out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* Mobile search bar */}
      {mobileSearchOpen && (
        <div className="md:hidden px-4 py-2 border-t border-border">
          <div className="relative w-full">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-muted" />
            <Input
              type="search"
              placeholder="Search comics..."
              className="pl-9 bg-canvas"
              autoFocus
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
            />
            {searchValue && (
              <button
                type="button"
                onClick={() => { setSearchValue(''); setMobileSearchOpen(false); }}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-muted hover:text-ink"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
