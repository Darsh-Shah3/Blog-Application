'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { api, Community, friendlyError, Page, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { PostCard } from '@/components/PostCard';
import { CommunityChat } from '@/components/CommunityChat';
import { CreatePostModal } from '@/components/CreatePostModal';
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

  if (authLoading || (loading && !community)) {
    return <CommunityDetailSkeleton />;
  }
  if (!community) {
    return (
      <div className="panel" style={{ marginTop: '1.5rem' }}>
        <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Community not found</h1>
        <p className="muted">This space may not exist, or the link is incomplete.</p>
        <Link href="/communities" className="btn secondary">Browse communities</Link>
      </div>
    );
  }

  const nextQ = `?next=${encodeURIComponent(`/c/${community.slug}`)}`;
  const creator = community.creatorUsername || 'member';

  return (
    <>
      <section className="hero community-hero-detail">
        <p className="muted" style={{ margin: '0 0 0.35rem', fontSize: '0.9rem' }}>Community</p>
        <h1>{community.name}</h1>
        <p>{community.description || 'A place for focused discussion.'}</p>
        <p className="muted" style={{ marginTop: '0.35rem', fontSize: '0.95rem' }}>
          {community.memberCount} members
          {community.joined ? ' · you’re a member' : ''}
          {' · created by '}
          <Link href={`/u/${creator}`}>{creator}</Link>
        </p>
        <div className="toolbar">
          <span className="chip active">{community.memberCount} members</span>
          {user && !community.joined && (
            <button className="btn" type="button" onClick={join}>Join</button>
          )}
          {user && community.joined && (
            <button className="btn secondary" type="button" onClick={leave}>Leave</button>
          )}
          {!user && (
            <Link className="btn" href={`/login${nextQ}`}>Sign in to join</Link>
          )}
          {user && community.joined && (
            <button className="btn secondary" type="button" onClick={() => setComposeOpen(true)}>
              Post here
            </button>
          )}
          {user && !community.joined && (
            <button className="btn ghost" type="button" onClick={join}>Join to post</button>
          )}
          {!user && (
            <Link className="btn secondary" href={`/login${nextQ}`}>Sign in to post</Link>
          )}
        </div>
      </section>

      {!user && (
        <div className="panel feed-guest-banner">
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
          <h2 style={{ fontFamily: 'var(--font-display)', margin: 0, fontSize: '1.25rem' }}>Posts</h2>
          {posts.map((p) => <PostCard key={p.id} post={p} />)}
          {posts.length === 0 && (
            <div className="panel muted">No posts in this community yet. Be the first to share something.</div>
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
    </>
  );
}
