'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useState, type ReactNode } from 'react';
import { useAuth } from '@/lib/auth';
import { isAdmin, roleLabel } from '@/lib/rbac';
import {
  CloseIcon,
  HomeIcon,
  LogInIcon,
  LogOutIcon,
  MenuIcon,
  PenIcon,
  ShieldIcon,
  UserIcon,
  UserPlusIcon,
  UsersIcon,
} from '@/components/icons';

type NavItem = {
  href: string;
  label: string;
  icon: ReactNode;
  match?: (path: string) => boolean;
};

export function Sidebar() {
  const { user, logout, loading } = useAuth();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const primaryRole = user?.roles?.find((r) => r === 'ROLE_ADMIN' || r === 'ROLE_MODERATOR')
    || user?.roles?.[0];

  const mainNav: NavItem[] = [
    {
      href: '/',
      label: 'Home',
      icon: <HomeIcon size={18} />,
      match: (p) => p === '/',
    },
    {
      href: '/communities',
      label: 'Communities',
      icon: <UsersIcon size={18} />,
      match: (p) => p.startsWith('/communities') || p.startsWith('/c/'),
    },
  ];

  if (user) {
    mainNav.push({
      href: '/submit',
      label: 'New post',
      icon: <PenIcon size={18} />,
      match: (p) => p.startsWith('/submit'),
    });
  }

  if (user && isAdmin(user)) {
    mainNav.push({
      href: '/admin',
      label: 'Admin',
      icon: <ShieldIcon size={18} />,
      match: (p) => p.startsWith('/admin'),
    });
  }

  const isActive = (item: NavItem) => (item.match ? item.match(pathname) : pathname === item.href);

  return (
    <>
      <header className="mobile-bar">
        <button
          type="button"
          className="icon-btn"
          aria-label={open ? 'Close menu' : 'Open menu'}
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
        >
          {open ? <CloseIcon size={20} /> : <MenuIcon size={20} />}
        </button>
        <Link href="/" className="brand brand-sm">
          Thread<span>ly</span>
        </Link>
        <div className="mobile-bar-spacer" />
      </header>

      {open && (
        <button
          type="button"
          className="sidebar-backdrop"
          aria-label="Close navigation"
          onClick={() => setOpen(false)}
        />
      )}

      <aside className={`sidebar ${open ? 'is-open' : ''}`} aria-label="Primary">
        <div className="sidebar-top">
          <Link href="/" className="brand">
            <span className="brand-mark" aria-hidden="true">T</span>
            <span className="brand-text">
              Thread<span>ly</span>
            </span>
          </Link>
          <p className="sidebar-tagline">Focused communities</p>
        </div>

        <nav className="sidebar-nav">
          <p className="nav-section-label">Browse</p>
          <ul className="nav-list">
            {mainNav.map((item) => (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={`nav-link ${isActive(item) ? 'active' : ''}`}
                >
                  <span className="nav-icon">{item.icon}</span>
                  <span>{item.label}</span>
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div className="sidebar-footer">
          {!loading && !user && (
            <>
              <p className="nav-section-label">Account</p>
              <ul className="nav-list">
                <li>
                  <Link href="/login" className={`nav-link ${pathname.startsWith('/login') ? 'active' : ''}`}>
                    <span className="nav-icon"><LogInIcon size={18} /></span>
                    <span>Log in</span>
                  </Link>
                </li>
                <li>
                  <Link href="/register" className="nav-link nav-link-cta">
                    <span className="nav-icon"><UserPlusIcon size={18} /></span>
                    <span>Sign up</span>
                  </Link>
                </li>
              </ul>
            </>
          )}

          {!loading && user && (
            <div className="user-card">
              <Link href={`/u/${user.username}`} className="user-card-main">
                <span className="user-avatar" aria-hidden="true">
                  <UserIcon size={16} />
                </span>
                <span className="user-meta">
                  <span className="user-name">u/{user.username}</span>
                  <span className="user-sub">
                    {primaryRole ? `${roleLabel(primaryRole)} · ` : ''}
                    {user.karma} karma
                  </span>
                </span>
              </Link>
              <button type="button" className="nav-link logout-btn" onClick={logout}>
                <span className="nav-icon"><LogOutIcon size={18} /></span>
                <span>Log out</span>
              </button>
            </div>
          )}
        </div>
      </aside>
    </>
  );
}
