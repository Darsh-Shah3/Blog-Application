'use client';

import Link from 'next/link';
import { useAuth } from '@/lib/auth';
import { getGrafanaUrl } from '@/lib/api';

export function SiteFooter() {
  const { user } = useAuth();
  const grafanaUrl = getGrafanaUrl();

  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <div>
          <div className="brand brand-with-image">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/brand-mark.svg" alt="" className="brand-img brand-img-sm" width={28} height={28} />
            Thread<span>ly</span>
          </div>
          <p className="muted" style={{ margin: '0.5rem 0 0', maxWidth: '22rem' }}>
            Communities, posts, votes, follows, and notifications — one stack for sharing and discovery.
          </p>
        </div>
        <div className="footer-cols">
          <div>
            <h3>Explore</h3>
            <Link href="/">Home</Link>
            <Link href="/feed">Feed</Link>
            <Link href="/communities">Communities</Link>
            <Link href="/about">About</Link>
          </div>
          <div>
            <h3>Account</h3>
            {user ? (
              <>
                <Link href={`/u/${user.username}`}>Your profile</Link>
                <Link href="/notifications">Notifications</Link>
                <Link href="/communities">Communities</Link>
              </>
            ) : (
              <>
                <Link href="/login">Log in</Link>
                <Link href="/register">Sign up</Link>
              </>
            )}
          </div>
          <div>
            <h3>Ops</h3>
            <Link href="/admin">Admin</Link>
            <a href={grafanaUrl} target="_blank" rel="noreferrer">Grafana</a>
          </div>
        </div>
      </div>
      <p className="footer-copy muted">
        © {new Date().getFullYear()} Threadly · built by{' '}
        <a href="https://github.com/Darsh-Shah3" target="_blank" rel="noreferrer">Darsh Shah</a>
      </p>
    </footer>
  );
}
