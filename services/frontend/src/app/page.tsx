'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { api, Community, Page, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Skeleton } from '@/components/loaders';

export default function LandingPage() {
  const { user, token, loading } = useAuth();
  const [communities, setCommunities] = useState<Community[]>([]);
  const [posts, setPosts] = useState<Post[]>([]);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      api<Page<Community>>('/api/v1/communities/discover?size=6', {}, token)
        .then((p) => p.content || [])
        .catch(() => [] as Community[]),
      api<Page<Post>>('/api/v1/posts?sort=hot&size=5', {}, token)
        .then((p) => p.content || [])
        .catch(() => [] as Post[]),
    ]).then(([c, p]) => {
      if (cancelled) return;
      setCommunities(c);
      setPosts(p);
      setReady(true);
    });
    return () => {
      cancelled = true;
    };
  }, [token]);

  return (
    <div className="home">
      <section className="home-hero" aria-labelledby="home-brand">
        <div className="home-hero-plane" aria-hidden="true">
          <div className="home-hero-mesh" />
          <svg className="home-hero-threads" viewBox="0 0 800 520" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              className="home-thread home-thread-a"
              d="M40 420 C 160 320, 220 480, 340 360 S 520 220, 760 280"
              stroke="currentColor"
              strokeWidth="1.5"
            />
            <path
              className="home-thread home-thread-b"
              d="M20 180 C 180 120, 260 300, 400 200 S 620 80, 780 160"
              stroke="currentColor"
              strokeWidth="1.5"
            />
            <path
              className="home-thread home-thread-c"
              d="M80 80 C 200 160, 280 40, 420 120 S 600 260, 740 140"
              stroke="currentColor"
              strokeWidth="1.25"
            />
            <g className="home-post-silhouettes" opacity="0.55">
              <rect x="470" y="120" width="220" height="88" rx="10" />
              <rect x="490" y="140" width="120" height="10" rx="3" />
              <rect x="490" y="160" width="170" height="8" rx="3" />
              <rect x="490" y="176" width="140" height="8" rx="3" />
              <rect x="430" y="230" width="260" height="72" rx="10" />
              <rect x="450" y="248" width="100" height="10" rx="3" />
              <rect x="450" y="268" width="190" height="8" rx="3" />
              <rect x="510" y="320" width="200" height="64" rx="10" />
              <rect x="528" y="338" width="90" height="10" rx="3" />
              <rect x="528" y="358" width="150" height="8" rx="3" />
            </g>
          </svg>
        </div>

        <div className="home-hero-copy">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/brand-mark.svg"
            alt=""
            className="home-hero-mark"
            width={88}
            height={88}
          />
          <h1 id="home-brand" className="home-brand">
            Threadly
          </h1>
          <p className="home-lead">
            Spaces for posts, replies, and live community chat — all in one place.
          </p>
          <div className="home-cta">
            {loading && (
              <>
                <Skeleton width="9rem" height={44} radius={10} />
                <Skeleton width="7.5rem" height={44} radius={10} />
              </>
            )}
            {!loading && !user && (
              <>
                <Link className="btn" href="/register">
                  Create account
                </Link>
                <Link className="btn secondary" href="/login">
                  Log in
                </Link>
              </>
            )}
            {!loading && user && (
              <>
                <Link className="btn" href="/feed">
                  Open feed
                </Link>
                <Link className="btn secondary" href="/communities">
                  Browse communities
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      <section className="home-stream" aria-label="What’s active">
        <div className="home-stream-col">
          <header className="home-stream-head">
            <h2>Spaces</h2>
            <Link href="/communities">All communities</Link>
          </header>
          {!ready && (
            <div className="home-stream-skel">
              <Skeleton height={22} />
              <Skeleton height={22} />
              <Skeleton height={22} />
            </div>
          )}
          {ready && communities.length === 0 && (
            <p className="muted">No communities yet — create the first one.</p>
          )}
          {ready && communities.length > 0 && (
            <ul className="home-stream-list">
              {communities.map((c) => (
                <li key={c.id}>
                  <Link href={`/c/${c.slug}`} className="home-stream-row">
                    <span className="home-stream-title">{c.name}</span>
                    <span className="home-stream-meta">{c.memberCount} members</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="home-stream-col">
          <header className="home-stream-head">
            <h2>Talking now</h2>
            <Link href="/feed">All posts</Link>
          </header>
          {!ready && (
            <div className="home-stream-skel">
              <Skeleton height={28} />
              <Skeleton height={28} />
              <Skeleton height={28} />
            </div>
          )}
          {ready && posts.length === 0 && (
            <p className="muted">No posts yet — the feed is quiet.</p>
          )}
          {ready && posts.length > 0 && (
            <ol className="home-stream-list home-stream-posts">
              {posts.map((p, i) => (
                <li key={p.id}>
                  <Link href={`/post/${p.id}`} className="home-stream-row home-stream-post">
                    <span className="home-stream-rank" aria-hidden="true">
                      {i + 1}
                    </span>
                    <span className="home-stream-post-body">
                      <span className="home-stream-title">{p.title}</span>
                      <span className="home-stream-meta">
                        {p.communityName || 'Community'}
                        {p.authorUsername ? ` · ${p.authorUsername}` : ''}
                        {typeof p.score === 'number' ? ` · ${p.score}` : ''}
                      </span>
                    </span>
                  </Link>
                </li>
              ))}
            </ol>
          )}
        </div>
      </section>
    </div>
  );
}
