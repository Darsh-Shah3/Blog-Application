'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { api, friendlyError, Post, getApiUrl } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { canDeleteContent } from '@/lib/rbac';

export function PostCard({
  post,
  onScoreChange,
  onDeleted,
}: {
  post: Post;
  onScoreChange?: (p: Post) => void;
  onDeleted?: (id: number) => void;
}) {
  const { token, user } = useAuth();
  const router = useRouter();
  const { success, error, info, confirm } = useToast();
  const [score, setScore] = useState(post.score);
  const [myVote, setMyVote] = useState<number>(0);
  const [busy, setBusy] = useState(false);
  const [imageBlobUrl, setImageBlobUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!post.mediaId || post.postType !== 'IMAGE' || !token) {
      setImageBlobUrl(null);
      return;
    }
    let revoked: string | null = null;
    let cancelled = false;
    (async () => {
      try {
        const res = await fetch(`${getApiUrl()}/api/v1/media/${post.mediaId}/content`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok || cancelled) return;
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        revoked = url;
        if (!cancelled) setImageBlobUrl(url);
      } catch {
        // keep UI stable if media fails
      }
    })();
    return () => {
      cancelled = true;
      if (revoked) URL.revokeObjectURL(revoked);
    };
  }, [post.mediaId, post.postType, token]);

  const vote = async (value: number) => {
    if (!token || !user) {
      info('Please sign in to vote.');
      return;
    }
    if (busy) return;
    setBusy(true);
    try {
      const next = myVote === value ? 0 : value;
      const res = await api<{ scoreDeltaApplied: number; value: number }>(
        '/api/v1/votes',
        {
          method: 'POST',
          body: JSON.stringify({ targetType: 'POST', targetId: post.id, value: next }),
        },
        token
      );
      const updated = score + (res.scoreDeltaApplied || 0);
      setScore(updated);
      setMyVote(res.value);
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
      if (!onDeleted) router.push('/');
    } catch (e) {
      error(friendlyError(e, 'Could not delete post.'));
    }
  };

  const openAttachment = async () => {
    if (!post.mediaId || !token) {
      info(token ? 'No attachment on this post.' : 'Please sign in to open files.');
      return;
    }
    try {
      const res = await fetch(`${getApiUrl()}/api/v1/media/${post.mediaId}/content`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error('Could not load file');
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener,noreferrer');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (e) {
      error(friendlyError(e, 'Could not open attachment.'));
    }
  };

  return (
    <article className="post-row">
      <div className="vote-col">
        <button className={`vote-btn up ${myVote === 1 ? 'active' : ''}`} onClick={() => vote(1)} type="button">▲</button>
        <div className="score">{score}</div>
        <button className={`vote-btn down ${myVote === -1 ? 'active' : ''}`} onClick={() => vote(-1)} type="button">▼</button>
      </div>
      <div>
        <div className="post-meta">
          {post.communitySlug ? (
            <Link href={`/c/${post.communitySlug}`}>c/{post.communitySlug}</Link>
          ) : (
            <span>community {post.communityId}</span>
          )}
          {' · '}
          {post.authorUsername ? (
            <Link href={`/u/${post.authorUsername}`}>u/{post.authorUsername}</Link>
          ) : (
            <span>user {post.authorId}</span>
          )}
          {' · '}
          {new Date(post.createdAt).toLocaleString()}
          {' · '}
          {post.postType}
        </div>
        <h2 className="post-title">
          <Link href={`/post/${post.id}`}>{post.title}</Link>
        </h2>
        {post.linkUrl && (
          <p className="post-excerpt">
            <a href={post.linkUrl} target="_blank" rel="noreferrer">{post.linkUrl}</a>
          </p>
        )}
        {post.content && (
          <p className="post-excerpt">
            {post.content.slice(0, 220)}{post.content.length > 220 ? '…' : ''}
          </p>
        )}
        {imageBlobUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={imageBlobUrl}
            alt=""
            style={{ maxWidth: '100%', marginTop: '0.75rem', borderRadius: 4 }}
          />
        )}
        {post.mediaId && post.postType !== 'IMAGE' && (
          <p className="post-meta" style={{ marginTop: '0.55rem' }}>
            <button className="btn ghost" type="button" onClick={openAttachment}>
              Open attachment (media #{post.mediaId})
            </button>
          </p>
        )}
        <div className="post-meta" style={{ marginTop: '0.55rem', display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <span>{post.commentCount} comments</span>
          {canDeleteContent(user, post.authorId) && (
            <button className="btn ghost" type="button" onClick={remove}>Delete</button>
          )}
        </div>
      </div>
    </article>
  );
}
