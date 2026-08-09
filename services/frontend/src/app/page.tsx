'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { api, Community, Page, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Skeleton } from '@/components/loaders';
import { PostCard } from '@/components/PostCard';
import { UsersIcon, MessageCircleIcon } from '@/components/icons';

function excerpt(text?: string | null, max = 120) {
  const t = (text || '').replace(/\s+/g, ' ').trim();
  if (!t) return '';
  return t.length > max ? `${t.slice(0, max)}…` : t;
}

export default function LandingPage() {
  const { user, token, loading } = useAuth();
  const [communities, setCommunities] = useState<Community[]>([]);
  const [hotPosts, setHotPosts] = useState<Post[]>([]);
  const [newPosts, setNewPosts] = useState<Post[]>([]);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      api<Page<Community>>('/api/v1/communities/discover?size=8', {}, token)
        .then((p) => p.content || [])
        .catch(() => [] as Community[]),
      api<Page<Post>>('/api/v1/posts?sort=hot&size=8', {}, token)
        .then((p) => p.content || [])
        .catch(() => [] as Post[]),
      api<Page<Post>>('/api/v1/posts?sort=new&size=6', {}, token)
        .then((p) => p.content || [])
        .catch(() => [] as Post[]),
    ]).then(([c, hot, neu]) => {
      if (cancelled) return;
      setCommunities(c);
      setHotPosts(hot);
      setNewPosts(neu);
      setReady(true);
    });
    return () => {
      cancelled = true;
    };
  }, [token]);

  const featuredHot = hotPosts[0];
  const talkingNow = hotPosts.slice(0, 6);
  const feedMain = hotPosts.slice(0, 5);
  const totalMembers = communities.reduce((n, c) => n + (c.memberCount || 0), 0);

  return (
    <div className="home">
      <section className="home-hero" aria-labelledby="home-brand">
        <div className="home-hero-plane" aria-hidden="true">
          <div className="home-hero-mesh" />
          <span className="home-orb home-orb-a" />
          <span className="home-orb home-orb-b" />
          <span className="home-orb home-orb-c" />
        </div>

        <div className="home-hero-layout">
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
              Thread<span className="home-brand-accent">ly</span>
            </h1>
            <p className="home-lead">
              Spaces for posts, threaded replies, and live community chat — built for discovery,
              not noise.
            </p>
            <div className="home-cta">
              {loading && (
                <>
                  <Skeleton width="9rem" height={44} radius={12} />
                  <Skeleton width="8rem" height={44} radius={12} />
                </>
              )}
              {!loading && !user && (
                <>
                  <Link className="btn" href="/register">
                    Create account
                  </Link>
                  <Link className="btn secondary" href="/feed">
                    Browse posts
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
            {ready && (
              <p className="home-hero-pulse muted">
                <strong>{communities.length || '—'}</strong> spaces
                <span aria-hidden="true"> · </span>
                <strong>{totalMembers || '—'}</strong> members
                <span aria-hidden="true"> · </span>
                <strong>{hotPosts.length || '—'}</strong> hot threads
              </p>
            )}
          </div>

          <div className="home-stage" aria-hidden="true">
            <div className="home-stage-card home-stage-card-a">
              <span className="home-stage-kicker">Live chat</span>
              <span className="home-stage-title">Community rooms, real-time</span>
              <span className="home-stage-meta">WebSocket threads · members only</span>
            </div>
            <div className="home-stage-card home-stage-card-b">
              <span className="home-stage-kicker">Hot posts</span>
              <span className="home-stage-title">
                {featuredHot?.title || 'Stories rising across spaces'}
              </span>
              <span className="home-stage-meta">
                {featuredHot
                  ? `${featuredHot.communityName || 'Community'}${featuredHot.authorUsername ? ` · ${featuredHot.authorUsername}` : ''}`
                  : 'Votes, replies, and follows'}
              </span>
            </div>
            <div className="home-stage-card home-stage-card-c">
              <span className="home-stage-kicker">Spaces</span>
              <span className="home-stage-title">
                {communities[0]?.name || 'Join circles that match you'}
              </span>
              <span className="home-stage-meta">
                {communities[0]
                  ? `${communities[0].memberCount} members · growing`
                  : 'Discover · join · post'}
              </span>
            </div>
          </div>
        </div>
      </section>

      {user && (
        <section className="home-welcome" aria-label="Your shortcuts">
          <div className="home-shell home-welcome-inner">
            <div>
              <p className="home-welcome-kicker">Signed in as {user.username}</p>
              <h2>Pick up where you left off</h2>
              <p className="muted">Jump into the feed, open a space, or check alerts.</p>
            </div>
            <div className="home-welcome-actions">
              <Link className="btn" href="/feed">Feed</Link>
              <Link className="btn secondary" href="/communities">Communities</Link>
              <Link className="btn ghost" href="/notifications">Alerts</Link>
              <Link className="btn ghost" href={`/u/${user.username}`}>Profile</Link>
            </div>
          </div>
        </section>
      )}

      <section className="home-body" aria-label="Explore Threadly">
        <div className="home-shell home-body-grid">
          <div className="home-col-main stack">
            <section className="home-panel" aria-labelledby="home-featured-heading">
              <header className="home-panel-head">
                <div>
                  <p className="home-panel-kicker">Featured</p>
                  <h2 id="home-featured-heading">Rising right now</h2>
                </div>
                <Link href="/feed?sort=hot">Full feed</Link>
              </header>
              {!ready && (
                <div className="home-stream-skel">
                  <Skeleton height={120} radius={14} />
                  <Skeleton height={120} radius={14} />
                </div>
              )}
              {ready && feedMain.length === 0 && (
                <p className="muted">No posts yet — the feed is quiet.</p>
              )}
              {ready && feedMain.length > 0 && (
                <div className="home-feed-list">
                  {feedMain.map((p) => (
                    <PostCard key={p.id} post={p} />
                  ))}
                </div>
              )}
              {ready && feedMain.length > 0 && (
                <div className="home-panel-foot">
                  <Link className="btn secondary" href="/feed">See more posts</Link>
                </div>
              )}
            </section>

            <section className="home-panel" aria-labelledby="home-new-heading">
              <header className="home-panel-head">
                <div>
                  <p className="home-panel-kicker">Fresh</p>
                  <h2 id="home-new-heading">Just posted</h2>
                </div>
                <Link href="/feed?sort=new">Newest first</Link>
              </header>
              {!ready && (
                <div className="home-stream-skel">
                  <Skeleton height={48} />
                  <Skeleton height={48} />
                  <Skeleton height={48} />
                </div>
              )}
              {ready && newPosts.length === 0 && (
                <p className="muted">Nothing new yet.</p>
              )}
              {ready && newPosts.length > 0 && (
                <ul className="home-fresh-list">
                  {newPosts.map((p) => (
                    <li key={p.id}>
                      <Link href={`/post/${p.id}`} className="home-fresh-row">
                        <span className="home-fresh-title">{p.title}</span>
                        <span className="home-fresh-meta muted">
                          {p.communityName || 'Community'}
                          {p.authorUsername ? ` · ${p.authorUsername}` : ''}
                          {typeof p.commentCount === 'number' ? ` · ${p.commentCount} comments` : ''}
                        </span>
                        {excerpt(p.content) && (
                          <span className="home-fresh-excerpt muted">{excerpt(p.content)}</span>
                        )}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>

          <aside className="home-col-rail stack">
            <section className="home-panel home-panel-compact" aria-labelledby="home-spaces-heading">
              <header className="home-panel-head">
                <div>
                  <p className="home-panel-kicker">Spaces</p>
                  <h2 id="home-spaces-heading">Communities</h2>
                </div>
                <Link href="/communities">All</Link>
              </header>
              {!ready && (
                <div className="home-stream-skel">
                  <Skeleton height={56} />
                  <Skeleton height={56} />
                  <Skeleton height={56} />
                </div>
              )}
              {ready && communities.length === 0 && (
                <p className="muted">No communities yet — create the first one.</p>
              )}
              {ready && communities.length > 0 && (
                <ul className="home-space-list">
                  {communities.map((c) => (
                    <li key={c.id}>
                      <Link href={`/c/${c.slug}`} className="home-space-card">
                        <span className="home-space-icon" aria-hidden>
                          <UsersIcon size={16} />
                        </span>
                        <span className="home-space-body">
                          <span className="home-space-name">{c.name}</span>
                          <span className="home-space-desc muted">
                            {excerpt(c.description, 72) || `${c.memberCount} members`}
                          </span>
                          <span className="home-space-meta muted">{c.memberCount} members</span>
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
              <div className="home-panel-foot">
                <Link className="btn secondary" href="/communities">
                  {user ? 'Create or join' : 'Browse spaces'}
                </Link>
              </div>
            </section>

            <section className="home-panel home-panel-compact" aria-labelledby="home-talking-heading">
              <header className="home-panel-head">
                <div>
                  <p className="home-panel-kicker">Hot</p>
                  <h2 id="home-talking-heading">Talking now</h2>
                </div>
                <Link href="/feed">Posts</Link>
              </header>
              {!ready && (
                <div className="home-stream-skel">
                  <Skeleton height={36} />
                  <Skeleton height={36} />
                  <Skeleton height={36} />
                </div>
              )}
              {ready && talkingNow.length === 0 && (
                <p className="muted">No trending posts yet.</p>
              )}
              {ready && talkingNow.length > 0 && (
                <ol className="home-talk-list">
                  {talkingNow.map((p, i) => (
                    <li key={p.id}>
                      <Link href={`/post/${p.id}`} className="home-talk-row">
                        <span className="home-stream-rank" aria-hidden="true">{i + 1}</span>
                        <span className="home-talk-body">
                          <span className="home-talk-title">{p.title}</span>
                          <span className="home-talk-meta muted">
                            {p.communityName || 'Community'}
                            {typeof p.score === 'number' ? ` · ▲ ${p.score}` : ''}
                            {typeof p.commentCount === 'number' ? ` · ${p.commentCount}` : ''}
                          </span>
                        </span>
                      </Link>
                    </li>
                  ))}
                </ol>
              )}
            </section>

            <section className="home-panel home-panel-compact home-howto" aria-labelledby="home-howto-heading">
              <header className="home-panel-head">
                <div>
                  <p className="home-panel-kicker">Guide</p>
                  <h2 id="home-howto-heading">How Threadly works</h2>
                </div>
              </header>
              <ul className="home-howto-list">
                <li>
                  <strong>Join a space</strong>
                  <span className="muted">Find a community around a topic you care about.</span>
                </li>
                <li>
                  <strong>Post &amp; vote</strong>
                  <span className="muted">Share writing, links, and files — score rises with the crowd.</span>
                </li>
                <li>
                  <strong>Thread replies</strong>
                  <span className="muted">Keep discussions nested and easy to follow.</span>
                </li>
                <li>
                  <strong>Live room chat</strong>
                  <span className="muted">Jump into community chat when a space is hoppin.</span>
                </li>
              </ul>
              <div className="home-panel-foot home-howto-links">
                <Link href="/about">About the product</Link>
                {!user && <Link href="/register">Join free</Link>}
              </div>
            </section>
          </aside>
        </div>
      </section>

      <section className="home-paths" aria-label="Quick paths">
        <div className="home-shell home-paths-grid">
          <Link href="/feed" className="home-path-card">
            <MessageCircleIcon size={22} />
            <strong>Posts feed</strong>
            <span className="muted">Hot, trending, new, and top — search by title anytime.</span>
          </Link>
          <Link href="/communities" className="home-path-card">
            <UsersIcon size={22} />
            <strong>Communities</strong>
            <span className="muted">Discover rooms, join members, open live chat.</span>
          </Link>
          <Link href="/about" className="home-path-card">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/brand-mark.svg" alt="" width={22} height={22} />
            <strong>Why Threadly</strong>
            <span className="muted">Microservices, JWT auth, and observability under the hood.</span>
          </Link>
        </div>
      </section>
    </div>
  );
}
