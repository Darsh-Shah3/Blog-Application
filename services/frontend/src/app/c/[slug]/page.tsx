'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { api, Community, friendlyError, Page, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { canDeleteCommunity } from '@/lib/rbac';
import { PostCard } from '@/components/PostCard';
import { CommunityChat } from '@/components/CommunityChat';
import { CreatePostModal } from '@/components/CreatePostModal';
import { TopBar } from '@/components/TopBar';
import Link from 'next/link';
import { CommunityDetailSkeleton } from '@/components/loaders';

export default function CommunityPage() {
  const params = useParams<{ slug: string }>();
  const router = useRouter();
  const { token, user, loading: authLoading } = useAuth();
  const { success, error } = useToast();
  const [community, setCommunity] = useState<Community | null>(null);
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [composeOpen, setComposeOpen] = useState(false);
  const [q, setQ] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const c = await api<Community>(`/api/v1/communities/by-slug/${params.slug}`, {}, token);
      setCommunity(c);
      const page = await api<Page<Post>>(`/api/v1/posts?communityId=${c.id}&sort=hot&size=30`, {}, token);
      setPosts(page.content || []);
    } catch (e) {
      error(friendlyError(e, 'Could not load community.'));
      setCommunity(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!params.slug || authLoading) return;
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.slug, token, authLoading]);

  const join = async () => {
    if (!community || !token) {
      error('Please sign in to join.');
      return;
    }
    try {
      await api(`/api/v1/communities/${community.id}/join`, { method: 'POST' }, token);
      success('Joined community.');
      load();
    } catch (e) {
      error(friendlyError(e, 'Could not join.'));
    }
  };

  const leave = async () => {
    if (!community || !token) return;
    try {
      await api(`/api/v1/communities/${community.id}/leave`, { method: 'POST' }, token);
      success('Left community.');
      load();
    } catch (e) {
      error(friendlyError(e, 'Could not leave.'));
    }
  };

  const removeCommunity = async () => {
    if (!community || !token) return;
    if (!window.confirm(`Delete community “${community.name}”? This cannot be undone.`)) return;
    try {
      await api(`/api/v1/communities/${community.id}`, { method: 'DELETE' }, token);
      success('Community deleted.');
      router.push('/communities');
    } catch (e) {
      error(friendlyError(e, 'Could not delete community.'));
    }
  };

  if (authLoading || (loading && !community)) {
    return <CommunityDetailSkeleton />;
  }
  if (!community) {
    return (
      <div className="panel page-shell" style={{ marginTop: '1.5rem' }}>
        <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Community not found</h1>
        <p className="muted">This space may not exist, or the link is incomplete.</p>
        <Link href="/communities" className="btn secondary">Browse communities</Link>
      </div>
    );
  }

  const nextQ = `?next=${encodeURIComponent(`/c/${community.slug}`)}`;
  const creator = community.creatorUsername || 'member';
  const filteredPosts = q.trim()
    ? posts.filter((p) => p.title.toLowerCase().includes(q.trim().toLowerCase()))
    : posts;

  return (
    <div className="page-shell page-shell-wide community-detail-page">
      <TopBar
        crumb={community.name}
        searchPlaceholder="Filter posts in this space…"
        defaultSearch={q}
        onSearch={setQ}
        actions={
          user && community.joined ? (
            <button className="btn" type="button" onClick={() => setComposeOpen(true)}>
              Post here
            </button>
          ) : user ? (
            <button className="btn" type="button" onClick={join}>Join</button>
          ) : (
            <Link className="btn" href={`/login${nextQ}`}>Log in</Link>
          )
        }
      />

      <Link href="/communities" className="post-detail-back">← Communities</Link>

      <header className="page-header page-header-community">
        <div className="page-header-row">
          <div>
            <p className="page-kicker">Community</p>
            <h1>{community.name}</h1>
            <p>{community.description || 'A place for focused discussion.'}</p>
            <div className="page-meta-row">
              <span className="muted">{community.memberCount} members</span>
              {community.joined && <span className="muted">· You&apos;re a member</span>}
              <span className="muted" style={{ fontSize: '0.9rem' }}>
                · Created by <Link href={`/u/${creator}`}>{creator}</Link>
              </span>
            </div>
          </div>
          <div className="page-header-actions">
            {user && !community.joined && (
              <button className="btn" type="button" onClick={join}>Join</button>
            )}
            {user && community.joined && (
              <button className="btn secondary" type="button" onClick={leave}>Leave</button>
            )}
            {canDeleteCommunity(user, community.creatorId) && (
              <button className="btn ghost danger-text" type="button" onClick={removeCommunity}>
                Delete community
              </button>
            )}
          </div>
        </div>
      </header>

      {!user && (
        <div className="feed-guest-banner soft-banner">
          <div>
            <strong>Browsing as a guest</strong>
            <p className="muted" style={{ margin: '0.25rem 0 0' }}>
              Read posts and chat history. Join after login to message members and publish.
            </p>
          </div>
          <div className="toolbar" style={{ margin: 0 }}>
            <Link className="btn" href={`/login${nextQ}`}>Log in</Link>
            <Link className="btn secondary" href={`/register${nextQ}`}>Sign up</Link>
          </div>
        </div>
      )}

      <div className="community-detail-grid">
        <div className="stack">
          <div className="community-posts-head">
            <h2>Posts</h2>
            <span className="muted" style={{ fontSize: '0.9rem' }}>
              {filteredPosts.length} shown
              {q.trim() ? ` · filtered` : ''}
            </span>
          </div>
          <div className="feed-list">
            {filteredPosts.map((p) => <PostCard key={p.id} post={p} />)}
          </div>
          {filteredPosts.length === 0 && (
            <div className="soft-empty muted">
              {q.trim()
                ? 'No posts match that filter.'
                : 'No posts in this community yet. Be the first to share something.'}
            </div>
          )}
        </div>
        <CommunityChat
          communityId={community.id}
          joined={community.joined}
          communityName={community.name}
        />
      </div>

      <CreatePostModal
        open={composeOpen}
        onClose={() => setComposeOpen(false)}
        defaultCommunityId={community.id}
        onCreated={(id) => {
          setComposeOpen(false);
          router.push(`/post/${id}`);
        }}
      />
    </div>
  );
}
