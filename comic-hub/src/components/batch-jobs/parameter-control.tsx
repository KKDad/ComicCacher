'use client';

import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { BatchJobParameter } from '@/generated/graphql';
import { BatchJobParameterType } from '@/generated/graphql';

interface ParameterControlProps {
  param: BatchJobParameter;
  value: string;
  onChange: (value: string) => void;
}

export function ParameterControl({ param, value, onChange }: ParameterControlProps) {
  const id = `param-${param.name}`;

  switch (param.type) {
    case BatchJobParameterType.Enum:
      return (
        <div className="space-y-2">
          <Label htmlFor={id}>{param.label}</Label>
          <Select value={value} onValueChange={onChange}>
            <SelectTrigger id={id}>
              <SelectValue />
            </SelectTrigger>
            <SelectContent className="z-popover">
              {param.options?.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      );

    case BatchJobParameterType.Boolean:
      return (
        <div className="flex items-center justify-between">
          <Label htmlFor={id}>{param.label}</Label>
          <Switch
            id={id}
            checked={value === 'true'}
            onCheckedChange={(checked) => onChange(String(checked))}
          />
        </div>
      );

    case BatchJobParameterType.Integer:
      return (
        <div className="space-y-2">
          <Label htmlFor={id}>{param.label}</Label>
          <Input
            id={id}
            type="number"
            value={value}
            onChange={(e) => onChange(e.target.value)}
          />
        </div>
      );

    case BatchJobParameterType.String:
      return (
        <div className="space-y-2">
          <Label htmlFor={id}>{param.label}</Label>
          <Input
            id={id}
            type="text"
            value={value}
            onChange={(e) => onChange(e.target.value)}
          />
        </div>
      );
  }
}
