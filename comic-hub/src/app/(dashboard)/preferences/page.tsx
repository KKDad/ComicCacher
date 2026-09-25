'use client';

import { useEffect, useRef, useCallback } from 'react';
import { useGetUserPreferencesQuery, useUpdateDisplaySettingsMutation } from '@/generated/graphql';
import { useQueryClient } from '@tanstack/react-query';
import { usePreferencesStore } from '@/stores/preferences-store';
import { type DisplaySettings, type Theme, type ReaderNavMode, type ReaderScrollOrder } from '@/lib/preferences-defaults';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Sun, Moon, Monitor } from 'lucide-react';
import { toast } from 'sonner';


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
          <CardDescription>Choose how Comics Hub looks on this device and others you sign in to.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex gap-2" role="group" aria-label="Theme">
            {([
              { value: 'light', label: 'Light', icon: Sun },
              { value: 'dark', label: 'Dark', icon: Moon },
              { value: 'system', label: 'System', icon: Monitor },
            ] as const).map(({ value, label, icon: Icon }) => (
              <Button
                key={value}
                variant={settings.theme === value ? 'default' : 'outline'}
                aria-pressed={settings.theme === value}
                size="sm"
                onClick={() => update({ theme: value as Theme })}
              >
                <Icon className="h-4 w-4 mr-1.5" aria-hidden="true" />
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
            label="Latest Updates"
            description="Show the comics with the newest strips"
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
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p id="pref-reading-list" className="text-sm font-medium">Reading list</p>
              <p className="text-xs text-muted-foreground">Which comics appear in the reader&apos;s reading list</p>
            </div>
            <div className="flex gap-2" role="group" aria-labelledby="pref-reading-list">
              <Button
                variant={settings.readerNavMode === 'favorites' ? 'default' : 'outline'}
                aria-pressed={settings.readerNavMode === 'favorites'}
                size="sm"
                onClick={() => update({ readerNavMode: 'favorites' as ReaderNavMode })}
              >
                Favorites
              </Button>
              <Button
                variant={settings.readerNavMode === 'all' ? 'default' : 'outline'}
                aria-pressed={settings.readerNavMode === 'all'}
                size="sm"
                onClick={() => update({ readerNavMode: 'all' as ReaderNavMode })}
              >
                All
              </Button>
            </div>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p id="pref-scroll-order" className="text-sm font-medium">Scroll order</p>
              <p className="text-xs text-muted-foreground">Order strips load when scrolling in the reader</p>
            </div>
            <div className="flex gap-2" role="group" aria-labelledby="pref-scroll-order">
              <Button
                variant={settings.readerScrollOrder === 'catchup' ? 'default' : 'outline'}
                aria-pressed={settings.readerScrollOrder === 'catchup'}
                size="sm"
                onClick={() => update({ readerScrollOrder: 'catchup' as ReaderScrollOrder })}
              >
                Catch up
              </Button>
              <Button
                variant={settings.readerScrollOrder === 'newest-first' ? 'default' : 'outline'}
                aria-pressed={settings.readerScrollOrder === 'newest-first'}
                size="sm"
                onClick={() => update({ readerScrollOrder: 'newest-first' as ReaderScrollOrder })}
              >
                Newest first
              </Button>
            </div>
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
  const id = `pref-${label.toLowerCase().replace(/\s+/g, '-')}`;
  return (
    <div className="flex items-center justify-between gap-4">
      <div>
        <Label htmlFor={id} className="text-sm font-medium">{label}</Label>
        <p id={`${id}-desc`} className="text-xs text-muted-foreground">{description}</p>
      </div>
      <Switch id={id} aria-describedby={`${id}-desc`} checked={checked} onCheckedChange={onCheckedChange} />
    </div>
  );
}
