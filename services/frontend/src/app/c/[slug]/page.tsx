'use client';

import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
import { api, Community, friendlyError, Page, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { PostCard } from '@/components/PostCard';
import Link from 'next/link';

export default function CommunityPage() {
  const params = useParams<{ slug: string }>();
  const { token, user } = useAuth();
  const { success, error } = useToast();
  const [community, setCommunity] = useState<Community | null>(null);
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);

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
    if (params.slug) load();
  }, [params.slug, token]);

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

  if (loading && !community) {
    return <p className="muted" style={{ marginTop: '1.5rem' }}>Loading community…</p>;
  }
  if (!community) {
    return (
      <div className="panel" style={{ marginTop: '1.5rem' }}>
        <p>Community not found.</p>
        <Link href="/communities" className="btn secondary">Back</Link>
      </div>
    );
  }

  return (
    <>
      <section className="hero">
        <h1>c/{community.slug}</h1>
        <p>{community.description || 'A place for focused discussion.'}</p>
        <div className="toolbar">
          <span className="chip active">{community.memberCount} members</span>
          {user && !community.joined && (
            <button className="btn" type="button" onClick={join}>Join</button>
          )}
          {user && community.joined && (
            <button className="btn secondary" type="button" onClick={leave}>Leave</button>
          )}
          <Link className="btn secondary" href={`/submit?communityId=${community.id}`}>Post here</Link>
        </div>
      </section>
      <div className="stack">
        {posts.map((p) => <PostCard key={p.id} post={p} />)}
        {posts.length === 0 && <div className="panel muted">No posts in this community yet.</div>}
      </div>
    </>
  );
}
