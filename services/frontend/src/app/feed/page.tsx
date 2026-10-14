'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { api, friendlyError, Page, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { PostCard } from '@/components/PostCard';
import { CreatePostModal } from '@/components/CreatePostModal';
import {
  ContentPlaceholder,
  PageSpinner,
  PostSkeletonList,
  SpinnerLabel,
} from '@/components/loaders';
import { TopBar } from '@/components/TopBar';
import { PenIcon } from '@/components/icons';

const SORTS_AUTH = [
  { id: 'for_you', label: 'For you' },
  { id: 'hot', label: 'Hot' },
  { id: 'trending', label: 'Trending' },
  { id: 'new', label: 'New' },
  { id: 'top', label: 'Top' },
] as const;

const SORTS_GUEST = [
  { id: 'hot', label: 'Hot' },
  { id: 'trending', label: 'Trending' },
  { id: 'new', label: 'New' },
  { id: 'top', label: 'Top' },
] as const;

type SortId = (typeof SORTS_AUTH)[number]['id'];
const PAGE_SIZE = 10;

export default function FeedPage() {
  const { token, user, loading: authLoading } = useAuth();
  const { error } = useToast();
  const [sort, setSort] = useState<SortId>('hot');
  const [q, setQ] = useState('');
  const [posts, setPosts] = useState<Post[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [composeOpen, setComposeOpen] = useState(false);

  useEffect(() => {
    if (authLoading) return;
    if (!token && sort === 'for_you') {
      setSort('hot');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, authLoading]);

  const load = async (search = q, s = sort, pageNum = 0, append = false) => {
    if (append) setLoadingMore(true);
    else setLoading(true);
    try {
      const qs = new URLSearchParams({
        sort: s,
        page: String(pageNum),
        size: String(PAGE_SIZE),
      });
      if (search.trim()) qs.set('q', search.trim());
      const result = await api<Page<Post>>(`/api/v1/posts?${qs}`, {}, token);
      const batch = result.content || [];
      setPosts((prev) => (append ? [...prev, ...batch] : batch));
      setPage(pageNum);
      setHasMore(!result.last && batch.length >= PAGE_SIZE);
    } catch (e) {
      error(friendlyError(e, 'Could not load posts.'));
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  useEffect(() => {
    if (authLoading) return;
    load(q, sort, 0, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sort, token, authLoading]);

  const sorts = token ? SORTS_AUTH : SORTS_GUEST;

  return (
    <div className="page-shell page-shell-wide feed-page">
      <div className="feed-layout">
        <div className="feed-main stack">
          <TopBar
            crumb="Posts"
            searchPlaceholder="Search posts by title…"
            defaultSearch={q}
            onSearch={(value) => {
              setQ(value);
              load(value, sort, 0, false);
            }}
            filters={(
              <>
                {sorts.map((s) => (
                  <button
                    key={s.id}
                    type="button"
                    className={`chip ${sort === s.id ? 'active' : ''}`}
                    onClick={() => setSort(s.id)}
                  >
                    {s.label}
                  </button>
                ))}
              </>
            )}
            actions={
              user ? (
                <button type="button" className="btn feed-compose-btn" onClick={() => setComposeOpen(true)}>
                  <PenIcon size={15} />
                  <span>New post</span>
                </button>
              ) : (
                <Link className="btn secondary" href="/login">Log in</Link>
              )
            }
          />

          {!authLoading && !token && (
            <div className="feed-guest-banner soft-banner">
              <div>
                <strong>Browsing as a guest</strong>
                <p className="muted" style={{ margin: '0.25rem 0 0' }}>
                  Read full posts and communities. Sign in to create, join, vote, or reply.
                </p>
              </div>
              <div className="toolbar" style={{ margin: 0 }}>
                <Link className="btn" href="/login">Log in</Link>
                <Link className="btn secondary" href="/register">Sign up</Link>
              </div>
            </div>
          )}

          {authLoading && <PageSpinner label="Checking session…" />}
          {loading && !authLoading && <PostSkeletonList count={5} />}
          {!loading && !authLoading && posts.length === 0 && (
            <ContentPlaceholder
              title="No posts yet"
              description={
                token
                  ? 'Join communities, follow people, or switch to Hot / Trending for global posts.'
                  : 'Demo posts appear once services seed the database — refresh after backends start, or switch sorts.'
              }
              action={(
                <>
                  <Link className="btn" href="/communities">Discover communities</Link>
                  {token ? (
                    <button type="button" className="btn secondary" onClick={() => setComposeOpen(true)}>New post</button>
                  ) : (
                    <Link className="btn secondary" href="/register">Sign up to post</Link>
                  )}
                </>
              )}
            />
          )}
          {!loading && (
            <div className="feed-list">
              {posts.map((p) => (
                <PostCard key={p.id} post={p} />
              ))}
            </div>
          )}
          {hasMore && (
            <div className="feed-load-more">
              <button
                type="button"
                className="btn secondary"
                disabled={loadingMore}
                onClick={() => load(q, sort, page + 1, true)}
              >
                {loadingMore ? <SpinnerLabel label="Loading more…" /> : 'Load more posts'}
              </button>
            </div>
          )}
          {loadingMore && posts.length > 0 && <PostSkeletonList count={2} />}
        </div>

        <aside className="feed-rail">
          <div className="panel feed-rail-card">
            <h2>How ranking works</h2>
            <ul className="feed-rail-list">
              <li><strong>Hot</strong> — score + recency blend</li>
              <li><strong>Trending</strong> — rising engagement</li>
              <li><strong>New</strong> — latest first</li>
              <li><strong>Top</strong> — all-time votes</li>
              {token && <li><strong>For you</strong> — joined spaces & follows</li>}
            </ul>
          </div>
          <div className="panel feed-rail-card">
            <h2>Spaces</h2>
            <p className="muted" style={{ marginTop: 0 }}>
              Join communities so For you and chat stay useful.
            </p>
            <Link className="btn secondary" href="/communities">Browse communities</Link>
          </div>
        </aside>
      </div>

      <CreatePostModal
        open={composeOpen}
        onClose={() => setComposeOpen(false)}
        onCreated={() => {
          setComposeOpen(false);
          load(q, sort, 0, false);
        }}
      />
    </div>
  );
}
