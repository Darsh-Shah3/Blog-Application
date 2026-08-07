'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { api, friendlyError, Page, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { PostCard } from '@/components/PostCard';

const SORTS = ['hot', 'new', 'top'] as const;

export default function FeedPage() {
  const { token, loading: authLoading } = useAuth();
  const { error } = useToast();
  const [sort, setSort] = useState<(typeof SORTS)[number]>('hot');
  const [q, setQ] = useState('');
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);

  const load = (search = q, s = sort) => {
    if (!token) {
      setPosts([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    const qs = new URLSearchParams({ sort: s, page: '0', size: '20' });
    if (search.trim()) qs.set('q', search.trim());
    api<Page<Post>>(`/api/v1/posts?${qs}`, {}, token)
      .then((page) => setPosts(page.content || []))
      .catch((e) => error(friendlyError(e, 'Could not load feed.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (authLoading) return;
    load();
  }, [sort, token, authLoading]);

  return (
    <>
      <section className="hero" style={{ paddingTop: '1.5rem' }}>
        <h1>Your feed</h1>
        <p>Hot, new, and top posts from communities you care about. Search by title anytime.</p>
      </section>

      {!authLoading && !token && (
        <div className="panel" style={{ marginBottom: '1rem' }}>
          <p style={{ marginTop: 0 }}>Sign in to load personalized posts.</p>
          <div className="toolbar">
            <Link className="btn" href="/login">Log in</Link>
            <Link className="btn secondary" href="/register">Sign up</Link>
          </div>
        </div>
      )}

      {token && (
        <>
          <form
            className="toolbar"
            onSubmit={(e) => {
              e.preventDefault();
              load();
            }}
          >
            <input
              className="input"
              style={{ minWidth: '12rem', flex: 1 }}
              placeholder="Search posts by title…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
            <button type="submit" className="btn secondary">Search</button>
            {SORTS.map((s) => (
              <button
                key={s}
                type="button"
                className={`chip ${sort === s ? 'active' : ''}`}
                onClick={() => setSort(s)}
              >
                {s}
              </button>
            ))}
          </form>
        </>
      )}

      {loading && token && <p className="muted">Loading feed…</p>}
      {!loading && token && posts.length === 0 && (
        <div className="panel">
          <p className="muted">No posts yet. Create a community and publish the first thread.</p>
          <div className="toolbar">
            <Link className="btn" href="/communities">Communities</Link>
            <Link className="btn secondary" href="/submit">New post</Link>
          </div>
        </div>
      )}
      <div className="stack">
        {posts.map((p) => (
          <PostCard key={p.id} post={p} />
        ))}
      </div>
    </>
  );
}
