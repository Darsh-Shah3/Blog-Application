'use client';

import { useEffect, useRef, useState } from 'react';
import { useParams } from 'next/navigation';
import {
  api,
  FollowStatus,
  friendlyError,
  getApiUrl,
  Page,
  Post,
  User,
} from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { useRouter } from 'next/navigation';
import { PostCard } from '@/components/PostCard';
import { UserAvatar } from '@/components/UserAvatar';
import { VerifiedTick } from '@/components/AuthorIdentity';
import { isSiteAdmin } from '@/lib/authorDirectory';
import { isAdmin, roleLabel, ROLES } from '@/lib/rbac';
import Link from 'next/link';
import { ContentPlaceholder, ProfileSkeleton, SpinnerLabel } from '@/components/loaders';

const ALL_ROLES = [ROLES.USER, ROLES.MODERATOR, ROLES.ADMIN];

export default function ProfilePage() {
  const params = useParams<{ username: string }>();
  const router = useRouter();
  const { user: me, token, refreshMe, logout } = useAuth();
  const { error, success, confirm } = useToast();
  const [user, setUser] = useState<User | null>(null);
  const [posts, setPosts] = useState<Post[]>([]);
  const [follow, setFollow] = useState<FollowStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [uploadingDp, setUploadingDp] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [roleBusy, setRoleBusy] = useState(false);
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const fileRef = useRef<HTMLInputElement>(null);

  const load = async () => {
    if (!params.username) return;
    setLoading(true);
    try {
      const u = await api<User>(`/api/v1/profiles/${params.username}`, {}, token);
      setUser(u);
      setSelectedRoles(u.roles?.length ? [...u.roles] : [ROLES.USER]);
      if (!u.deleted) {
        const page = await api<Page<Post>>(`/api/v1/posts?authorId=${u.id}&sort=new&size=30`, {}, token);
        setPosts(page.content || []);
        try {
          const st = await api<FollowStatus>(`/api/v1/profiles/${params.username}/follow-status`, {}, token);
          setFollow(st);
        } catch {
          setFollow(null);
        }
      } else {
        // Keep posts visible for soft-deleted accounts
        const page = await api<Page<Post>>(`/api/v1/posts?authorId=${u.id}&sort=new&size=30`, {}, token);
        setPosts(page.content || []);
        setFollow(null);
      }
    } catch (e) {
      error(friendlyError(e, 'Could not load profile.'));
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
      success(st.following ? `Following ${user.username}` : `Unfollowed ${user.username}`);
    } catch (e) {
      error(friendlyError(e));
    } finally {
      setBusy(false);
    }
  };

  const uploadDp = async (file: File | null) => {
    if (!file || !token) return;
    if (!file.type.startsWith('image/')) {
      error('Please choose an image file (JPEG, PNG, WebP, GIF).');
      return;
    }
    if (file.size > 8 * 1024 * 1024) {
      error('Image must be under 8 MB.');
      return;
    }
    setUploadingDp(true);
    try {
      const form = new FormData();
      form.append('file', file);
      const res = await fetch(`${getApiUrl()}/api/v1/media/uploads`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form,
      });
      if (!res.ok) {
        let message = `Upload failed (${res.status})`;
        try {
          const body = await res.json();
          message = body.message || message;
        } catch {
          // ignore
        }
        throw new Error(message);
      }
      const meta = (await res.json()) as { id: string; kind?: string };
      if (meta.kind && meta.kind !== 'IMAGE') {
        throw new Error('Only image files can be used as a profile photo.');
      }
      const updated = await api<User>(
        '/api/v1/auth/profile',
        {
          method: 'PUT',
          body: JSON.stringify({ avatarMediaId: meta.id }),
        },
        token
      );
      setUser(updated);
      await refreshMe();
      success('Profile photo updated.');
    } catch (e) {
      error(friendlyError(e, 'Could not update profile photo.'));
    } finally {
      setUploadingDp(false);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  const removeDp = async () => {
    if (!token) return;
    setUploadingDp(true);
    try {
      const updated = await api<User>(
        '/api/v1/auth/profile',
        {
          method: 'PUT',
          body: JSON.stringify({ avatarMediaId: '' }),
        },
        token
      );
      setUser(updated);
      await refreshMe();
      success('Profile photo removed.');
    } catch (e) {
      error(friendlyError(e, 'Could not remove profile photo.'));
    } finally {
      setUploadingDp(false);
    }
  };

  const deleteOwnAccount = async () => {
    if (!token || !user) return;
    const ok = await confirm({
      title: 'Delete your account?',
      message:
        'Your profile will be deactivated. You will not be able to sign in again. Your posts stay on Threadly and will show as from a deleted account.',
      confirmLabel: 'Delete account',
    });
    if (!ok) return;
    setDeleting(true);
    try {
      await api('/api/v1/auth/account', { method: 'DELETE' }, token);
      success('Account deleted.');
      logout();
      router.replace('/');
    } catch (e) {
      error(friendlyError(e, 'Could not delete account.'));
      setDeleting(false);
    }
  };

  const toggleProfileRole = (role: string) => {
    setSelectedRoles((prev) => {
      const current = new Set(prev.length ? prev : [ROLES.USER]);
      if (current.has(role)) {
        if (role === ROLES.USER && current.size === 1) return prev;
        current.delete(role);
      } else {
        current.add(role);
      }
      if (role === ROLES.ADMIN || role === ROLES.MODERATOR) {
        current.add(ROLES.USER);
      }
      return Array.from(current);
    });
  };

  const setPresetRoles = (tier: 'user' | 'moderator' | 'admin') => {
    if (tier === 'admin') setSelectedRoles([ROLES.USER, ROLES.ADMIN]);
    else if (tier === 'moderator') setSelectedRoles([ROLES.USER, ROLES.MODERATOR]);
    else setSelectedRoles([ROLES.USER]);
  };

  const saveRoles = async () => {
    if (!token || !user || user.deleted) return;
    setRoleBusy(true);
    try {
      const updated = await api<User>(
        `/api/v1/admin/users/${user.id}/roles`,
        {
          method: 'PUT',
          body: JSON.stringify({ roles: selectedRoles.length ? selectedRoles : [ROLES.USER] }),
        },
        token
      );
      setUser((prev) => (prev ? { ...prev, roles: updated.roles } : prev));
      setSelectedRoles(updated.roles?.length ? [...updated.roles] : [ROLES.USER]);
      success('Roles saved. They must sign in again for new rights.');
    } catch (e) {
      error(friendlyError(e, 'Could not update roles.'));
    } finally {
      setRoleBusy(false);
    }
  };

  if (loading && !user) {
    return <ProfileSkeleton />;
  }
  if (!user) {
    return (
      <div className="page-shell soft-banner" style={{ marginTop: '1.5rem' }}>
        <div>
          <p>User not found.</p>
          <Link href="/" className="btn secondary">Home</Link>
        </div>
      </div>
    );
  }

  const isSelf = me?.id === user.id;
  const admin = !user.deleted && isSiteAdmin(user.roles, user.username);
  const deactivated = Boolean(user.deleted);
  const canManageRoles = Boolean(me && token && isAdmin(me) && !deactivated);

  return (
    <div className="page-shell page-shell-wide profile-page">
      <section className="profile-hero">
        <div className="profile-hero-row">
          <div className="profile-dp-wrap">
            <UserAvatar
              username={user.username}
              avatarMediaId={deactivated ? null : user.avatarMediaId}
              isAdmin={admin}
              size={112}
              className={`profile-dp${deactivated ? ' user-dp-deleted' : ''}`}
            />
            {isSelf && !admin && !deactivated && (
              <div className="profile-dp-actions">
                <input
                  ref={fileRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  hidden
                  onChange={(e) => uploadDp(e.target.files?.[0] || null)}
                />
                <button
                  type="button"
                  className="btn secondary"
                  disabled={uploadingDp}
                  onClick={() => fileRef.current?.click()}
                >
                  {uploadingDp ? (
                    <SpinnerLabel label="Updating…" />
                  ) : user.avatarMediaId ? (
                    'Change photo'
                  ) : (
                    'Add photo'
                  )}
                </button>
                {user.avatarMediaId && (
                  <button
                    type="button"
                    className="btn ghost"
                    disabled={uploadingDp}
                    onClick={removeDp}
                  >
                    Remove
                  </button>
                )}
              </div>
            )}
            {admin && (
              <p className="muted" style={{ margin: 0, fontSize: '0.82rem', textAlign: 'center', maxWidth: '11rem' }}>
                Official Threadly account
              </p>
            )}
          </div>
          <div className="profile-hero-meta">
            <h1 className="profile-title-row">
              <span>{deactivated ? 'Deleted account' : (user.displayName || user.username)}</span>
              {admin && <VerifiedTick size={22} title="Verified admin" />}
            </h1>
            {deactivated ? (
              <p className="profile-deactivated-banner">
                This account has been deactivated. Posts below may still be visible; the member can no longer sign in.
              </p>
            ) : (
              <p>
                @{user.username} · {user.karma} karma
                {follow ? ` · ${follow.followerCount} followers · ${follow.followingCount} following` : ''}
              </p>
            )}
            {!deactivated && user.bio ? <p className="profile-bio">{user.bio}</p> : null}
            {!deactivated && user.roles && user.roles.length > 0 && (
              <div className="role-badges" style={{ marginTop: '0.65rem' }}>
                {user.roles.map((r) => (
                  <span key={r} className="role-badge">{roleLabel(r)}</span>
                ))}
              </div>
            )}
            {canManageRoles && (
              <div className="profile-admin-roles">
                <h3>Admin · change roles</h3>
                <p className="muted" style={{ margin: 0, fontSize: '0.85rem' }}>
                  Assign Member, Moderator, or Admin for @{user.username}. Save to apply.
                </p>
                <div className="role-presets" style={{ marginTop: '0.55rem' }}>
                  <button type="button" className="btn secondary btn-sm" onClick={() => setPresetRoles('user')}>Member</button>
                  <button type="button" className="btn secondary btn-sm" onClick={() => setPresetRoles('moderator')}>Moderator</button>
                  <button type="button" className="btn secondary btn-sm" onClick={() => setPresetRoles('admin')}>Admin</button>
                </div>
                <div className="role-toggles">
                  {ALL_ROLES.map((role) => (
                    <label key={role} className="role-check">
                      <input
                        type="checkbox"
                        checked={selectedRoles.includes(role)}
                        onChange={() => toggleProfileRole(role)}
                      />
                      {roleLabel(role)}
                    </label>
                  ))}
                  <button type="button" className="btn" disabled={roleBusy} onClick={saveRoles}>
                    {roleBusy ? <SpinnerLabel label="Saving…" /> : 'Save roles'}
                  </button>
                </div>
              </div>
            )}
            {isSelf && !deactivated && (
              <div className="toolbar" style={{ marginTop: '0.85rem' }}>
                <button
                  type="button"
                  className="btn ghost danger-text"
                  disabled={deleting}
                  onClick={deleteOwnAccount}
                >
                  {deleting ? <SpinnerLabel label="Deleting…" /> : 'Delete my account'}
                </button>
              </div>
            )}
            {!isSelf && !deactivated && me && (
              <div className="toolbar" style={{ marginTop: '0.85rem' }}>
                <button type="button" className="btn" disabled={busy} onClick={toggleFollow}>
                  {busy ? <SpinnerLabel label="Updating…" /> : follow?.following ? 'Unfollow' : 'Follow'}
                </button>
                <Link className="btn secondary" href={`/messages?with=${encodeURIComponent(user.username)}`}>
                  Message
                </Link>
              </div>
            )}
            {!isSelf && !deactivated && !me && (
              <div className="toolbar" style={{ marginTop: '0.85rem' }}>
                <Link className="btn" href={`/login?next=${encodeURIComponent(`/u/${user.username}`)}`}>
                  Log in to message
                </Link>
              </div>
            )}
          </div>
        </div>
      </section>
      <div className="stack feed-list profile-posts">
        {posts.map((p) => <PostCard key={p.id} post={p} />)}
        {posts.length === 0 && (
          <ContentPlaceholder title="No posts yet" description="This user has not published anything." />
        )}
      </div>
    </div>
  );
}
