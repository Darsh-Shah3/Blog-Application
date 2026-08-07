'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { api, CommentNode, friendlyError, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { canDeleteContent } from '@/lib/rbac';
import { PostCard } from '@/components/PostCard';
import Link from 'next/link';

function CommentTree({
  nodes,
  onReply,
  onDelete,
  canDelete,
}: {
  nodes: CommentNode[];
  onReply: (parentId: number) => void;
  onDelete: (id: number) => void;
  canDelete: (authorId: number) => boolean;
}) {
  return (
    <>
      {nodes.map((c) => (
        <div key={c.id} className="comment">
          <div className="comment-head">
            <Link href={`/u/${c.authorUsername || c.authorId}`}>u/{c.authorUsername || c.authorId}</Link>
            {' · '}{c.score} points · {new Date(c.createdAt).toLocaleString()}
          </div>
          <div style={{ whiteSpace: 'pre-wrap' }}>{c.content}</div>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <button className="btn ghost" type="button" onClick={() => onReply(c.id)}>Reply</button>
            {canDelete(c.authorId) && (
              <button className="btn ghost" type="button" onClick={() => onDelete(c.id)}>Delete</button>
            )}
          </div>
          {c.replies?.length > 0 && (
            <CommentTree nodes={c.replies} onReply={onReply} onDelete={onDelete} canDelete={canDelete} />
          )}
        </div>
      ))}
    </>
  );
}

export default function PostDetailPage() {
  const params = useParams<{ id: string }>();
  const { token, user } = useAuth();
  const { success, error, confirm } = useToast();
  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<CommentNode[]>([]);
  const [content, setContent] = useState('');
  const [parentId, setParentId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const p = await api<Post>(`/api/v1/posts/${params.id}`, {}, token);
      setPost(p);
      const tree = await api<CommentNode[]>(`/api/v1/comments/threads/${params.id}`, {}, token);
      setComments(tree || []);
    } catch (e) {
      error(friendlyError(e, 'Could not load post.'));
      setPost(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (params.id) load();
  }, [params.id, token]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!token || !post) {
      error('Please sign in to comment.');
      return;
    }
    try {
      await api('/api/v1/comments', {
        method: 'POST',
        body: JSON.stringify({ postId: post.id, parentId, content }),
      }, token);
      setContent('');
      setParentId(null);
      success('Comment posted.');
      load();
    } catch (err) {
      error(friendlyError(err, 'Could not post comment.'));
    }
  };

  const deleteComment = async (id: number) => {
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
      load();
    } catch (e) {
      error(friendlyError(e, 'Could not delete comment.'));
    }
  };

  if (loading && !post) {
    return <p className="muted" style={{ marginTop: '1.5rem' }}>Loading post…</p>;
  }
  if (!post) {
    return (
      <div className="panel" style={{ marginTop: '1.5rem' }}>
        <p>Post not found.</p>
        <Link href="/" className="btn secondary">Home</Link>
      </div>
    );
  }

  return (
    <div style={{ marginTop: '1.25rem' }} className="stack">
      <PostCard post={post} onScoreChange={setPost} />
      <section className="panel">
        <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>
          Comments {parentId ? <span className="muted">(replying to #{parentId})</span> : null}
        </h2>
        {user ? (
          <form className="form" onSubmit={submit}>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Add to the discussion"
              required
            />
            <div className="toolbar">
              <button className="btn" type="submit">Comment</button>
              {parentId && (
                <button className="btn secondary" type="button" onClick={() => setParentId(null)}>
                  Cancel reply
                </button>
              )}
            </div>
          </form>
        ) : (
          <p className="muted">Log in to comment.</p>
        )}
        <CommentTree
          nodes={comments}
          onReply={setParentId}
          onDelete={deleteComment}
          canDelete={(authorId) => canDeleteContent(user, authorId)}
        />
      </section>
    </div>
  );
}
