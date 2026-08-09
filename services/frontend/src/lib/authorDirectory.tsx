'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { api, User } from '@/lib/api';
import { useAuth } from '@/lib/auth';

export type AuthorCard = {
  id?: string;
  username: string;
  displayName?: string;
  avatarMediaId?: string | null;
  roles?: string[];
  /** Soft-deleted / deactivated account */
  deleted?: boolean;
};

/** True for platform admins — use brand mark + blue verified tick. */
export function isSiteAdmin(roles?: string[] | null, username?: string | null) {
  if (roles?.some((r) => r === 'ROLE_ADMIN' || r === 'ADMIN')) return true;
  return (username || '').toLowerCase() === 'admin';
}

type Ctx = {
  get: (username?: string | null) => AuthorCard | undefined;
  ensure: (usernames: (string | null | undefined)[]) => void;
};

const AuthorDirectoryContext = createContext<Ctx | null>(null);

function toCard(u: User): AuthorCard {
  return {
    id: u.id,
    username: u.username,
    displayName: u.displayName,
    avatarMediaId: u.avatarMediaId,
    roles: u.roles,
    deleted: Boolean(u.deleted),
  };
}

export function AuthorDirectoryProvider({ children }: { children: ReactNode }) {
  const { user: me, token } = useAuth();
  const [byUsername, setByUsername] = useState<Record<string, AuthorCard>>({});
  const cacheRef = useRef(byUsername);
  cacheRef.current = byUsername;
  const pending = useRef(new Set<string>());
  const inFlight = useRef(new Set<string>());

  useEffect(() => {
    if (!me?.username) return;
    const key = me.username.toLowerCase();
    setByUsername((prev) => ({ ...prev, [key]: toCard(me) }));
  }, [me]);

  const ensure = useCallback((usernames: (string | null | undefined)[]) => {
    for (const raw of usernames) {
      const name = (raw || '').trim().replace(/^@/, '');
      if (!name) continue;
      const key = name.toLowerCase();
      if (cacheRef.current[key] || pending.current.has(key) || inFlight.current.has(key)) continue;
      pending.current.add(key);
    }
    queueMicrotask(() => {
      const batch = Array.from(pending.current).slice(0, 24);
      if (batch.length === 0) return;
      for (const k of batch) {
        pending.current.delete(k);
        inFlight.current.add(k);
      }
      Promise.all(
        batch.map(async (key) => {
          try {
            const u = await api<User>(`/api/v1/profiles/${encodeURIComponent(key)}`, {}, token || undefined);
            return { key, card: toCard(u) as AuthorCard | null };
          } catch {
            // Unknown handle — treat as unknown, not necessarily deleted
            return { key, card: { username: key } as AuthorCard };
          } finally {
            inFlight.current.delete(key);
          }
        })
      ).then((rows) => {
        setByUsername((prev) => {
          const next = { ...prev };
          for (const row of rows) {
            if (row.card) next[row.key] = row.card;
          }
          return next;
        });
      });
    });
  }, [token]);

  const get = useCallback(
    (username?: string | null) => {
      const key = (username || '').trim().replace(/^@/, '').toLowerCase();
      if (!key) return undefined;
      return byUsername[key];
    },
    [byUsername]
  );

  const value = useMemo(() => ({ get, ensure }), [get, ensure]);

  return (
    <AuthorDirectoryContext.Provider value={value}>
      {children}
    </AuthorDirectoryContext.Provider>
  );
}

export function useAuthorDirectory() {
  const ctx = useContext(AuthorDirectoryContext);
  if (!ctx) {
    return {
      get: () => undefined,
      ensure: () => undefined,
    } as Ctx;
  }
  return ctx;
}

/** Resolve author card; triggers background fetch when missing. */
export function useAuthor(username?: string | null) {
  const { get, ensure } = useAuthorDirectory();
  useEffect(() => {
    if (username) ensure([username]);
  }, [username, ensure]);
  return get(username);
}
