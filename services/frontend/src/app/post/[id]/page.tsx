'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { api, CommentNode, Community, friendlyError, Page, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { canDeleteContent } from '@/lib/rbac';
import { DeleteIconBtn } from '@/components/ActionIcon';
import { PostCard } from '@/components/PostCard';
import Link from 'next/link';
import { PostDetailSkeleton, SpinnerLabel } from '@/components/loaders';

const COMMENT_PAGE = 25;

function avatarLetter(name?: string | null) {
  const s = (name || '?').trim();
  return s ? s[0]!.toUpperCase() : '?';
}

function relativeTime(iso: string) {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const sec = Math.max(0, Math.floor((Date.now() - then) / 1000));
  if (sec < 60) return 'just now';
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min} minute${min === 1 ? '' : 's'} ago`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr} hour${hr === 1 ? '' : 's'} ago`;
  const day = Math.floor(hr / 24);
  if (day < 30) return `${day} day${day === 1 ? '' : 's'} ago`;
  return new Date(iso).toLocaleDateString();
}

function CommentTree({
  nodes,
  replyToId,
  replyDraft,
  setReplyDraft,
  onStartReply,
  onCancelReply,
  onSubmitReply,
  onDelete,
  canDelete,
  canReply,
  depth = 0,
  parentAuthor,
}: {
  nodes: CommentNode[];
  replyToId: string | null;
  replyDraft: string;
  setReplyDraft: (v: string) => void;
  onStartReply: (id: string) => void;
  onCancelReply: () => void;
  onSubmitReply: (parentId: string) => void;
  onDelete: (id: string) => void;
  canDelete: (authorId: number | string) => boolean;
  canReply: boolean;
  depth?: number;
  parentAuthor?: string;
}) {
  return (
    <div className={depth === 0 ? 'yt-comment-list' : 'yt-comment-replies'}>
      {nodes.map((c) => {
        const name = c.authorUsername || String(c.authorId);
        const isReplying = replyToId === c.id;
        return (
          <div key={c.id} className={`yt-thread ${depth > 0 ? 'yt-thread-nested' : ''}`}>
            <div className="yt-thread-line" aria-hidden="true" />
            <div className={`yt-comment ${depth > 0 ? 'yt-comment-nested' : ''}`}>
              <div className="yt-avatar" aria-hidden="true">{avatarLetter(c.authorUsername)}</div>
              <div className="yt-comment-body">
                <div className="yt-comment-meta">
                  <Link href={`/u/${name}`} className="yt-author">{name}</Link>
                  {parentAuthor && depth > 0 && (
                    <span className="yt-reply-to">
                      replied to <strong>@{parentAuthor}</strong>
                    </span>
                  )}
                  <span className="yt-time">{relativeTime(c.createdAt)}</span>
                </div>
                <p className="yt-comment-text">{c.content}</p>
                <div className="yt-comment-actions">
                  <span className="yt-likes" title="Score">▲ {c.score}</span>
                  {canReply && (
                    <button type="button" className="yt-action-btn" onClick={() => onStartReply(c.id)}>
                      Reply
                    </button>
                  )}
                  {canDelete(c.authorId) && (
                    <DeleteIconBtn
                      className="yt-icon-delete"
                      label="Delete comment"
                      onClick={() => onDelete(c.id)}
                    />
                  )}
                </div>

                {isReplying && (
                  <form
                    className="yt-inline-reply"
                    onSubmit={(e) => {
                      e.preventDefault();
                      onSubmitReply(c.id);
                    }}
                  >
                    <p className="muted yt-inline-reply-label">
                      Replying to <strong>{name}</strong>
                    </p>
                    <textarea
                      value={replyDraft}
                      onChange={(e) => setReplyDraft(e.target.value)}
                      placeholder={`Reply to ${name}…`}
                      required
                      rows={2}
                      autoFocus
                    />
                    <div className="yt-inline-reply-actions">
                      <button type="button" className="btn ghost" onClick={onCancelReply}>Cancel</button>
                      <button type="submit" className="btn">Reply</button>
                    </div>
                  </form>
                )}

                {c.replies?.length > 0 && (
                  <CommentTree
                    nodes={c.replies}
                    replyToId={replyToId}
                    replyDraft={replyDraft}
                    setReplyDraft={setReplyDraft}
                    onStartReply={onStartReply}
                    onCancelReply={onCancelReply}
                    onSubmitReply={onSubmitReply}
                    onDelete={onDelete}
                    canDelete={canDelete}
                    canReply={canReply}
                    depth={depth + 1}
                    parentAuthor={name}
                  />
                )}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default function PostDetailPage() {
  const params = useParams<{ id: string }>();
  const { token, user, loading: authLoading } = useAuth();
  const { success, error, confirm } = useToast();
  const [post, setPost] = useState<Post | null>(null);
  const [related, setRelated] = useState<Post[]>([]);
  const [community, setCommunity] = useState<Community | null>(null);
  const [comments, setComments] = useState<CommentNode[]>([]);
  const [commentPage, setCommentPage] = useState(0);
  const [hasMoreComments, setHasMoreComments] = useState(false);
  const [content, setContent] = useState('');
  const [parentId, setParentId] = useState<string | null>(null);
  const [replyDraft, setReplyDraft] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadingComments, setLoadingComments] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const [joining, setJoining] = useState(false);

  const loadComments = async (pageNum = 0, append = false) => {
    if (append) setLoadingComments(true);
    try {
      const page = await api<Page<CommentNode>>(
        `/api/v1/comments/threads/${params.id}?page=${pageNum}&size=${COMMENT_PAGE}`,
        {},
        token
      );
      const batch = page.content || [];
      setComments((prev) => (append ? [...prev, ...batch] : batch));
      setCommentPage(pageNum);
      setHasMoreComments(!page.last && batch.length >= COMMENT_PAGE);
    } finally {
      setLoadingComments(false);
    }
  };

  const load = async () => {
    setLoading(true);
    try {
      const p = await api<Post>(`/api/v1/posts/${params.id}`, {}, token);
      setPost(p);
      await loadComments(0, false);
      api<Post[]>(`/api/v1/posts/${params.id}/related?limit=6`, {}, token)
        .then((r) => setRelated(r || []))
        .catch(() => setRelated([]));
      if (p.communitySlug) {
        api<Community>(`/api/v1/communities/by-slug/${p.communitySlug}`, {}, token)
          .then(setCommunity)
          .catch(() => setCommunity(null));
      } else {
        setCommunity(null);
      }
    } catch (e) {
      error(friendlyError(e, 'Could not load post.'));
      setPost(null);
      setCommunity(null);
    } finally {
      setLoading(false);
    }
  };

  const joinCommunity = async () => {
    if (!community || !token) {
      error('Please sign in to join.');
      return;
    }
    setJoining(true);
    try {
      await api(`/api/v1/communities/${community.id}/join`, { method: 'POST' }, token);
      success('Joined community.');
      setCommunity((c) => (c ? { ...c, joined: true, memberCount: c.memberCount + 1 } : c));
    } catch (e) {
      error(friendlyError(e, 'Could not join.'));
    } finally {
      setJoining(false);
    }
  };

  useEffect(() => {
    if (!params.id || authLoading) return;
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id, token, authLoading]);

  const submitTopLevel = async (e: FormEvent) => {
    e.preventDefault();
    if (!token || !post) {
      error('Please sign in to comment.');
      return;
    }
    try {
      await api('/api/v1/comments', {
        method: 'POST',
        body: JSON.stringify({ postId: post.id, parentId: null, content }),
      }, token);
      setContent('');
      success('Comment posted.');
      await loadComments(0, false);
      setPost((prev) => prev ? { ...prev, commentCount: prev.commentCount + 1 } : prev);
    } catch (err) {
      error(friendlyError(err, 'Could not post comment.'));
    }
  };

  const submitReply = async (parent: string) => {
    if (!token || !post) {
      error('Please sign in to reply.');
      return;
    }
    if (!replyDraft.trim()) return;
    try {
      await api('/api/v1/comments', {
        method: 'POST',
        body: JSON.stringify({ postId: post.id, parentId: parent, content: replyDraft }),
      }, token);
      setReplyDraft('');
      setParentId(null);
      success('Reply posted.');
      await loadComments(0, false);
      setPost((prev) => prev ? { ...prev, commentCount: prev.commentCount + 1 } : prev);
    } catch (err) {
      error(friendlyError(err, 'Could not post reply.'));
    }
  };

  const deleteComment = async (id: string) => {
    if (!token) return;
    const ok = await confirm({
      title: 'Delete comment?',
      message: 'This cannot be undone.',
      confirmLabel: 'Delete',
    });
    if (!ok) return;
    try {
      await api(`/api/v1/comments/${id}`, { method: 'DELETE' }, token);
      success('Comment deleted.');
      await loadComments(0, false);
    } catch (e) {
      error(friendlyError(e, 'Could not delete comment.'));
    }
  };

  if (authLoading || (loading && !post)) {
    return <PostDetailSkeleton />;
  }

  if (!post) {
    return (
      <div className="panel" style={{ marginTop: '1.5rem' }}>
        <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Post not found</h1>
        <p className="muted">It may have been removed, or the link is incomplete.</p>
        <div className="toolbar">
          <Link href="/feed" className="btn">Back to posts</Link>
          <Link href="/communities" className="btn secondary">Communities</Link>
        </div>
      </div>
    );
  }

  const full = post.content || '';
  const isLong = full.length > 400;
  const shown = expanded || !isLong ? full : `${full.slice(0, 400)}…`;
  const totalComments = post.commentCount ?? comments.length;
  const nextPostQuery = typeof window !== 'undefined'
    ? `?next=${encodeURIComponent(`/post/${post.id}`)}`
    : '';

  const authorName = post.authorUsername || 'member';
  const communityName = community?.name || post.communityName || post.communitySlug || 'Community';
  const communitySlug = community?.slug || post.communitySlug;

  return (
    <div className="page-shell page-shell-wide post-detail stack">
      <Link href="/feed" className="post-detail-back">← Posts</Link>

      {!user && (
        <div className="panel feed-guest-banner guest-post-banner">
          <div>
            <strong>Reading as a guest</strong>
            <p className="muted" style={{ margin: '0.25rem 0 0' }}>
              You can read this post and its comments. Sign in to vote, reply, or join the conversation.
            </p>
          </div>
          <div className="toolbar" style={{ margin: 0 }}>
            <Link className="btn" href={`/login${nextPostQuery}`}>Log in</Link>
            <Link className="btn secondary" href={`/register${nextPostQuery}`}>Sign up</Link>
          </div>
        </div>
      )}

      <div className="feed-layout post-detail-layout">
        <div className="feed-main stack">
          <header className="page-header page-header-post">
            <p className="page-kicker">Discussion</p>
            <div className="page-meta-row" style={{ marginTop: 0, marginBottom: '0.55rem' }}>
              {communitySlug ? (
                <Link href={`/c/${communitySlug}`} className="page-pill teal">
                  {communityName}
                </Link>
              ) : (
                <span className="page-pill teal">Community</span>
              )}
              {post.authorUsername && (
                <Link href={`/u/${post.authorUsername}`} className="page-pill">
                  {post.authorUsername}
                </Link>
              )}
              <span className="muted" style={{ fontSize: '0.88rem' }}>
                {new Date(post.createdAt).toLocaleString()}
              </span>
            </div>
            <h1 style={{ marginBottom: 0 }}>{post.title}</h1>
          </header>

          <PostCard post={post} onScoreChange={setPost} expandedBody />
          {full && (
            <section className="panel post-body">
              <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.6, fontSize: '1.02rem' }}>{shown}</div>
              {isLong && (
                <button type="button" className="btn ghost" style={{ marginTop: '0.75rem' }} onClick={() => setExpanded((v) => !v)}>
                  {expanded ? 'Show less' : 'Read more'}
                </button>
              )}
            </section>
          )}
          <section className="panel yt-comments-panel">
            <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>
              {totalComments} {totalComments === 1 ? 'Comment' : 'Comments'}
            </h2>
            {user ? (
              <form className="yt-composer" onSubmit={submitTopLevel}>
                <div className="yt-avatar yt-avatar-self" aria-hidden="true">
                  {avatarLetter(user.username)}
                </div>
                <div className="yt-composer-fields">
                  <textarea
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    placeholder="Add a comment…"
                    required
                    rows={2}
                  />
                  <div className="yt-composer-actions">
                    <button className="btn" type="submit">Comment</button>
                  </div>
                </div>
              </form>
            ) : (
              <p className="muted">
                <Link href={`/login${nextPostQuery}`}>Log in</Link> to leave a comment.
              </p>
            )}
            {comments.length === 0 ? (
              <p className="muted" style={{ marginTop: '1rem' }}>No comments yet. Be the first to reply.</p>
            ) : (
              <CommentTree
                nodes={comments}
                replyToId={parentId}
                replyDraft={replyDraft}
                setReplyDraft={setReplyDraft}
                onStartReply={(id) => {
                  setParentId(id);
                  setReplyDraft('');
                }}
                onCancelReply={() => {
                  setParentId(null);
                  setReplyDraft('');
                }}
                onSubmitReply={submitReply}
                onDelete={deleteComment}
                canDelete={(authorId) => canDeleteContent(user, authorId)}
                canReply={Boolean(user)}
              />
            )}
            {hasMoreComments && (
              <div style={{ display: 'flex', justifyContent: 'center', marginTop: '1rem' }}>
                <button
                  type="button"
                  className="btn secondary"
                  disabled={loadingComments}
                  onClick={() => loadComments(commentPage + 1, true)}
                >
                  {loadingComments ? <SpinnerLabel label="Loading comments…" /> : 'Load more comments'}
                </button>
              </div>
            )}
          </section>
        </div>

        <aside className="feed-rail post-rail" aria-label="Post context">
          <div className="panel feed-rail-card post-rail-card">
            <div className="post-rail-title">
              <h2>About this space</h2>
            </div>
            {communitySlug ? (
              <>
                <p>
                  <Link href={`/c/${communitySlug}`}><strong>{communityName}</strong></Link>
                  {community?.description ? ` — ${community.description}` : ' — focused discussion on Threadly.'}
                </p>
                <div className="post-rail-stats">
                  <div className="post-rail-stat">
                    <strong>{community?.memberCount ?? '—'}</strong>
                    <span>Members</span>
                  </div>
                  <div className="post-rail-stat">
                    <strong>{totalComments}</strong>
                    <span>Comments here</span>
                  </div>
                </div>
                <div className="post-rail-actions">
                  <Link className="btn secondary" href={`/c/${communitySlug}`}>
                    Open community
                  </Link>
                  {user && community && !community.joined && (
                    <button className="btn" type="button" disabled={joining} onClick={joinCommunity}>
                      {joining ? 'Joining…' : 'Join community'}
                    </button>
                  )}
                  {user && community?.joined && (
                    <p className="muted" style={{ margin: 0, fontSize: '0.85rem' }}>You&apos;re a member</p>
                  )}
                  {!user && (
                    <Link className="btn" href={`/login${nextPostQuery}`}>Sign in to join</Link>
                  )}
                </div>
              </>
            ) : (
              <p>This post isn&apos;t tied to a community listing.</p>
            )}
          </div>

          <div className="panel feed-rail-card post-rail-card">
            <div className="post-rail-title">
              <h2>Author</h2>
            </div>
            <div className="post-rail-author">
              <div className="yt-avatar" aria-hidden="true">{avatarLetter(authorName)}</div>
              <div className="post-rail-author-text">
                <Link href={`/u/${authorName}`}>{authorName}</Link>
                <p className="muted" style={{ margin: '0.15rem 0 0', fontSize: '0.82rem' }}>
                  Posted {relativeTime(post.createdAt)}
                </p>
              </div>
            </div>
            <div className="post-rail-stats">
              <div className="post-rail-stat">
                <strong>{post.score}</strong>
                <span>Score</span>
              </div>
              <div className="post-rail-stat">
                <strong>{post.postType}</strong>
                <span>Type</span>
              </div>
            </div>
          </div>

          <div className="panel feed-rail-card post-rail-card">
            <div className="post-rail-title">
              <h2>Related</h2>
              {communitySlug && (
                <Link href={`/c/${communitySlug}`} className="muted" style={{ fontSize: '0.82rem' }}>
                  See all
                </Link>
              )}
            </div>
            {related.length === 0 ? (
              <p>No other posts in this space yet.</p>
            ) : (
              <ul className="post-rail-related">
                {related.map((p) => (
                  <li key={p.id}>
                    <Link href={`/post/${p.id}`}>
                      <span className="post-rail-related-title">{p.title}</span>
                      <span className="post-rail-related-meta">
                        ▲ {p.score} · {p.commentCount} comments
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="panel feed-rail-card post-rail-card">
            <div className="post-rail-title">
              <h2>Explore</h2>
            </div>
            <p>Find more spaces and follow people who post often.</p>
            <div className="post-rail-actions">
              <Link className="btn secondary" href="/communities">Browse communities</Link>
              <Link className="btn ghost" href="/feed">Back to posts</Link>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
