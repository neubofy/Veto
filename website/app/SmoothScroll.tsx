'use client';
import { ReactLenis, useLenis } from 'lenis/react';
import { useEffect } from 'react';

function HashHandler({ children }: { children: React.ReactNode }) {
  const lenis = useLenis();

  useEffect(() => {
    if (!lenis) return;
    const handleHashClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      const anchor = target.closest('a');
      if (!anchor) return;
      const href = anchor.getAttribute('href');
      if (href?.startsWith('#') && href.length > 1) {
        e.preventDefault();
        lenis.scrollTo(href);
        window.history.pushState(null, '', href);
      } else if (href?.includes('#')) {
        const hash = href.substring(href.indexOf('#'));
        if (window.location.pathname === href.substring(0, href.indexOf('#'))) {
          e.preventDefault();
          lenis.scrollTo(hash);
          window.history.pushState(null, '', href);
        }
      }
    };
    
    // Initial load hash scroll
    if (window.location.hash) {
      setTimeout(() => {
        lenis.scrollTo(window.location.hash);
      }, 500);
    }

    document.documentElement.addEventListener('click', handleHashClick);
    return () => document.documentElement.removeEventListener('click', handleHashClick);
  }, [lenis]);

  return <>{children}</>;
}

export default function SmoothScroll({ children }: { children: React.ReactNode }) {
  return (
    <ReactLenis root options={{ lerp: 0.1, duration: 1.5, smoothWheel: true }}>
      <HashHandler>{children}</HashHandler>
    </ReactLenis>
  );
}