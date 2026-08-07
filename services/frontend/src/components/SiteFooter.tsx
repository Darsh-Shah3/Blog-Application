'use client';

import Link from 'next/link';

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <div>
          <div className="brand">Thread<span>ly</span></div>
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
            <Link href="/submit">New post</Link>
          </div>
          <div>
            <h3>Account</h3>
            <Link href="/login">Log in</Link>
            <Link href="/register">Sign up</Link>
            <Link href="/notifications">Notifications</Link>
            <Link href="/forgot-password">Reset password</Link>
          </div>
          <div>
            <h3>Ops</h3>
            <Link href="/admin">Admin</Link>
            <a href="http://localhost:3001" target="_blank" rel="noreferrer">Grafana</a>
            <a href="http://localhost:8080/actuator/health" target="_blank" rel="noreferrer">API health</a>
          </div>
        </div>
      </div>
      <p className="footer-copy muted">© {new Date().getFullYear()} Threadly · microservices demo</p>
    </footer>
  );
}
