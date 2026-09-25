'use client';

import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { Search, X } from 'lucide-react';
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
import { useUser } from '@/contexts/user-context';
import { useLogout } from '@/hooks/use-auth';
import { getGravatarUrl } from '@/lib/gravatar';

interface SearchFieldProps {
  value: string;
  onChange: (value: string) => void;
  onClear: () => void;
  autoFocus?: boolean;
}

function SearchField({ value, onChange, onClear, autoFocus }: SearchFieldProps) {
  return (
    <div className="relative w-full" role="search">
      <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-muted" aria-hidden="true" />
      <Input
        type="search"
        placeholder="Search comics..."
        aria-label="Search comics"
        className="pl-9 pr-9 bg-canvas [&::-webkit-search-cancel-button]:hidden"
        value={value}
        autoFocus={autoFocus}
        onChange={(e) => onChange(e.target.value)}
      />
      {value && (
        <button
          type="button"
          onClick={onClear}
          aria-label="Clear search"
          className="absolute right-1 top-1/2 -translate-y-1/2 p-2 rounded-md text-ink-muted hover:text-ink"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
}

export function Header() {
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

  // Refining a search replaces the history entry; arriving from elsewhere pushes one.
  const navigateSearch = useCallback((value: string) => {
    const trimmed = value.trim();
    const onComics = pathname === '/comics';
    const navigate = onComics ? router.replace : router.push;
    if (trimmed) {
      navigate(`/comics?q=${encodeURIComponent(trimmed)}`);
    } else if (onComics) {
      navigate('/comics');
    }
  }, [router, pathname]);

  // Debounced live search — updates the URL 300ms after the user stops typing
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
    <header className="fixed top-0 left-0 right-0 z-sticky bg-surface border-b border-border">
      <div className="flex items-center justify-between gap-4 h-[var(--header-height)] px-4 lg:px-6">
        <div className="flex items-center gap-4 flex-1 min-w-0">
          <Link
            href="/"
            className="font-display text-xl font-bold text-primary rounded-md"
          >
            Comics Hub
          </Link>

          <div className="hidden md:flex items-center flex-1 max-w-md">
            <SearchField
              value={searchValue}
              onChange={setSearchValue}
              onClear={() => setSearchValue('')}
            />
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            aria-label={mobileSearchOpen ? 'Close search' : 'Search comics'}
            aria-expanded={mobileSearchOpen}
            onClick={() => setMobileSearchOpen((v) => !v)}
          >
            {mobileSearchOpen ? <X className="h-5 w-5" /> : <Search className="h-5 w-5" />}
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="rounded-full"
                aria-label={`Account menu for ${user?.displayName ?? 'user'}`}
              >
                <Avatar className="h-8 w-8">
                  {gravatarUrl && <AvatarImage src={gravatarUrl} alt="" />}
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
                  <p className="text-xs text-ink-subtle">{user?.email ?? '—'}</p>
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

      {mobileSearchOpen && (
        <div className="md:hidden px-4 py-2 border-t border-border">
          <SearchField
            value={searchValue}
            onChange={setSearchValue}
            autoFocus
            onClear={() => {
              setSearchValue('');
              setMobileSearchOpen(false);
            }}
          />
        </div>
      )}
    </header>
  );
}
