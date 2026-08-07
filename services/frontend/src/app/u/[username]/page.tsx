'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { api, FollowStatus, friendlyError, Page, Post, User } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { PostCard } from '@/components/PostCard';
import Link from 'next/link';

export default function ProfilePage() {
  const params = useParams<{ username: string }>();
  const { user: me, token } = useAuth();
  const { error, success } = useToast();
  const [user, setUser] = useState<User | null>(null);
  const [posts, setPosts] = useState<Post[]>([]);
  const [follow, setFollow] = useState<FollowStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const load = async () => {
    if (!params.username) return;
    setLoading(true);
    try {
      const u = await api<User>(`/api/v1/profiles/${params.username}`, {}, token);
      setUser(u);
      const page = await api<Page<Post>>(`/api/v1/posts?authorId=${u.id}&sort=new&size=30`, {}, token);
      setPosts(page.content || []);
      const st = await api<FollowStatus>(`/api/v1/profiles/${params.username}/follow-status`, {}, token);
      setFollow(st);
    } catch (e) {
      error(friendlyError(e, 'Could not load profile.'));
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [params.username, token]);

  const toggleFollow = async () => {
    if (!token || !user || !me) return;
    setBusy(true);
    try {
      const method = follow?.following ? 'DELETE' : 'POST';
      const st = await api<FollowStatus>(
        `/api/v1/profiles/${user.username}/follow`,
        { method },
        token
      );
      setFollow(st);
      success(st.following ? `Following @${user.username}` : `Unfollowed @${user.username}`);
    } catch (e) {
      error(friendlyError(e));
    } finally {
      setBusy(false);
    }
  };

  if (loading && !user) {
    return <p className="muted" style={{ marginTop: '1.5rem' }}>Loading profile…</p>;
  }
  if (!user) {
    return (
      <div className="panel" style={{ marginTop: '1.5rem' }}>
        <p>User not found.</p>
        <Link href="/" className="btn secondary">Home</Link>
      </div>
    );
  }

  const isSelf = me?.id === user.id;

  return (
    <>
      <section className="hero">
        <h1>u/{user.username}</h1>
        <p>
          {user.displayName} · {user.karma} karma
          {follow ? ` · ${follow.followerCount} followers · ${follow.followingCount} following` : ''}
          {user.bio ? ` — ${user.bio}` : ''}
        </p>
        {user.roles && user.roles.length > 0 && (
          <div className="role-badges" style={{ marginTop: '0.75rem' }}>
            {user.roles.map((r) => (
              <span key={r} className="role-badge">{r.replace(/^ROLE_/, '')}</span>
            ))}
          </div>
        )}
        {!isSelf && me && (
          <div className="toolbar" style={{ marginTop: '0.85rem' }}>
            <button type="button" className="btn" disabled={busy} onClick={toggleFollow}>
              {busy ? '…' : follow?.following ? 'Unfollow' : 'Follow'}
            </button>
          </div>
        )}
      </section>
      <div className="stack">
        {posts.map((p) => <PostCard key={p.id} post={p} />)}
        {posts.length === 0 && <div className="panel muted">No posts yet.</div>}
      </div>
    </>
  );
}
