'use client';

import { usePathname } from 'next/navigation';
import type { ReactNode } from 'react';
import { Sidebar } from '@/components/Sidebar';
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

  if (bare) {
    return (
      <div className="auth-shell">
        <ErrorBoundary>{children}</ErrorBoundary>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="content-column">
        <main className="main">
          <ErrorBoundary>{children}</ErrorBoundary>
        </main>
        <SiteFooter />
      </div>
    </div>
  );
}
