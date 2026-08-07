'use client';

import Link from 'next/link';
import { useAuth } from '@/lib/auth';

const FEATURES = [
  {
    title: 'Communities',
    body: 'Create or join topic spaces, discover spaces by search, and follow conversations that match your interests.',
    href: '/communities',
    cta: 'Browse communities',
  },
  {
    title: 'Posts & media',
    body: 'Share text, links, images, or files. Sort the home feed by hot, new, or top and filter by author or community.',
    href: '/feed',
    cta: 'Open the feed',
  },
  {
    title: 'Discuss & vote',
    body: 'Nested comment threads and votes. Moderators and admins can keep conversations healthy.',
    href: '/feed',
    cta: 'See discussions',
  },
  {
    title: 'Follow people',
    body: 'Follow authors you care about. When they publish, you get an in-app notification and optional email.',
    href: '/feed',
    cta: 'Start exploring',
  },
  {
    title: 'Notifications',
    body: 'Platform alerts and follow updates land in one inbox. Filter unread or search by title.',
    href: '/notifications',
    cta: 'Open inbox',
  },
  {
    title: 'Admin & reports',
    body: 'Admins manage roles, review the full audit trail, and run platform activity reports.',
    href: '/admin',
    cta: 'Admin area',
  },
];

export default function LandingPage() {
  const { user, loading } = useAuth();

  return (
    <div className="landing">
      <section className="landing-hero">
        <p className="landing-kicker">Threadly</p>
        <h1>Communities that feel alive</h1>
        <p className="landing-lead">
          A Reddit-style place to create communities, publish posts, vote, and follow people you trust —
          with JWT-secured APIs, audit history, and real-time style notifications when creators post.
        </p>
        <div className="toolbar" style={{ marginTop: '1.25rem' }}>
          {!loading && !user && (
            <>
              <Link className="btn" href="/register">Create account</Link>
              <Link className="btn secondary" href="/login">Log in</Link>
            </>
          )}
          {!loading && user && (
            <>
              <Link className="btn" href="/feed">Go to feed</Link>
              <Link className="btn secondary" href="/submit">New post</Link>
            </>
          )}
          <Link className="btn ghost" href="/communities">Explore communities</Link>
        </div>
      </section>

      <section className="landing-grid">
        {FEATURES.map((f) => (
          <article key={f.title} className="landing-card">
            <h2>{f.title}</h2>
            <p>{f.body}</p>
            <Link href={f.href} className="btn secondary">{f.cta}</Link>
          </article>
        ))}
      </section>

      <section className="panel landing-cta">
        <h2 style={{ marginTop: 0 }}>Ready when you are</h2>
        <p className="muted">
          Jump into the feed, open a community, or follow a creator whose posts you do not want to miss.
        </p>
        <div className="toolbar">
          <Link className="btn" href="/feed">Home feed</Link>
          <Link className="btn secondary" href="/communities">Communities</Link>
          <Link className="btn ghost" href="/notifications">Notifications</Link>
        </div>
      </section>
    </div>
  );
}
