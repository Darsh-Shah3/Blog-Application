'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { useRouter } from 'next/navigation';
import { api, Community, friendlyError, Page, getApiUrl } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { SpinnerLabel } from '@/components/loaders';
import { ClockIcon, LinkIcon, PaperclipIcon, PenIcon } from '@/components/icons';
import { AuthorAvatar, VerifiedTick } from '@/components/AuthorIdentity';
import { isSiteAdmin } from '@/lib/authorDirectory';

type PostType = 'TEXT' | 'LINK' | 'IMAGE' | 'FILE';

type MediaMeta = {
  id: string;
  originalName: string;
  contentType: string;
  kind: string;
  sizeBytes: number;
  url: string;
};

function localDatetimeValue(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function CreatePostModal({
  open,
  onClose,
  defaultCommunityId,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  defaultCommunityId?: string;
  onCreated?: (postId: string) => void;
}) {
  const { token, user } = useAuth();
  const router = useRouter();
  const { success, error } = useToast();
  const [mounted, setMounted] = useState(false);
  const [communities, setCommunities] = useState<Community[]>([]);
  const [communityId, setCommunityId] = useState(defaultCommunityId || '');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [linkUrl, setLinkUrl] = useState('');
  const [postType, setPostType] = useState<PostType>('TEXT');
  const [mediaList, setMediaList] = useState<MediaMeta[]>([]);
  const [notificationSummary, setNotificationSummary] = useState('');
  const [uploading, setUploading] = useState(false);
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [scheduleLocal, setScheduleLocal] = useState('');
  const [publishing, setPublishing] = useState(false);

  const minSchedule = useMemo(() => {
    const d = new Date(Date.now() + 2 * 60 * 1000);
    return localDatetimeValue(d);
  }, []);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!open) return;
    if (defaultCommunityId) setCommunityId(defaultCommunityId);
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose, defaultCommunityId]);

  useEffect(() => {
    if (!open || !token) return;
    api<Page<Community>>('/api/v1/communities?size=100', {}, token)
      .then((page) => setCommunities(page.content || []))
      .catch((e) => error(friendlyError(e, 'Could not load communities.')));
  }, [open, token, error]);

  if (!open || !mounted) return null;

  const onUpload = async (file: File | null) => {
    if (!file || !token) return;
    setUploading(true);
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
      const meta = (await res.json()) as MediaMeta;
      setMediaList((prev) => [...prev, meta]);
      if (meta.kind === 'IMAGE') setPostType('IMAGE');
      else if (postType === 'TEXT') setPostType('FILE');
      success('File attached.');
    } catch (e) {
      error(friendlyError(e, 'Upload failed. Check file type and size.'));
    } finally {
      setUploading(false);
    }
  };

  const canPublish = Boolean(
    communityId
    && title.trim()
    && !uploading
    && !publishing
    && (postType !== 'LINK' || linkUrl.trim())
    && (postType === 'TEXT' || postType === 'LINK' || mediaList.length > 0)
  );

  const reset = () => {
    setTitle('');
    setContent('');
    setLinkUrl('');
    setPostType('TEXT');
    setMediaList([]);
    setNotificationSummary('');
    setScheduleOpen(false);
    setScheduleLocal('');
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) {
      error('Please sign in to publish.');
      return;
    }
    if ((postType === 'IMAGE' || postType === 'FILE') && mediaList.length === 0) {
      error('Upload at least one file for IMAGE/FILE posts.');
      return;
    }
    if (postType === 'LINK' && !linkUrl.trim()) {
      error('Enter a link URL for LINK posts.');
      return;
    }
    let scheduledAt: string | null = null;
    if (scheduleOpen && scheduleLocal) {
      const at = new Date(scheduleLocal);
      if (Number.isNaN(at.getTime()) || at.getTime() < Date.now() + 30_000) {
        error('Pick a schedule time at least a minute in the future.');
        return;
      }
      scheduledAt = at.toISOString();
    }
    setPublishing(true);
    try {
      const links = postType === 'LINK'
        ? linkUrl.split(/[\n,]+/).map((s) => s.trim()).filter(Boolean)
        : [];
      const post = await api<{ id: string; status?: string }>('/api/v1/posts', {
        method: 'POST',
        body: JSON.stringify({
          communityId,
          title,
          content: content || null,
          postType: mediaList.length && postType === 'TEXT' ? 'FILE' : postType,
          linkUrls: links,
          media: mediaList.map((m) => ({
            id: m.id,
            originalName: m.originalName,
            contentType: m.contentType,
            mediaKind: m.kind,
            url: m.url,
          })),
          notificationSummary: notificationSummary.trim() || null,
          scheduledAt,
        }),
      }, token);
      success(post.status === 'SCHEDULED' || scheduledAt ? 'Post scheduled.' : 'Post published.');
      reset();
      onClose();
      if (onCreated) onCreated(post.id);
      else if (!(post.status === 'SCHEDULED' || scheduledAt)) router.push(`/post/${post.id}`);
      else router.refresh();
    } catch (err) {
      error(friendlyError(err, 'Could not publish post.'));
    } finally {
      setPublishing(false);
    }
  };

  const gate = (
    <div className="post-modal-overlay" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="post-modal-card panel" onClick={(e) => e.stopPropagation()}>
        <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Sign in to post</h2>
        <p className="muted">Create posts after you log in.</p>
        <div className="toolbar">
          <a className="btn" href="/login">Log in</a>
          <button type="button" className="btn ghost" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );

  if (!user) {
    return createPortal(gate, document.body);
  }

  const admin = isSiteAdmin(user.roles, user.username);

  const dialog = (
    <div
      className="post-modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="Create post"
      onClick={onClose}
    >
      <div className="post-modal-card composer-card" onClick={(e) => e.stopPropagation()}>
        <header className="composer-header">
          <div className="composer-author">
            <AuthorAvatar
              username={user.username}
              avatarMediaId={user.avatarMediaId}
              roles={user.roles}
              size={40}
              className="composer-avatar-img"
            />
            <div>
              <div className="composer-name author-name">
                <span>{user.displayName || user.username}</span>
                {admin && <VerifiedTick size={14} />}
              </div>
              <label className="composer-audience">
                <span className="muted">Post to</span>
                <select
                  value={communityId}
                  onChange={(e) => setCommunityId(e.target.value)}
                  required
                >
                  <option value="">Select community</option>
                  {communities.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </label>
            </div>
          </div>
          <div className="composer-header-aside">
            <span className="muted composer-esc-hint">Esc to close</span>
            <button type="button" className="composer-close" aria-label="Close" onClick={onClose}>
              ×
            </button>
          </div>
        </header>

        <form onSubmit={onSubmit} className="composer-form">
          <div className="composer-body">
            <input
              className="composer-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              maxLength={300}
              placeholder="Add a title"
              autoFocus
            />
            <textarea
              className="composer-text"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="What do you want to talk about?"
              rows={6}
            />
            {postType === 'LINK' && (
              <input
                className="composer-link"
                value={linkUrl}
                onChange={(e) => setLinkUrl(e.target.value)}
                placeholder="https:// - paste link(s)"
                required
              />
            )}
            <input
              className="composer-summary"
              value={notificationSummary}
              onChange={(e) => setNotificationSummary(e.target.value)}
              maxLength={500}
              placeholder="Optional blurb for followers alerts"
            />
            {mediaList.length > 0 && (
              <ul className="composer-media-list">
                {mediaList.map((m) => (
                  <li key={m.id}>
                    <span>{m.originalName}</span>
                    <span className="muted">{m.kind}</span>
                    <button
                      type="button"
                      className="btn ghost"
                      onClick={() => setMediaList((prev) => prev.filter((x) => x.id !== m.id))}
                    >
                      Remove
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {scheduleOpen && (
              <div className="composer-schedule">
                <div className="composer-schedule-head">
                  <ClockIcon size={18} />
                  <strong>Schedule post</strong>
                  <button
                    type="button"
                    className="btn ghost"
                    onClick={() => {
                      setScheduleOpen(false);
                      setScheduleLocal('');
                    }}
                  >
                    Clear
                  </button>
                </div>
                <label className="composer-schedule-fields">
                  Date and time
                  <input
                    type="datetime-local"
                    min={minSchedule}
                    value={scheduleLocal}
                    onChange={(e) => setScheduleLocal(e.target.value)}
                  />
                </label>
              </div>
            )}
          </div>

          <footer className="composer-footer">
            <div className="composer-tools">
              <label className="composer-tool" title="Add photo or file">
                <PaperclipIcon size={20} />
                <input
                  type="file"
                  hidden
                  onChange={(e) => onUpload(e.target.files?.[0] ?? null)}
                  disabled={uploading}
                />
              </label>
              <button
                type="button"
                className={`composer-tool ${postType === 'LINK' ? 'active' : ''}`}
                title="Add link"
                onClick={() => setPostType((t) => (t === 'LINK' ? 'TEXT' : 'LINK'))}
              >
                <LinkIcon size={20} />
              </button>
              <button
                type="button"
                className={`composer-tool ${scheduleOpen ? 'active' : ''}`}
                title="Schedule"
                onClick={() => {
                  setScheduleOpen(true);
                  if (!scheduleLocal) setScheduleLocal(minSchedule);
                }}
              >
                <ClockIcon size={20} />
              </button>
              <span className="composer-tool muted" title="Text post">
                <PenIcon size={20} />
              </span>
            </div>
            <div className="composer-actions">
              <button type="button" className="btn ghost" onClick={onClose}>
                Cancel
              </button>
              {uploading && <SpinnerLabel label="Uploading..." />}
              <button className="btn" type="submit" disabled={!canPublish}>
                {publishing
                  ? <SpinnerLabel label="Working..." />
                  : scheduleOpen && scheduleLocal
                    ? 'Schedule'
                    : 'Post'}
              </button>
            </div>
          </footer>
        </form>
      </div>
    </div>
  );

  return createPortal(dialog, document.body);
}
