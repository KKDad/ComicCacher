'use client';

import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { WINDOWS } from './health';

export const ALL_SOURCES = '__all__';

export interface Filters {
  source: string | null;
  query: string;
  attentionOnly: boolean;
  days: number;
}

interface RetrievalFiltersProps {
  filters: Filters;
  sources: string[];
  onChange: (filters: Filters) => void;
}

export function RetrievalFilters({ filters, sources, onChange }: RetrievalFiltersProps) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
      <div className="flex items-center gap-2">
        <Switch
          id="retrieval-attention-only"
          checked={filters.attentionOnly}
          onCheckedChange={(attentionOnly) => onChange({ ...filters, attentionOnly })}
        />
        <Label htmlFor="retrieval-attention-only" className="text-sm text-ink-subtle">
          Only comics needing attention
        </Label>
      </div>
      <Select
        value={filters.source ?? ALL_SOURCES}
        onValueChange={(value) => onChange({ ...filters, source: value === ALL_SOURCES ? null : value })}
      >
        <SelectTrigger aria-label="Source" className="sm:w-44">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ALL_SOURCES}>Every source</SelectItem>
          {sources.map((s) => (
            <SelectItem key={s} value={s}>
              {s}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select value={String(filters.days)} onValueChange={(value) => onChange({ ...filters, days: Number(value) })}>
        <SelectTrigger aria-label="Days" className="sm:w-32">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {WINDOWS.map((d) => (
            <SelectItem key={d} value={String(d)}>
              {d} days
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Input
        type="search"
        placeholder="Search comics"
        aria-label="Search comics"
        value={filters.query}
        onChange={(e) => onChange({ ...filters, query: e.target.value })}
        className="sm:max-w-64"
      />
    </div>
  );
}
