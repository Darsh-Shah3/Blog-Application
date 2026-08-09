'use client';

import Link from 'next/link';
import { useEffect, type MouseEvent } from 'react';
import { UserAvatar } from '@/components/UserAvatar';
import { isSiteAdmin, useAuthor, useAuthorDirectory } from '@/lib/authorDirectory';

/** Blue verified tick (admin / official accounts). */
export function VerifiedTick({ size = 14, title = 'Verified admin' }: { size?: number; title?: string }) {
  return (
    <svg
      className="verified-tick"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      aria-label={title}
      role="img"
    >
      <title>{title}</title>
      <circle cx="12" cy="12" r="11" fill="#1d9bf0" />
      <path
        d="M10.1 15.9 6.8 12.6l1.4-1.4 1.9 1.9 5-5.1 1.4 1.4-6.4 6.5z"
        fill="#fff"
      />
    </svg>
  );
}

type AvatarProps = {
  username?: string | null;
  size?: number;
  className?: string;
  avatarMediaId?: string | null;
  roles?: string[] | null;
};

/** Avatar that loads profile DP (or admin brand mark) from author directory. */
export function AuthorAvatar({
  username,
  size = 36,
  className = '',
  avatarMediaId,
  roles,
}: AvatarProps) {
  const card = useAuthor(username);
  if (card?.deleted) {
    return (
      <span
        className={`user-dp user-dp-fallback user-dp-deleted ${className}`.trim()}
        style={{ width: size, height: size, fontSize: Math.max(11, size * 0.32) }}
        title="Account deleted"
        aria-label="Deleted account"
      >
        ?
      </span>
    );
  }
  const resolvedRoles = roles || card?.roles;
  const admin = isSiteAdmin(resolvedRoles, username || card?.username);
  const mediaId = admin ? null : (avatarMediaId ?? card?.avatarMediaId);

  return (
    <UserAvatar
      username={username || card?.username}
      avatarMediaId={mediaId}
      isAdmin={admin}
      size={size}
      className={className}
    />
  );
}

type NameProps = {
  username?: string | null;
  href?: boolean;
  className?: string;
  roles?: string[] | null;
  onClick?: (e: MouseEvent) => void;
};

/** @username with admin blue tick, or “Deleted account” when deactivated. */
export function AuthorName({
  username,
  href = true,
  className = '',
  roles,
  onClick,
}: NameProps) {
  const card = useAuthor(username);
  if (card?.deleted) {
    return (
      <span className={`author-name author-name-deleted ${className}`.trim()} title={`was @${username}`}>
        Deleted account
      </span>
    );
  }
  const name = username || card?.username || 'user';
  const admin = isSiteAdmin(roles || card?.roles, name);
  const label = (
    <span className={`author-name ${className}`.trim()}>
      <span>{name.startsWith('@') ? name : `@${name}`}</span>
      {admin && <VerifiedTick size={13} />}
    </span>
  );
  if (!href || !username) return label;
  return (
    <Link href={`/u/${username.replace(/^@/, '')}`} className="author-name-link" onClick={onClick}>
      {label}
    </Link>
  );
}

/** Prefetch a list of usernames into the author cache. */
export function usePrefetchAuthors(usernames: (string | null | undefined)[]) {
  const { ensure } = useAuthorDirectory();
  const key = usernames.filter(Boolean).join('|');
  useEffect(() => {
    ensure(usernames);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, ensure]);
}
