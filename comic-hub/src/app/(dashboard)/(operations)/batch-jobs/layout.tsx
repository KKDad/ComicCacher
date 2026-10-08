import { JetBrains_Mono } from 'next/font/google';

// Only the batch-job logs and parameters use a monospace face, so it loads here
// rather than on every page.
const jetBrainsMono = JetBrains_Mono({
  variable: '--font-mono',
  subsets: ['latin'],
  display: 'swap',
});

export default function BatchJobsLayout({ children }: { children: React.ReactNode }) {
  return <div className={jetBrainsMono.variable}>{children}</div>;
}
