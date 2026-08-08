'use client';

import Link from 'next/link';
import { useAuth } from '@/lib/auth';

export function AboutCtas({ variant }: { variant: 'hero' | 'footer' }) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="toolbar" style={{ marginTop: variant === 'hero' ? '1.1rem' : undefined }}>
        <span className="btn secondary" style={{ opacity: 0.6, pointerEvents: 'none' }}>…</span>
      </div>
    );
  }

  if (user) {
    if (variant === 'hero') {
      return (
        <div className="toolbar" style={{ marginTop: '1.1rem' }}>
          <Link className="btn" href="/feed">Open the feed</Link>
          <Link className="btn secondary" href="/submit">Create post</Link>
        </div>
      );
    }
    return (
      <div className="toolbar">
        <Link className="btn" href="/submit">Create post</Link>
        <Link className="btn secondary" href="/communities">Communities</Link>
        <Link className="btn ghost" href="/feed">Posts</Link>
      </div>
    );
  }

  if (variant === 'hero') {
    return (
      <div className="toolbar" style={{ marginTop: '1.1rem' }}>
        <Link className="btn" href="/register">Get started</Link>
        <Link className="btn secondary" href="/feed">Open the feed</Link>
      </div>
    );
  }

  return (
    <div className="toolbar">
      <Link className="btn" href="/register">Create account</Link>
      <Link className="btn secondary" href="/communities">Communities</Link>
      <Link className="btn ghost" href="/">Home</Link>
    </div>
  );
}
