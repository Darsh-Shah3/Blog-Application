'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useState, type MouseEvent, type ReactNode } from 'react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { isAdmin, roleLabel } from '@/lib/rbac';
import {
  HomeIcon,
  LogInIcon,
  LogOutIcon,
  MessageCircleIcon,
  MessageIcon,
  ShieldIcon,
  UserPlusIcon,
  UsersIcon,
} from '@/components/icons';
import { Skeleton } from '@/components/loaders';
import { UserAvatar } from '@/components/UserAvatar';

const COLLAPSE_KEY = 'threadly-sidebar-collapsed';

type NavItem = {
  href: string;
  label: string;
  icon: ReactNode;
  badge?: number;
  match?: (path: string) => boolean;
};

function ChevronLeftIcon({ size = 18 }: { size?: number }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="m15 18-6-6 6-6" />
    </svg>
  );
}

export function Sidebar({
  open = false,
  onOpenChange,
}: {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
}) {
  const { user, token, logout, loading } = useAuth();
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);
  const [unread, setUnread] = useState(0);
  const [dmUnread, setDmUnread] = useState(0);
  const mobileOpen = open;

  useEffect(() => {
    try {
      setCollapsed(localStorage.getItem(COLLAPSE_KEY) === '1');
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    if (!mobileOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onOpenChange?.(false);
    };
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = prev;
      window.removeEventListener('keydown', onKey);
    };
  }, [mobileOpen, onOpenChange]);

  useEffect(() => {
    if (!token) {
      setUnread(0);
      setDmUnread(0);
      return;
    }
    const load = () => {
      api<{ count: number }>('/api/v1/notifications/unread-count', {}, token)
        .then((r) => setUnread(r.count || 0))
        .catch(() => setUnread(0));
      api<{ count: number }>('/api/v1/notifications/unread-count?type=DIRECT_MESSAGE', {}, token)
        .then((r) => setDmUnread(r.count || 0))
        .catch(() => setDmUnread(0));
    };
    load();
    const t = setInterval(load, 15000);
    return () => clearInterval(t);
  }, [token, pathname]);

  const setCollapsedPersist = (next: boolean) => {
    setCollapsed(next);
    try {
      localStorage.setItem(COLLAPSE_KEY, next ? '1' : '0');
    } catch {
      // ignore
    }
  };

  const expandSidebar = () => setCollapsedPersist(false);
  const collapseSidebar = () => setCollapsedPersist(true);

  /** When collapsed, click the rail (not interactive nav controls) to expand. */
  const onRailClick = (e: MouseEvent<HTMLElement>) => {
    if (!collapsed || mobileOpen) return;
    const target = e.target as HTMLElement;
    if (target.closest('a, button')) return;
    expandSidebar();
  };

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
      href: '/feed',
      label: 'Posts',
      icon: <FlameGlyph />,
      match: (p) => p.startsWith('/feed') || p.startsWith('/post/'),
    },
    {
      href: '/communities',
      label: 'Communities',
      icon: <UsersIcon size={18} />,
      match: (p) => p.startsWith('/communities') || p.startsWith('/c/'),
    },
    {
      href: '/about',
      label: 'About',
      icon: <InfoGlyph />,
      match: (p) => p.startsWith('/about'),
    },
  ];

  if (user) {
    mainNav.push({
      href: '/messages',
      label: 'Messages',
      icon: <MessageCircleIcon size={18} />,
      badge: dmUnread > 0 ? dmUnread : undefined,
      match: (p) => p.startsWith('/messages'),
    });
    mainNav.push({
      href: '/notifications',
      label: 'Alerts',
      icon: <MessageIcon size={18} />,
      badge: unread > 0 ? unread : undefined,
      match: (p) => p.startsWith('/notifications'),
    });
  }

  const adminNav: NavItem[] = user && isAdmin(user)
    ? [
        {
          href: '/admin',
          label: 'Admin',
          icon: <ShieldIcon size={18} />,
          match: (p) =>
            p === '/admin'
            || (p.startsWith('/admin')
              && !p.startsWith('/admin/analytics')
              && !p.startsWith('/admin/infrastructure')),
        },
        {
          href: '/admin/analytics',
          label: 'Reports',
          icon: <ChartGlyph />,
          match: (p) => p.startsWith('/admin/analytics'),
        },
        {
          href: '/admin/infrastructure',
          label: 'Infra',
          icon: <InfraGlyph />,
          match: (p) => p.startsWith('/admin/infrastructure'),
        },
      ]
    : [];

  const isActive = (item: NavItem) => (item.match ? item.match(pathname) : pathname === item.href);
  const railCollapsed = collapsed && !mobileOpen;

  return (
    <>
      {mobileOpen && (
        <button
          type="button"
          className="sidebar-backdrop"
          aria-label="Close navigation"
          onClick={() => onOpenChange?.(false)}
        />
      )}

      <aside
        className={[
          'sidebar',
          mobileOpen ? 'is-open' : '',
          railCollapsed ? 'is-collapsed' : '',
          railCollapsed ? 'sidebar-click-expand' : '',
        ].filter(Boolean).join(' ')}
        aria-label="Primary"
        data-collapsed={railCollapsed ? 'true' : 'false'}
        onClick={onRailClick}
        title={railCollapsed ? 'Click to expand menu' : undefined}
      >
        <div className="sidebar-top">
          {railCollapsed ? (
            <button
              type="button"
              className="sidebar-brand sidebar-brand-btn"
              onClick={expandSidebar}
              title="Expand spaces"
              aria-label="Expand sidebar"
            >
              <PanelExpandIcon size={18} />
            </button>
          ) : (
            <>
              <p className="sidebar-spaces-title">Spaces</p>
              <button
                type="button"
                className="sidebar-collapse-btn"
                onClick={collapseSidebar}
                aria-label="Collapse sidebar"
                title="Collapse"
              >
                <ChevronLeftIcon size={16} />
              </button>
            </>
          )}
        </div>

        <nav className="sidebar-nav">
          <p className="nav-section-label">Browse</p>
          <ul className="nav-list">
            {mainNav.map((item) => (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={`nav-link ${isActive(item) ? 'active' : ''}`}
                  title={item.label}
                  onClick={() => {
                    if (railCollapsed) expandSidebar();
                  }}
                >
                  <span className="nav-icon">{item.icon}</span>
                  <span className="nav-label">{item.label}</span>
                  {item.badge != null && item.badge > 0 && (
                    <span className="nav-badge">{item.badge > 99 ? '99+' : item.badge}</span>
                  )}
                </Link>
              </li>
            ))}
          </ul>
          {adminNav.length > 0 && (
            <>
              <p className="nav-section-label admin-label">Admin</p>
              <ul className="nav-list admin-nav">
                {adminNav.map((item) => (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      className={`nav-link ${isActive(item) ? 'active' : ''}`}
                      title={item.label}
                      onClick={() => {
                        if (railCollapsed) expandSidebar();
                      }}
                    >
                      <span className="nav-icon">{item.icon}</span>
                      <span className="nav-label">{item.label}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </>
          )}
        </nav>

        <div className="sidebar-footer">
          {loading && (
            <div className="user-card" style={{ padding: '0.35rem 0.5rem' }} aria-busy="true">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                <Skeleton width={34} height={34} radius={10} />
                <div style={{ flex: 1 }}>
                  <Skeleton width="70%" height={12} />
                  <Skeleton width="45%" height={10} style={{ marginTop: '0.35rem' }} />
                </div>
              </div>
            </div>
          )}

          {!loading && !user && (
            <>
              <p className="nav-section-label">Account</p>
              <ul className="nav-list">
                <li>
                  <Link
                    href="/login"
                    className={`nav-link ${pathname.startsWith('/login') ? 'active' : ''}`}
                    title="Log in"
                  >
                    <span className="nav-icon"><LogInIcon size={18} /></span>
                    <span className="nav-label">Log in</span>
                  </Link>
                </li>
                <li>
                  <Link href="/register" className="nav-link nav-link-cta" title="Sign up">
                    <span className="nav-icon"><UserPlusIcon size={18} /></span>
                    <span className="nav-label">Sign up</span>
                  </Link>
                </li>
              </ul>
            </>
          )}

          {!loading && user && (
            <div className="user-card">
              <Link
                href={`/u/${user.username}`}
                className="user-card-main"
                title={user.username}
                onClick={() => {
                  if (railCollapsed) expandSidebar();
                }}
              >
                <span className="user-avatar" aria-hidden="true">
                  <UserAvatar
                    username={user.username}
                    avatarMediaId={user.avatarMediaId}
                    isAdmin={Boolean(user.roles?.some((r) => r === 'ROLE_ADMIN' || r === 'ADMIN'))
                      || user.username.toLowerCase() === 'admin'}
                    size={32}
                    className="user-avatar-dp"
                  />
                </span>
                <span className="user-meta">
                  <span className="user-name">{user.username}</span>
                  <span className="user-sub">
                    {primaryRole ? `${roleLabel(primaryRole)} · ` : ''}
                    {user.karma} karma
                  </span>
                </span>
              </Link>
              <button
                type="button"
                className="nav-link logout-btn"
                onClick={() => {
                  if (railCollapsed) {
                    expandSidebar();
                    return;
                  }
                  logout();
                }}
                title="Log out"
              >
                <span className="nav-icon"><LogOutIcon size={18} /></span>
                <span className="nav-label">Log out</span>
              </button>
            </div>
          )}
        </div>
      </aside>
    </>
  );
}

function FlameGlyph() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={18}
      height={18}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1.5-3C8.5 7.5 9 6 10 5c2 3.5 4 4.5 4 8.5a4.5 4.5 0 1 1-9 0c0-1.38.5-2.5 1.5-3.5.5.9 1.2 1.75 2 2.5Z" />
    </svg>
  );
}

function ChartGlyph() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={18}
      height={18}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M3 3v18h18" />
      <path d="M7 14v4" />
      <path d="M12 9v9" />
      <path d="M17 5v13" />
    </svg>
  );
}

function InfraGlyph() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={18}
      height={18}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="5" y="5" width="14" height="14" rx="2" />
      <rect x="9" y="9" width="6" height="6" rx="1" />
      <path d="M9 1v3" />
      <path d="M15 1v3" />
      <path d="M9 20v3" />
      <path d="M15 20v3" />
      <path d="M1 9h3" />
      <path d="M1 15h3" />
      <path d="M20 9h3" />
      <path d="M20 15h3" />
    </svg>
  );
}

function PanelExpandIcon({ size = 18 }: { size?: number }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="3" y="4" width="18" height="16" rx="2" />
      <path d="M9 4v16" />
      <path d="m14 10 3 2-3 2" />
    </svg>
  );
}

function InfoGlyph() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={18}
      height={18}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="9" />
      <path d="M12 16v-4" />
      <path d="M12 8h.01" />
    </svg>
  );
}
