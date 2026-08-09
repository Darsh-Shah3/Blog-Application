'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import type { ReactNode } from 'react';

const LINKS = [
  { href: '/admin', label: 'Control', match: (p: string) => p === '/admin' },
  { href: '/admin/analytics', label: 'Reports', match: (p: string) => p.startsWith('/admin/analytics') },
  { href: '/admin/infrastructure', label: 'Infra', match: (p: string) => p.startsWith('/admin/infrastructure') },
] as const;

/** Compact admin sub-nav — no heavy tab button rows. */
export function AdminNav({ trailing }: { trailing?: ReactNode }) {
  const pathname = usePathname() || '/admin';
  return (
    <nav className="admin-nav-bar" aria-label="Admin sections">
      <div className="admin-nav-links">
        {LINKS.map((l) => (
          <Link
            key={l.href}
            href={l.href}
            className={`admin-nav-link${l.match(pathname) ? ' is-active' : ''}`}
          >
            {l.label}
          </Link>
        ))}
      </div>
      {trailing && <div className="admin-nav-trailing">{trailing}</div>}
    </nav>
  );
}

export function AdminPageShell({
  kicker,
  title,
  blurb,
  trailing,
  children,
}: {
  kicker: string;
  title: string;
  blurb?: string;
  trailing?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="page-shell page-shell-wide admin-shell stack">
      <header className="admin-page-head">
        <div>
          <p className="admin-kicker">{kicker}</p>
          <h1 className="admin-title">{title}</h1>
          {blurb && <p className="admin-blurb">{blurb}</p>}
        </div>
        {trailing}
      </header>
      <AdminNav />
      {children}
    </div>
  );
}
