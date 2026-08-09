'use client';

import Link from 'next/link';
import { useAuth } from '@/lib/auth';
import { isAdmin } from '@/lib/rbac';

export function SiteFooter() {
  const { user } = useAuth();
  const year = new Date().getFullYear();

  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <div className="footer-brand-block">
          <div className="brand brand-with-image">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/brand-mark.svg" alt="" className="brand-img brand-img-sm" width={28} height={28} />
            Thread<span className="home-brand-accent">ly</span>
          </div>
          <p className="muted footer-tagline">
            Threadly is a community-native reading and discussion app — join spaces, publish posts,
            vote thoughtfully, and chat live with people who care about the same topics.
          </p>
        </div>

        <div className="footer-cols">
          <div>
            <h3>Discover</h3>
            <Link href="/">Home</Link>
            <Link href="/feed">Posts feed</Link>
            <Link href="/communities">Communities</Link>
            <Link href="/about">About Threadly</Link>
          </div>
          <div>
            <h3>You</h3>
            {user ? (
              <>
                <Link href={`/u/${user.username}`}>Your profile</Link>
                <Link href="/messages">Messages</Link>
                <Link href="/notifications">Alerts</Link>
                <Link href="/communities">Your communities</Link>
                <Link href="/feed">Write &amp; browse</Link>
              </>
            ) : (
              <>
                <Link href="/login">Log in</Link>
                <Link href="/register">Create account</Link>
                <Link href="/forgot-password">Reset password</Link>
                <Link href="/about">Why Threadly</Link>
              </>
            )}
          </div>
          <div>
            <h3>{user && isAdmin(user) ? 'Admin' : 'Help'}</h3>
            {user && isAdmin(user) ? (
              <>
                <Link href="/admin">Control center</Link>
                <Link href="/admin/analytics">Platform reports</Link>
                <Link href="/admin/infrastructure">Infrastructure</Link>
                <Link href="/about">Architecture</Link>
              </>
            ) : (
              <>
                <Link href="/about">Product story</Link>
                <Link href="/communities">Find a space</Link>
                <Link href="/register">Join free</Link>
              </>
            )}
          </div>
        </div>
      </div>

      <div className="footer-copy muted">
        <span>© {year} Threadly · communities that stay readable</span>
        <span>
          Built by{' '}
          <a href="https://github.com/Darsh-Shah3" target="_blank" rel="noreferrer">
            Darsh Shah
          </a>
        </span>
      </div>
    </footer>
  );
}
