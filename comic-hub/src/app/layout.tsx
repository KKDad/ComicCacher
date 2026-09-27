import type { Metadata } from "next";
import { Figtree, Bricolage_Grotesque, DynaPuff } from "next/font/google";
import "./globals.css";
import { Providers } from "@/lib/providers";
import { THEME_BOOTSTRAP_SCRIPT } from "@/lib/theme";
import { InlineScript } from "@/components/theme/inline-script";

// Body text: rounder than a neutral grotesque, so it sits well beside DynaPuff
const figtree = Figtree({
  variable: "--font-primary",
  subsets: ["latin"],
  display: "swap",
});

// Section headings, comic names and strip dates
const bricolage = Bricolage_Grotesque({
  variable: "--font-heading",
  subsets: ["latin"],
  weight: ["600", "700"],
  display: "swap",
});

// The wordmark and page titles only
const dynaPuff = DynaPuff({
  variable: "--font-display",
  subsets: ["latin"],
  weight: ["700"],
  display: "swap",
});

// JetBrains Mono loads only on the batch-jobs pages that use it (see its layout).

export const metadata: Metadata = {
  title: { template: "%s · Comics Hub", default: "Comics Hub" },
  description: "Your personal comic strip collection",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    // suppressHydrationWarning: the inline script sets the theme class on <html> before React hydrates
    <html lang="en" suppressHydrationWarning>
      <head>
        <InlineScript html={THEME_BOOTSTRAP_SCRIPT} />
      </head>
      <body
        className={`${figtree.variable} ${bricolage.variable} ${dynaPuff.variable} antialiased`}
      >
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
