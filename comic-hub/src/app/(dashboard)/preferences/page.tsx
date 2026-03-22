'use client';

import { useEffect, useRef, useCallback } from 'react';
import { useGetUserPreferencesQuery, useUpdateDisplaySettingsMutation } from '@/generated/graphql';
import { useQueryClient } from '@tanstack/react-query';
import { usePreferencesStore } from '@/stores/preferences-store';
import { type DisplaySettings, type Theme, type ReaderNavMode, type ReaderScrollOrder } from '@/lib/preferences-defaults';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Sun, Moon, Monitor } from 'lucide-react';
import { toast } from 'sonner';

const COMICS_PER_PAGE_OPTIONS = [12, 24, 48, 96];
const ZOOM_OPTIONS = [50, 75, 100, 125, 150];

export default function PreferencesPage() {
  const queryClient = useQueryClient();
  const { data: prefsData } = useGetUserPreferencesQuery();
  const settings = usePreferencesStore((s) => s.settings);
  const isHydrated = usePreferencesStore((s) => s.isHydrated);
  const hydrate = usePreferencesStore((s) => s.hydrate);
  const setSettings = usePreferencesStore((s) => s.setSettings);

  const debounceRef = useRef<ReturnType<typeof setTimeout>>(null);

  const mutation = useUpdateDisplaySettingsMutation({
    onSuccess: (data) => {
      if (data.updateDisplaySettings.errors.length > 0) {
        toast.error('Failed to save preferences');
      } else {
        toast.success('Preferences saved');
        queryClient.invalidateQueries({ queryKey: ['GetUserPreferences'] });
      }
    },
    onError: () => {
      toast.error('Failed to save preferences');
    },
  });

  // Hydrate store from server data on first load
  useEffect(() => {
    if (prefsData?.preferences?.displaySettings !== undefined) {
      hydrate(prefsData.preferences.displaySettings);
    }
  }, [prefsData?.preferences?.displaySettings, hydrate]);

  const saveSettings = useCallback(
    (next: DisplaySettings) => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
      debounceRef.current = setTimeout(() => {
        mutation.mutate({ settings: next });
      }, 300);
    },
    [mutation],
  );

  const update = useCallback(
    (partial: Partial<DisplaySettings>) => {
      setSettings(partial);
      const next = { ...settings, ...partial };
      saveSettings(next);
    },
    [setSettings, settings, saveSettings],
  );

  if (!isHydrated) {
    return (
      <div className="space-y-6 max-w-2xl">
        <div>
          <h1 className="text-2xl font-bold">Preferences</h1>
          <p className="text-muted-foreground mt-1">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-2xl font-bold">Preferences</h1>
        <p className="text-muted-foreground mt-1">Customize your Comics Hub experience.</p>
      </div>

      {/* Appearance */}
      <Card>
        <CardHeader>
          <CardTitle>Appearance</CardTitle>
          <CardDescription>Choose how Comics Hub looks to you.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex gap-2">
            {([
              { value: 'light', label: 'Light', icon: Sun },
              { value: 'dark', label: 'Dark', icon: Moon },
              { value: 'system', label: 'System', icon: Monitor },
            ] as const).map(({ value, label, icon: Icon }) => (
              <Button
                key={value}
                variant={settings.theme === value ? 'default' : 'outline'}
                size="sm"
                onClick={() => update({ theme: value as Theme })}
              >
                <Icon className="h-4 w-4 mr-1.5" />
                {label}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Dashboard */}
      <Card>
        <CardHeader>
          <CardTitle>Dashboard</CardTitle>
          <CardDescription>Choose which sections appear on your dashboard.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <SettingsSwitch
            label="Continue Reading"
            description="Show your most recently read comic"
            checked={settings.showContinueReading}
            onCheckedChange={(v) => update({ showContinueReading: v })}
          />
          <SettingsSwitch
            label="Favorites"
            description="Show your favorite comics"
            checked={settings.showFavorites}
            onCheckedChange={(v) => update({ showFavorites: v })}
          />
          <SettingsSwitch
            label="Recently Added"
            description="Show the latest comics with new strips"
            checked={settings.showRecentlyAdded}
            onCheckedChange={(v) => update({ showRecentlyAdded: v })}
          />
        </CardContent>
      </Card>

      {/* Reading */}
      <Card>
        <CardHeader>
          <CardTitle>Reading</CardTitle>
          <CardDescription>Configure your default reading behavior.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium">Reading list</p>
              <p className="text-xs text-muted-foreground">Which comics appear in the reader sidebar</p>
            </div>
            <div className="flex gap-2">
              <Button
                variant={settings.readerNavMode === 'favorites' ? 'default' : 'outline'}
                size="sm"
                onClick={() => update({ readerNavMode: 'favorites' as ReaderNavMode })}
              >
                Favorites
              </Button>
              <Button
                variant={settings.readerNavMode === 'all' ? 'default' : 'outline'}
                size="sm"
                onClick={() => update({ readerNavMode: 'all' as ReaderNavMode })}
              >
                All
              </Button>
            </div>
          </div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium">Scroll order</p>
              <p className="text-xs text-muted-foreground">Order strips load when scrolling in the reader</p>
            </div>
            <div className="flex gap-2">
              <Button
                variant={settings.readerScrollOrder === 'catchup' ? 'default' : 'outline'}
                size="sm"
                onClick={() => update({ readerScrollOrder: 'catchup' as ReaderScrollOrder })}
              >
                Catch up
              </Button>
              <Button
                variant={settings.readerScrollOrder === 'newest-first' ? 'default' : 'outline'}
                size="sm"
                onClick={() => update({ readerScrollOrder: 'newest-first' as ReaderScrollOrder })}
              >
                Newest first
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Display */}
      <Card>
        <CardHeader>
          <CardTitle>Display</CardTitle>
          <CardDescription>Adjust layout and zoom defaults.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium">Comics per page</p>
              <p className="text-xs text-muted-foreground">Number of comics shown per page</p>
            </div>
            <Select
              value={String(settings.comicsPerPage)}
              onValueChange={(v) => update({ comicsPerPage: Number(v) })}
            >
              <SelectTrigger className="w-24">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {COMICS_PER_PAGE_OPTIONS.map((n) => (
                  <SelectItem key={n} value={String(n)}>
                    {n}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium">Default zoom</p>
              <p className="text-xs text-muted-foreground">Default zoom level for comic strips</p>
            </div>
            <Select
              value={String(settings.defaultZoom)}
              onValueChange={(v) => update({ defaultZoom: Number(v) })}
            >
              <SelectTrigger className="w-24">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {ZOOM_OPTIONS.map((n) => (
                  <SelectItem key={n} value={String(n)}>
                    {n}%
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function SettingsSwitch({
  label,
  description,
  checked,
  onCheckedChange,
}: {
  label: string;
  description: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between">
      <div>
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">{description}</p>
      </div>
      <Switch checked={checked} onCheckedChange={onCheckedChange} />
    </div>
  );
}
