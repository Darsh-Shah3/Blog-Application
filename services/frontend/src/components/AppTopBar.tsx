'use client';

import Link from 'next/link';
import { FormEvent, useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useTheme } from '@/lib/theme';
import {
  CloseIcon,
  MenuIcon,
  MessageIcon,
  MoonIcon,
  PenIcon,
  SearchIcon,
  SunIcon,
} from '@/components/icons';
import { UserAvatar } from '@/components/UserAvatar';

export function AppTopBar({
  navOpen,
  onToggleNav,
}: {
  navOpen: boolean;
  onToggleNav: () => void;
}) {
  const { user, token, loading } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const pathname = usePathname();
  const router = useRouter();
  const [q, setQ] = useState('');
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (!token) {
      setUnread(0);
      return;
    }
    const load = () => {
      api<{ count: number }>('/api/v1/notifications/unread-count', {}, token)
        .then((r) => setUnread(r.count || 0))
        .catch(() => setUnread(0));
    };
    load();
    const t = setInterval(load, 15000);
    return () => clearInterval(t);
  }, [token, pathname]);

  const submitSearch = (e: FormEvent) => {
    e.preventDefault();
    const query = q.trim();
    router.push(query ? `/feed?q=${encodeURIComponent(query)}` : '/feed');
  };

  return (
    <header className="app-topbar" role="banner">
      <div className="app-topbar-inner">
        <div className="app-topbar-left">
          <button
            type="button"
            className="icon-btn app-topbar-menu"
            aria-label={navOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={navOpen}
            onClick={onToggleNav}
          >
            {navOpen ? <CloseIcon size={20} /> : <MenuIcon size={20} />}
          </button>
          <Link href="/" className="app-topbar-brand" title="Threadly">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/brand-mark.svg" alt="" className="brand-img brand-img-sm" width={28} height={28} />
            <span className="brand-text">
              Thread<span>ly</span>
            </span>
          </Link>
        </div>

        <form className="app-topbar-search" onSubmit={submitSearch} role="search">
          <SearchIcon size={16} />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search posts…"
            aria-label="Search posts"
          />
        </form>

        <div className="app-topbar-right">
          {user && (
            <Link href="/feed" className="btn app-topbar-compose" title="Create post">
              <PenIcon size={15} />
              <span>Post</span>
            </Link>
          )}
          {user && (
            <Link
              href="/notifications"
              className="icon-btn app-topbar-icon"
              aria-label="Alerts"
              title="Alerts"
            >
              <MessageIcon size={18} />
              {unread > 0 && (
                <span className="app-topbar-badge">{unread > 99 ? '99+' : unread}</span>
              )}
            </Link>
          )}
          <button
            type="button"
            className="icon-btn app-topbar-icon"
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            title={theme === 'dark' ? 'Light mode' : 'Dark mode'}
          >
            {theme === 'dark' ? <SunIcon size={18} /> : <MoonIcon size={18} />}
          </button>
          {!loading && user && (
            <Link
              href={`/u/${user.username}`}
              className="app-topbar-avatar"
              title={user.username}
            >
              <UserAvatar
                username={user.username}
                avatarMediaId={user.avatarMediaId}
                isAdmin={Boolean(user.roles?.some((r) => r === 'ROLE_ADMIN' || r === 'ADMIN'))
                  || user.username.toLowerCase() === 'admin'}
                size={30}
              />
            </Link>
          )}
          {!loading && !user && (
            <Link href="/login" className="btn secondary app-topbar-login">
              Log in
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
