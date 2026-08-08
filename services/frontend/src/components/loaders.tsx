'use client';

import type { CSSProperties, ReactNode } from 'react';

/** Single shimmer bone (content placeholder). */
export function Skeleton({
  className = '',
  width,
  height,
  radius = 8,
  style,
}: {
  className?: string;
  width?: string | number;
  height?: string | number;
  radius?: number | string;
  style?: CSSProperties;
}) {
  return (
    <span
      className={`skeleton ${className}`.trim()}
      style={{
        width: width ?? '100%',
        height: height ?? '0.9rem',
        borderRadius: radius,
        ...style,
      }}
      aria-hidden="true"
    />
  );
}

/** YouTube-style circular spinner. */
export function Spinner({
  size = 28,
  label = 'Loading',
  className = '',
}: {
  size?: number;
  label?: string;
  className?: string;
}) {
  return (
    <span
      className={`spinner ${className}`.trim()}
      style={{ width: size, height: size }}
      role="status"
      aria-label={label}
    >
      <span className="spinner-ring" />
    </span>
  );
}

/** Inline row: spinner + label (buttons, forms, load-more). */
export function SpinnerLabel({
  label = 'Loading…',
  size = 16,
}: {
  label?: string;
  size?: number;
}) {
  return (
    <span className="spinner-label">
      <Spinner size={size} label={label} />
      <span>{label}</span>
    </span>
  );
}

/** Centered page/section spinner with optional message. */
export function PageSpinner({
  label = 'Loading…',
  className = '',
}: {
  label?: string;
  className?: string;
}) {
  return (
    <div className={`page-spinner ${className}`.trim()} role="status" aria-live="polite">
      <Spinner size={36} label={label} />
      <p className="muted page-spinner-text">{label}</p>
    </div>
  );
}

/** Empty-content placeholder (soft card with icon area + lines). */
export function ContentPlaceholder({
  title = 'Nothing here yet',
  description,
  action,
}: {
  title?: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="content-placeholder panel">
      <div className="content-placeholder-icon" aria-hidden>
        <span />
      </div>
      <h3 className="content-placeholder-title">{title}</h3>
      {description && <p className="muted content-placeholder-desc">{description}</p>}
      {action && <div className="content-placeholder-action">{action}</div>}
    </div>
  );
}

/** YouTube-ish post row skeleton. */
export function PostSkeleton() {
  return (
    <article className="post-row post-skeleton" aria-hidden="true">
      <div className="vote-col">
        <Skeleton width={28} height={28} radius={8} />
        <Skeleton width={22} height={14} radius={4} style={{ margin: '0.25rem 0' }} />
        <Skeleton width={28} height={28} radius={8} />
      </div>
      <div className="stack" style={{ gap: '0.55rem', flex: 1 }}>
        <div style={{ display: 'flex', gap: '0.45rem', flexWrap: 'wrap' }}>
          <Skeleton width="5rem" height={12} />
          <Skeleton width="4.5rem" height={12} />
          <Skeleton width="7rem" height={12} />
        </div>
        <Skeleton width="78%" height={20} radius={6} />
        <Skeleton width="100%" height={12} />
        <Skeleton width="92%" height={12} />
        <Skeleton width="55%" height={12} />
        <div style={{ display: 'flex', gap: '0.65rem', marginTop: '0.25rem' }}>
          <Skeleton width="6rem" height={14} radius={999} />
          <Skeleton width="4rem" height={14} radius={999} />
        </div>
      </div>
    </article>
  );
}

export function PostSkeletonList({ count = 4 }: { count?: number }) {
  return (
    <div className="stack" aria-busy="true" aria-label="Loading posts">
      {Array.from({ length: count }, (_, i) => (
        <PostSkeleton key={i} />
      ))}
    </div>
  );
}

/** Community card skeleton. */
export function CommunitySkeleton() {
  return (
    <article className="panel community-skeleton" aria-hidden="true">
      <Skeleton width="40%" height={22} radius={6} style={{ marginBottom: '0.65rem' }} />
      <Skeleton width="100%" height={12} />
      <Skeleton width="88%" height={12} style={{ marginTop: '0.35rem' }} />
      <Skeleton width="30%" height={12} style={{ marginTop: '0.65rem' }} />
    </article>
  );
}

export function CommunitySkeletonList({ count = 4 }: { count?: number }) {
  return (
    <div className="stack" aria-busy="true" aria-label="Loading communities">
      {Array.from({ length: count }, (_, i) => (
        <CommunitySkeleton key={i} />
      ))}
    </div>
  );
}

/** Notification card skeleton (thumb + text). */
export function NotificationSkeleton() {
  return (
    <article className="notif-card notif-skeleton" aria-hidden="true">
      <Skeleton className="notif-thumb-skel" width={88} height={64} radius={8} />
      <div className="stack" style={{ gap: '0.45rem', flex: 1 }}>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <Skeleton width="5rem" height={12} />
          <Skeleton width="7rem" height={12} />
        </div>
        <Skeleton width="70%" height={16} />
        <Skeleton width="95%" height={12} />
        <Skeleton width="5rem" height={28} radius={999} style={{ marginTop: '0.25rem' }} />
      </div>
    </article>
  );
}

export function NotificationSkeletonList({ count = 5 }: { count?: number }) {
  return (
    <div className="stack notif-stack" aria-busy="true" aria-label="Loading notifications">
      {Array.from({ length: count }, (_, i) => (
        <NotificationSkeleton key={i} />
      ))}
    </div>
  );
}

/** Profile header + posts skeleton. */
export function ProfileSkeleton() {
  return (
    <div className="stack" aria-busy="true" aria-label="Loading profile">
      <section className="hero" style={{ paddingTop: '1rem' }}>
        <Skeleton width="12rem" height={36} radius={8} style={{ marginBottom: '0.75rem' }} />
        <Skeleton width="60%" height={14} />
        <Skeleton width="40%" height={14} style={{ marginTop: '0.4rem' }} />
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.85rem' }}>
          <Skeleton width="5.5rem" height={34} radius={999} />
          <Skeleton width="4.5rem" height={34} radius={999} />
        </div>
      </section>
      <PostSkeletonList count={3} />
    </div>
  );
}

/** Full post detail page skeleton. */
export function PostDetailSkeleton() {
  return (
    <div className="stack" style={{ marginTop: '1.25rem' }} aria-busy="true" aria-label="Loading post">
      <Skeleton width="5rem" height={14} />
      <PostSkeleton />
      <div className="panel">
        <Skeleton width="5rem" height={22} style={{ marginBottom: '0.85rem' }} />
        <Skeleton width="100%" height={12} />
        <Skeleton width="100%" height={12} style={{ marginTop: '0.4rem' }} />
        <Skeleton width="80%" height={12} style={{ marginTop: '0.4rem' }} />
      </div>
      <div className="panel">
        <Skeleton width="7rem" height={22} style={{ marginBottom: '0.85rem' }} />
        <Skeleton width="100%" height={72} radius={10} style={{ marginBottom: '0.75rem' }} />
        <Skeleton width="6rem" height={34} radius={999} />
        <div style={{ marginTop: '1rem' }} className="stack">
          <Skeleton width="30%" height={12} />
          <Skeleton width="90%" height={12} />
          <Skeleton width="75%" height={12} />
        </div>
      </div>
    </div>
  );
}

/** Community page header + posts. */
export function CommunityDetailSkeleton() {
  return (
    <div aria-busy="true" aria-label="Loading community">
      <section className="hero">
        <Skeleton width="14rem" height={36} radius={8} style={{ marginBottom: '0.65rem' }} />
        <Skeleton width="55%" height={14} />
        <div className="toolbar" style={{ marginTop: '0.85rem' }}>
          <Skeleton width="6rem" height={32} radius={999} />
          <Skeleton width="4.5rem" height={34} radius={999} />
          <Skeleton width="5.5rem" height={34} radius={999} />
        </div>
      </section>
      <PostSkeletonList count={3} />
    </div>
  );
}

/** Admin report cards + user rows. */
export function AdminSkeleton() {
  return (
    <div className="stack" style={{ marginTop: '1.25rem' }} aria-busy="true" aria-label="Loading admin">
      <section className="hero" style={{ paddingTop: '1.5rem' }}>
        <Skeleton width="8rem" height={36} />
        <Skeleton width="70%" height={14} style={{ marginTop: '0.65rem' }} />
      </section>
      <div className="panel">
        <Skeleton width="9rem" height={22} style={{ marginBottom: '0.85rem' }} />
        <div className="report-grid">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i}>
              <Skeleton width="3rem" height={28} />
              <Skeleton width="5rem" height={12} style={{ marginTop: '0.35rem' }} />
            </div>
          ))}
        </div>
      </div>
      <div className="panel">
        <Skeleton width="5rem" height={22} style={{ marginBottom: '0.85rem' }} />
        <Skeleton width="100%" height={40} radius={10} style={{ marginBottom: '0.75rem' }} />
        {Array.from({ length: 3 }, (_, i) => (
          <div key={i} className="admin-user-row">
            <div style={{ flex: 1 }}>
              <Skeleton width="40%" height={16} />
              <Skeleton width="60%" height={12} style={{ marginTop: '0.4rem' }} />
            </div>
            <Skeleton width="8rem" height={34} radius={999} />
          </div>
        ))}
      </div>
    </div>
  );
}

/** Form panel placeholder (submit / auth suspense). */
export function FormSkeleton({ lines = 4 }: { lines?: number }) {
  return (
    <div className="panel" style={{ maxWidth: 640, margin: '1.5rem auto' }} aria-busy="true">
      <Skeleton width="10rem" height={28} style={{ marginBottom: '1rem' }} />
      {Array.from({ length: lines }, (_, i) => (
        <div key={i} style={{ marginBottom: '0.85rem' }}>
          <Skeleton width="5rem" height={12} style={{ marginBottom: '0.4rem' }} />
          <Skeleton width="100%" height={40} radius={10} />
        </div>
      ))}
      <Skeleton width="7rem" height={40} radius={999} />
    </div>
  );
}
