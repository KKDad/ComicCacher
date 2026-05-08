'use client';

import { useEffect } from 'react';

export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error('Global error boundary caught:', error);
  }, [error]);

  return (
    <html lang="en">
      <body
        style={{
          fontFamily: 'system-ui, sans-serif',
          background: '#0a0a0a',
          color: '#fafafa',
          margin: 0,
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '1.5rem',
        }}
      >
        <div style={{ maxWidth: 420, textAlign: 'center' }}>
          <h1 style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>Something broke badly</h1>
          <p style={{ marginBottom: '1.5rem', opacity: 0.8 }}>
            ComicCacher hit an error it couldn&apos;t recover from. Reload to try again.
          </p>
          <button
            type="button"
            onClick={reset}
            style={{
              padding: '0.5rem 1rem',
              background: '#fafafa',
              color: '#0a0a0a',
              border: 'none',
              borderRadius: '0.375rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Reload
          </button>
        </div>
      </body>
    </html>
  );
}
