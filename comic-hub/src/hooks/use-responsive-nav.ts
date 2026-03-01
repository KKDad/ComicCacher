import { useEffect, useState } from 'react';

export type NavLayout = 'desktop' | 'tablet' | 'mobile';

export function useResponsiveNav() {
  const [layout, setLayout] = useState<NavLayout>('desktop');

  useEffect(() => {
    const updateLayout = () => {
      const width = window.innerWidth;
      if (width >= 1024) {
        setLayout('desktop');
      } else if (width >= 768) {
        setLayout('tablet');
      } else {
        setLayout('mobile');
      }
    };

    updateLayout();
    window.addEventListener('resize', updateLayout);

    return () => window.removeEventListener('resize', updateLayout);
  }, []);

  return { layout };
}
