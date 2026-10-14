'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState, type MouseEvent } from 'react';
import { api, friendlyError, Post, mediaContentUrl } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { canDeleteContent } from '@/lib/rbac';
import { DeleteIconBtn, ViewIconLink } from '@/components/ActionIcon';
import { MessageCircleIcon } from '@/components/icons';
import { AuthorAvatar, AuthorName } from '@/components/AuthorIdentity';
import { usePrefetchAuthors } from '@/components/AuthorIdentity';
import { renderLinkedText } from '@/lib/richText';

export function PostCard({
  post,
  onScoreChange,
  onDeleted,
  expandedBody = false,
}: {
  post: Post;
  onScoreChange?: (p: Post) => void;
  onDeleted?: (id: string) => void;
  /** When true, skip truncated content (detail page shows full body). */
  expandedBody?: boolean;
}) {
  const { token, user } = useAuth();
  const router = useRouter();
  const { success, error, info, confirm } = useToast();
  const [score, setScore] = useState(post.score);
  const [myVote, setMyVote] = useState<number>(0);
  const [busy, setBusy] = useState(false);

  const firstImage = post.media?.find((m) => (m.mediaKind || '').toUpperCase() === 'IMAGE')
    || (post.postType === 'IMAGE' ? post.media?.[0] : undefined);
  const imageSrc = firstImage?.url
    || (firstImage?.id ? mediaContentUrl(firstImage.id) : null);

  const openPost = () => {
    if (!expandedBody) router.push(`/post/${post.id}`);
  };

  const stop = (e: MouseEvent) => e.stopPropagation();

  const vote = async (value: number) => {
    if (!token || !user) {
      info('Please sign in to vote.');
      return;
    }
    if (busy) return;
    setBusy(true);
    try {
      const res = await api<{ scoreDeltaApplied: number; value: number }>(
        '/api/v1/votes',
        {
          method: 'POST',
          body: JSON.stringify({
            targetType: 'POST',
            targetId: post.id,
            targetName: post.title,
            value: nextValue(myVote, value),
          }),
        },
        token
      );
      const nextVote = res.value;
      const updated = score + (res.scoreDeltaApplied || 0);
      setScore(updated);
      setMyVote(nextVote);
      onScoreChange?.({ ...post, score: updated });
    } catch (e) {
      error(friendlyError(e, 'Could not save vote.'));
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!token || !canDeleteContent(user, post.authorId)) return;
    const ok = await confirm({
      title: 'Delete post?',
      message: 'This permanently removes the post.',
      confirmLabel: 'Delete',
    });
    if (!ok) return;
    try {
      await api(`/api/v1/posts/${post.id}`, { method: 'DELETE' }, token);
      success('Post deleted.');
      onDeleted?.(post.id);
      if (!onDeleted) router.push('/feed');
    } catch (e) {
      error(friendlyError(e, 'Could not delete post.'));
    }
  };

  const openAttachment = async (mediaId: string) => {
    window.open(mediaContentUrl(mediaId), '_blank', 'noopener,noreferrer');
  };

  const communityLabel = post.communityName || post.communitySlug || 'Community';
  const authorLabel = post.authorUsername || 'user';
  usePrefetchAuthors([post.authorUsername]);

  return (
    <article
      className={`post-row ${expandedBody ? 'post-row-static' : 'post-row-clickable'}`}
      onClick={expandedBody ? undefined : openPost}
      role={expandedBody ? undefined : 'link'}
      tabIndex={expandedBody ? undefined : 0}
      onKeyDown={expandedBody ? undefined : (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          openPost();
        }
      }}
    >
      <div className="post-row-body" style={{ minWidth: 0, flex: 1 }}>
        <div className="post-meta post-meta-with-avatar">
          <AuthorAvatar username={post.authorUsername} size={28} className="post-meta-avatar" />
          <span className="post-meta-text">
            {post.communitySlug ? (
              <Link href={`/c/${post.communitySlug}`} onClick={stop}>{communityLabel}</Link>
            ) : (
              <span>{communityLabel}</span>
            )}
            {' · '}
            {post.authorUsername ? (
              <AuthorName username={post.authorUsername} onClick={stop} />
            ) : (
              <span>{authorLabel}</span>
            )}
            {' · '}
            {new Date(post.createdAt).toLocaleString()}
          </span>
        </div>
        {!expandedBody && (
          <h2 className="post-title">
            <Link href={`/post/${post.id}`} onClick={stop}>{post.title}</Link>
          </h2>
        )}
        {post.linkUrls && post.linkUrls.length > 0 && (
          <div className="post-link-list" onClick={stop}>
            {post.linkUrls.slice(0, 2).map((u) => (
              <a key={u} href={u} target="_blank" rel="noreferrer" className="post-link-chip" title={u}>
                <span className="post-link-chip-label">{linkLabel(u)}</span>
                <span className="post-link-chip-host muted">{linkHost(u)}</span>
              </a>
            ))}
            {post.linkUrls.length > 2 && <span className="muted">+{post.linkUrls.length - 2} more</span>}
          </div>
        )}
        {!expandedBody && post.content && (
          <p className="post-excerpt">
            {renderLinkedText(excerptContent(post.content, 220))}
          </p>
        )}
        {imageSrc && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={imageSrc}
            alt=""
            loading="lazy"
            className="post-thumb"
          />
        )}
        {post.media && post.media.filter((m) => (m.mediaKind || '').toUpperCase() !== 'IMAGE').length > 0 && (
          <p className="post-meta" style={{ marginTop: '0.55rem', display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
            {post.media.filter((m) => (m.mediaKind || '').toUpperCase() !== 'IMAGE').map((m) => (
              <button key={m.id} className="btn ghost" type="button" onClick={(e) => { stop(e); openAttachment(m.id); }}>
                {m.originalName || 'Attachment'}
              </button>
            ))}
          </p>
        )}
        <div className="post-meta post-actions" onClick={stop}>
          <div className="vote-inline" aria-label="Vote">
            <button
              className={`vote-btn up ${myVote === 1 ? 'active' : ''}`}
              onClick={() => vote(1)}
              type="button"
              aria-label="Upvote"
              disabled={busy}
            >
              ▲
            </button>
            <span className={`score ${myVote !== 0 ? 'score-voted' : ''}`}>{score}</span>
            <button
              className={`vote-btn down ${myVote === -1 ? 'active' : ''}`}
              onClick={() => vote(-1)}
              type="button"
              aria-label="Downvote"
              disabled={busy}
            >
              ▼
            </button>
          </div>
          <Link href={`/post/${post.id}`} className="post-action-comments">
            <MessageCircleIcon size={15} />
            <span>{post.commentCount}</span>
          </Link>
          <ViewIconLink href={`/post/${post.id}`} label="View post" onClick={stop} />
          {canDeleteContent(user, post.authorId) && (
            <DeleteIconBtn label="Delete post" onClick={remove} />
          )}
        </div>
      </div>
    </article>
  );
}

function nextValue(current: number, clicked: number) {
  return current === clicked ? 0 : clicked;
}

function excerptContent(text: string, max = 220) {
  const t = text.replace(/\s+/g, ' ').trim();
  if (t.length <= max) return t;
  // Prefer cutting after a complete markdown link when possible
  const slice = t.slice(0, max);
  const open = slice.lastIndexOf('[');
  const close = slice.lastIndexOf(')');
  if (open > close && open > max - 40) {
    return `${t.slice(0, open).trimEnd()}…`;
  }
  return `${slice.trimEnd()}…`;
}

function linkHost(url: string) {
  try {
    return new URL(url).host;
  } catch {
    return url;
  }
}

function linkLabel(url: string) {
  try {
    const u = new URL(url);
    const path = u.pathname === '/' ? '' : u.pathname;
    return path ? `${u.host}${path.length > 28 ? `${path.slice(0, 28)}…` : path}` : u.host;
  } catch {
    return url.length > 48 ? `${url.slice(0, 48)}…` : url;
  }
}
