"use client"

import {
  CircleCheckIcon,
  InfoIcon,
  Loader2Icon,
  OctagonXIcon,
  TriangleAlertIcon,
} from "lucide-react"
import { Toaster as Sonner, type ToasterProps } from "sonner"
import { useHydrated } from "@/hooks/use-hydrated"
import { usePreferencesStore } from "@/stores/preferences-store"

const Toaster = ({ ...props }: ToasterProps) => {
  // Follow the reader's chosen theme (ThemeSync applies the same value to <html>). Until
  // hydration the persisted value isn't known on the server, so start from "system".
  const hydrated = useHydrated()
  const preferred = usePreferencesStore((s) => s.settings.theme)
  const theme = hydrated ? preferred : "system"

  return (
    <Sonner
      theme={theme}
      className="toaster group"
      icons={{
        success: <CircleCheckIcon className="size-4" />,
        info: <InfoIcon className="size-4" />,
        warning: <TriangleAlertIcon className="size-4" />,
        error: <OctagonXIcon className="size-4" />,
        loading: <Loader2Icon className="size-4 animate-spin" />,
      }}
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "var(--border)",
          "--border-radius": "var(--radius)",
        } as React.CSSProperties
      }
      {...props}
    />
  )
}

export { Toaster }
