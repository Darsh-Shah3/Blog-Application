'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  assignAdminRoles,
  createAdminUser,
  deleteAdminUser,
  friendlyError,
  listAdminRoles,
  listAdminUsers,
  RoleInfo,
  User,
} from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { isAdmin, roleLabel, ROLES } from '@/lib/rbac';
import { AdminSkeleton, SpinnerLabel } from '@/components/loaders';
import { AdminPageShell } from '@/components/AdminShell';
import { TopBar } from '@/components/TopBar';
import { UserAvatar } from '@/components/UserAvatar';
import { isSiteAdmin } from '@/lib/authorDirectory';

type Tier = 'user' | 'moderator' | 'admin';

function rolesForTier(tier: Tier): string[] {
  if (tier === 'admin') return [ROLES.USER, ROLES.ADMIN];
  if (tier === 'moderator') return [ROLES.USER, ROLES.MODERATOR];
  return [ROLES.USER];
}

function tierFromRoles(roles: string[] | undefined): Tier {
  if (roles?.includes(ROLES.ADMIN)) return 'admin';
  if (roles?.includes(ROLES.MODERATOR)) return 'moderator';
  return 'user';
}

export default function AdminUsersPage() {
  const { user, token, loading } = useAuth();
  const router = useRouter();
  const { success, error, info, confirm } = useToast();

  const [users, setUsers] = useState<User[]>([]);
  const [rolesCatalog, setRolesCatalog] = useState<RoleInfo[]>([]);
  const [q, setQ] = useState('');
  const [selected, setSelected] = useState<Record<string, string[]>>({});
  const [busyId, setBusyId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [createTier, setCreateTier] = useState<Tier>('user');

  useEffect(() => {
    if (loading) return;
    if (!user || !isAdmin(user)) {
      info('Admin access required.');
      router.replace('/');
    }
  }, [user, loading, router, info]);

  const load = async (search = q) => {
    if (!token || !isAdmin(user)) return;
    try {
      const [page, catalog] = await Promise.all([
        listAdminUsers(token, search),
        listAdminRoles(token),
      ]);
      setUsers(page.content || []);
      setRolesCatalog(catalog || []);
      const map: Record<string, string[]> = {};
      (page.content || []).forEach((u) => {
        map[u.id] = u.roles?.length ? [...u.roles] : [ROLES.USER];
      });
      setSelected(map);
    } catch (e) {
      error(friendlyError(e, 'Could not load users.'));
    }
  };

  useEffect(() => {
    if (token && isAdmin(user)) {
      load();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, user?.id]);

  const sortedCatalog = useMemo(() => {
    const order = [ROLES.USER, ROLES.MODERATOR, ROLES.ADMIN];
    return [...rolesCatalog].sort(
      (a, b) => order.indexOf(a.name) - order.indexOf(b.name)
    );
  }, [rolesCatalog]);

  const setPresetRoles = (userId: string, tier: Tier) => {
    setSelected((prev) => ({ ...prev, [userId]: rolesForTier(tier) }));
  };

  const saveRoles = async (userId: string) => {
    if (!token) return;
    setBusyId(userId);
    try {
      const roles = selected[userId]?.length ? selected[userId] : [ROLES.USER];
      await assignAdminRoles(token, userId, roles);
      success('Access updated. User must sign in again for the new JWT rights.');
      await load();
    } catch (e) {
      error(friendlyError(e, 'Could not update access.'));
    } finally {
      setBusyId(null);
    }
  };

  const deleteUser = async (u: User) => {
    if (!token || !user) return;
    if (u.id === user.id) {
      error('Use your profile to delete your own account.');
      return;
    }
    const ok = await confirm({
      title: `Delete @${u.username}?`,
      message:
        'Soft-deletes the account. Posts stay on Threadly and will show as from a deleted account. The member is emailed if SMTP is enabled.',
      confirmLabel: 'Delete account',
    });
    if (!ok) return;
    setDeletingId(u.id);
    try {
      await deleteAdminUser(token, u.id);
      success(`@${u.username} account deleted.`);
      await load();
    } catch (e) {
      error(friendlyError(e, 'Could not delete user.'));
    } finally {
      setDeletingId(null);
    }
  };

  const onCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) return;
    setCreating(true);
    try {
      const created = await createAdminUser(token, {
        username: username.trim(),
        email: email.trim(),
        password,
        displayName: displayName.trim() || undefined,
        roles: rolesForTier(createTier),
      });
      const accessLabel =
        createTier === 'admin' ? 'Admin' : createTier === 'moderator' ? 'Moderator' : 'Member';
      success(`Created @${created.username} with ${accessLabel} access.`);
      setUsername('');
      setEmail('');
      setPassword('');
      setDisplayName('');
      setCreateTier('user');
      await load();
    } catch (err) {
      error(friendlyError(err, 'Could not create user.'));
    } finally {
      setCreating(false);
    }
  };

  if (loading || !user || !isAdmin(user)) {
    return <AdminSkeleton />;
  }

  return (
    <AdminPageShell
      kicker="Admin console"
      title="Users & access"
      blurb="Create accounts and grant or revoke platform roles. Access is role-based (RBAC): permissions come from the assigned role."
      trailing={(
        <button type="button" className="btn secondary" onClick={() => load()}>
          Refresh
        </button>
      )}
    >
      <TopBar
        crumb="Users & access"
        searchPlaceholder="Search username or email…"
        defaultSearch={q}
        onSearch={(value) => {
          setQ(value);
          load(value);
        }}
      />

      <div className="admin-split">
        <section className="admin-card">
          <header className="admin-card-head">
            <h2>Create user</h2>
          </header>
          <p className="muted admin-role-help">
            Provisions an account without public signup. Pick the initial access tier below.
          </p>
          <form className="admin-create-form" onSubmit={onCreate}>
            <label className="admin-field">
              <span>Username</span>
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                minLength={3}
                maxLength={50}
                required
                autoComplete="off"
              />
            </label>
            <label className="admin-field">
              <span>Email</span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="off"
              />
            </label>
            <label className="admin-field">
              <span>Temporary password</span>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                minLength={6}
                maxLength={100}
                required
                autoComplete="new-password"
              />
            </label>
            <label className="admin-field">
              <span>Display name (optional)</span>
              <input
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                maxLength={100}
                autoComplete="off"
              />
            </label>
            <div className="admin-field">
              <span>Initial access</span>
              <div className="role-presets" role="group" aria-label="Initial role">
                {([
                  ['user', 'Member'],
                  ['moderator', 'Moderator'],
                  ['admin', 'Admin'],
                ] as const).map(([tier, label]) => (
                  <button
                    key={tier}
                    type="button"
                    className={`role-pill${createTier === tier ? ' is-active' : ''}`}
                    onClick={() => setCreateTier(tier)}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
            <button className="btn" type="submit" disabled={creating}>
              {creating ? <SpinnerLabel label="Creating…" /> : 'Create user'}
            </button>
          </form>
        </section>

        <section className="admin-card">
          <header className="admin-card-head">
            <h2>Role catalog</h2>
            <span className="muted">{sortedCatalog.length} roles</span>
          </header>
          <p className="muted admin-role-help">
            Permissions are derived from roles. Assigning a role grants those rights; removing it takes them away.
          </p>
          <ul className="admin-role-catalog">
            {sortedCatalog.map((role) => (
              <li key={role.name} className="admin-role-item">
                <strong>{role.displayName || roleLabel(role.name)}</strong>
                <p className="muted">{role.description || '—'}</p>
                <div className="role-badges">
                  {(role.permissions || []).map((p) => (
                    <span key={p} className="role-badge role-badge-perm">{p}</span>
                  ))}
                  {(role.permissions || []).length === 0 && (
                    <span className="muted">No permissions mapped</span>
                  )}
                </div>
              </li>
            ))}
            {sortedCatalog.length === 0 && <li className="muted">No roles loaded.</li>}
          </ul>
        </section>
      </div>

      <section className="admin-card">
        <header className="admin-card-head">
          <h2>Grant or revoke access</h2>
          <span className="muted">{users.length} loaded</span>
        </header>
        <p className="muted admin-role-help">
          Choose Member, Moderator, or Admin, then save. The user must sign in again for the new rights to apply.
          {' '}
          <Link href="/admin">Back to control center</Link>
        </p>
        <div className="admin-user-table">
          {users.map((u) => {
            const current = selected[u.id] || [ROLES.USER];
            const tier = tierFromRoles(current);
            return (
              <article key={u.id} className="admin-user-card">
                <div className="admin-user-main">
                  <UserAvatar
                    username={u.username}
                    avatarMediaId={u.avatarMediaId}
                    isAdmin={isSiteAdmin(u.roles, u.username)}
                    size={40}
                    className="admin-user-avatar"
                  />
                  <div className="admin-user-copy">
                    <strong className="admin-user-name">
                      <Link href={`/u/${u.username}`}>@{u.username}</Link>
                    </strong>
                    <p className="muted admin-user-meta">
                      {u.email || '—'} · karma {u.karma ?? 0}
                    </p>
                    <div className="role-badges">
                      {(u.roles || []).map((r) => (
                        <span key={r} className="role-badge">{roleLabel(r)}</span>
                      ))}
                    </div>
                  </div>
                </div>
                <div className="admin-user-actions">
                  <div className="role-presets" role="group" aria-label={`Access for ${u.username}`}>
                    <button
                      type="button"
                      className={`role-pill${tier === 'user' ? ' is-active' : ''}`}
                      onClick={() => setPresetRoles(u.id, 'user')}
                    >
                      Member
                    </button>
                    <button
                      type="button"
                      className={`role-pill${tier === 'moderator' ? ' is-active' : ''}`}
                      onClick={() => setPresetRoles(u.id, 'moderator')}
                    >
                      Moderator
                    </button>
                    <button
                      type="button"
                      className={`role-pill${tier === 'admin' ? ' is-active' : ''}`}
                      onClick={() => setPresetRoles(u.id, 'admin')}
                    >
                      Admin
                    </button>
                  </div>
                  <div className="admin-user-buttons">
                    <button
                      className="btn"
                      type="button"
                      disabled={busyId === u.id}
                      onClick={() => saveRoles(u.id)}
                    >
                      {busyId === u.id ? <SpinnerLabel label="Saving…" /> : 'Save access'}
                    </button>
                    {u.id !== user.id && (
                      <button
                        className="btn ghost"
                        type="button"
                        disabled={deletingId === u.id}
                        onClick={() => deleteUser(u)}
                        title="Soft-delete account"
                      >
                        {deletingId === u.id ? <SpinnerLabel label="Deleting…" /> : 'Delete'}
                      </button>
                    )}
                  </div>
                </div>
              </article>
            );
          })}
          {users.length === 0 && <p className="muted">No users found.</p>}
        </div>
      </section>
    </AdminPageShell>
  );
}
