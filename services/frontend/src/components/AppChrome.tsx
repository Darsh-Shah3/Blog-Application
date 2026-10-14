'use client';

import { useEffect, useState, type ReactNode } from 'react';
import { usePathname } from 'next/navigation';
import { Sidebar } from '@/components/Sidebar';
import { AppTopBar } from '@/components/AppTopBar';
import { SiteFooter } from '@/components/SiteFooter';
import { ErrorBoundary } from '@/components/ErrorBoundary';

/** Routes that render as bare full-page experiences (no sidebar / footer). */
const BARE_PREFIXES = [
  '/login',
  '/register',
  '/forgot-password',
  '/reset-password',
  '/oauth',
];

function isBareRoute(pathname: string) {
  return BARE_PREFIXES.some((p) => pathname === p || pathname.startsWith(`${p}/`));
}

export function AppChrome({ children }: { children: ReactNode }) {
  const pathname = usePathname() || '/';
  const bare = isBareRoute(pathname);
  const [navOpen, setNavOpen] = useState(false);

  useEffect(() => {
    setNavOpen(false);
  }, [pathname]);

  if (bare) {
    return (
      <div className="auth-shell">
        <ErrorBoundary>{children}</ErrorBoundary>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <AppTopBar navOpen={navOpen} onToggleNav={() => setNavOpen((v) => !v)} />
      <Sidebar open={navOpen} onOpenChange={setNavOpen} />
      <div className="content-column">
        <main className="main">
          <ErrorBoundary>{children}</ErrorBoundary>
        </main>
        <SiteFooter />
      </div>
    </div>
  );
}
