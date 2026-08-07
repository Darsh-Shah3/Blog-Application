'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { isAdmin, roleLabel } from '@/lib/rbac';
import { useTheme } from '@/lib/theme';
import { MoonIcon, SunIcon } from '@/components/icons';

export function SiteHeader() {
  const { user, token, logout, loading } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [unread, setUnread] = useState(0);
  const primaryRole = user?.roles?.find((r) => r === 'ROLE_ADMIN' || r === 'ROLE_MODERATOR')
    || user?.roles?.[0];

  useEffect(() => {
    if (!token) {
      setUnread(0);
      return;
    }
    api<{ count: number }>('/api/v1/notifications/unread-count', {}, token)
      .then((r) => setUnread(r.count || 0))
      .catch(() => setUnread(0));
    const t = setInterval(() => {
      api<{ count: number }>('/api/v1/notifications/unread-count', {}, token)
        .then((r) => setUnread(r.count || 0))
        .catch(() => undefined);
    }, 30000);
    return () => clearInterval(t);
  }, [token]);

  return (
    <header className="site-header">
      <div className="header-inner">
        <Link href="/" className="brand">
          Thread<span>ly</span>
        </Link>
        <div className="nav-actions">
          <Link href="/feed" className="btn ghost">Feed</Link>
          <Link href="/communities" className="btn ghost">Communities</Link>
          {user && <Link href="/submit" className="btn secondary">New post</Link>}
          {user && (
            <Link href="/notifications" className="btn ghost">
              Alerts{unread > 0 ? ` (${unread})` : ''}
            </Link>
          )}
          {user && isAdmin(user) && (
            <Link href="/admin" className="btn secondary">Admin</Link>
          )}
          <button
            type="button"
            className="theme-toggle"
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            title={theme === 'dark' ? 'Light mode' : 'Dark mode'}
          >
            {theme === 'dark' ? <SunIcon size={18} /> : <MoonIcon size={18} />}
            <span className="theme-toggle-label">
              {theme === 'dark' ? 'Light' : 'Dark'}
            </span>
          </button>
          {!loading && !user && (
            <>
              <Link href="/login" className="btn ghost">Log in</Link>
              <Link href="/register" className="btn">Sign up</Link>
            </>
          )}
          {!loading && user && (
            <>
              <Link href={`/u/${user.username}`} className="btn ghost">
                u/{user.username}
                {primaryRole ? ` · ${roleLabel(primaryRole)}` : ''} · {user.karma} karma
              </Link>
              <button className="btn secondary" onClick={logout} type="button">Log out</button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
