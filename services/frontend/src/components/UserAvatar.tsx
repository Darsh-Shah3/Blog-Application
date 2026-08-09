'use client';

import { mediaContentUrl } from '@/lib/api';

type Props = {
  username?: string | null;
  avatarMediaId?: string | null;
  size?: number;
  className?: string;
  /** Prefer over avatarMediaId when already resolved */
  src?: string | null;
  /** Platform admin — always show brand mark */
  isAdmin?: boolean;
};

function letter(name?: string | null) {
  const t = (name || '?').trim();
  return (t[0] || '?').toUpperCase();
}

/** Shared circular/rounded profile picture with letter fallback (or brand mark for admins). */
export function UserAvatar({
  username,
  avatarMediaId,
  size = 40,
  className = '',
  src,
  isAdmin = false,
}: Props) {
  if (isAdmin) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src="/brand-mark.svg"
        alt={username ? `@${username}` : 'Threadly admin'}
        width={size}
        height={size}
        className={`user-dp user-dp-admin ${className}`.trim()}
        style={{ width: size, height: size }}
        loading="lazy"
      />
    );
  }

  const url = src
    || (avatarMediaId ? mediaContentUrl(avatarMediaId) : null);

  if (url) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={url}
        alt={username ? `@${username}` : 'Profile photo'}
        width={size}
        height={size}
        className={`user-dp ${className}`.trim()}
        style={{ width: size, height: size }}
        loading="lazy"
      />
    );
  }

  return (
    <span
      className={`user-dp user-dp-fallback ${className}`.trim()}
      style={{ width: size, height: size, fontSize: Math.max(12, size * 0.38) }}
      aria-hidden
    >
      {letter(username)}
    </span>
  );
}
