'use client';

import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { api, friendlyError, getApiUrl, mediaContentUrl, Post, MediaRef } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { hasInlineLinks, normalizeHref, renderLinkedText } from '@/lib/richText';
import { SpinnerLabel } from '@/components/loaders';
import { LinkIcon, PaperclipIcon } from '@/components/icons';
import { AuthorAvatar, VerifiedTick } from '@/components/AuthorIdentity';
import { isSiteAdmin } from '@/lib/authorDirectory';
import { SelectionLinkPanel } from '@/components/SelectionLinkPanel';

type MediaMeta = {
  id: string;
  originalName: string;
  contentType: string;
  kind: string;
  sizeBytes?: number;
  url: string;
};

function toMeta(m: MediaRef): MediaMeta {
  return {
    id: m.id,
    originalName: m.originalName || 'file',
    contentType: m.contentType || 'application/octet-stream',
    kind: (m.mediaKind || 'OTHER').toUpperCase(),
    url: m.url || mediaContentUrl(m.id),
  };
}

export function EditPostModal({
  open,
  post,
  onClose,
  onSaved,
}: {
  open: boolean;
  post: Post | null;
  onClose: () => void;
  onSaved: (p: Post) => void;
}) {
  const { token, user } = useAuth();
  const { success, error } = useToast();
  const bodyRef = useRef<HTMLTextAreaElement>(null);
  const [mounted, setMounted] = useState(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [links, setLinks] = useState<string[]>([]);
  const [linkDraft, setLinkDraft] = useState('');
  const [linkToolsOpen, setLinkToolsOpen] = useState(false);
  const [mediaList, setMediaList] = useState<MediaMeta[]>([]);
  const [uploading, setUploading] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!open || !post) return;
    setTitle(post.title || '');
    setContent(post.content || '');
    const existing = (post.linkUrls || []).filter(Boolean);
    setLinks(existing);
    setLinkToolsOpen(existing.length > 0 || post.postType === 'LINK' || hasInlineLinks(post.content || ''));
    setLinkDraft('');
    setMediaList((post.media || []).map(toMeta));
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setLinkToolsOpen(true);
      }
    };
    window.addEventListener('keydown', onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = prev;
    };
  }, [open, post, onClose]);

  if (!open || !post || !mounted) return null;

  const stopKeys = (e: KeyboardEvent) => {
    e.stopPropagation();
  };

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
      success(meta.kind === 'IMAGE' ? 'Image attached.' : 'File attached.');
    } catch (e) {
      error(friendlyError(e, 'Upload failed.'));
    } finally {
      setUploading(false);
    }
  };

  const removeMedia = async (id: string) => {
    setMediaList((prev) => prev.filter((m) => m.id !== id));
    if (!token) return;
    try {
      await api(`/api/v1/media/${id}`, { method: 'DELETE' }, token);
    } catch {
      // Keep removed from the post even if orphan file cleanup fails.
    }
  };

  const addLink = () => {
    const url = normalizeHref(linkDraft);
    if (!url) return;
    setLinks((prev) => (prev.includes(url) ? prev : [...prev, url]));
    setLinkDraft('');
    setLinkToolsOpen(true);
  };

  const removeLink = (url: string) => {
    setLinks((prev) => prev.filter((u) => u !== url));
  };

  const resolvePostType = (): Post['postType'] => {
    const images = mediaList.some((m) => m.kind === 'IMAGE');
    const files = mediaList.some((m) => m.kind !== 'IMAGE');
    if (images) return 'IMAGE';
    if (files) return 'FILE';
    if (links.length > 0) return 'LINK';
    return 'TEXT';
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!token || busy || uploading) return;
    const nextType = resolvePostType();
    if (nextType === 'LINK' && links.length === 0) {
      error('Add at least one link, or clear the link section.');
      return;
    }
    if ((nextType === 'IMAGE' || nextType === 'FILE') && mediaList.length === 0) {
      error('Attach at least one image or file, or remove media.');
      return;
    }
    setBusy(true);
    try {
      const updated = await api<Post>(`/api/v1/posts/${post.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          title: title.trim(),
          content,
          postType: nextType,
          linkUrls: links,
          media: mediaList.map((m) => ({
            id: m.id,
            originalName: m.originalName,
            contentType: m.contentType,
            mediaKind: m.kind,
            url: m.url,
          })),
        }),
      }, token);
      success('Post updated.');
      onSaved(updated);
      onClose();
    } catch (err) {
      error(friendlyError(err, 'Could not update post.'));
    } finally {
      setBusy(false);
    }
  };

  const admin = user ? isSiteAdmin(user.roles, user.username) : false;
  const imagePreviews = mediaList.filter((m) => m.kind === 'IMAGE');
  const otherMedia = mediaList.filter((m) => m.kind !== 'IMAGE');
  const showPreview = hasInlineLinks(content);

  const dialog = (
    <div
      className="post-modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="Edit post"
      onClick={onClose}
      onKeyDown={stopKeys}
    >
      <div className="post-modal-card composer-card edit-post-card" onClick={(e) => e.stopPropagation()} onKeyDown={stopKeys}>
        <header className="composer-header">
          <div className="composer-author">
            {user && (
              <AuthorAvatar
                username={user.username}
                avatarMediaId={user.avatarMediaId}
                roles={user.roles}
                size={44}
                className="composer-avatar-img"
              />
            )}
            <div>
              <div className="composer-name author-name">
                <span>{user?.displayName || user?.username || 'You'}</span>
                {admin && <VerifiedTick size={14} />}
              </div>
              <p className="muted" style={{ margin: '0.15rem 0 0', fontSize: '0.88rem' }}>
                Edit your post · Link tool (Ctrl+K)
              </p>
            </div>
          </div>
          <div className="composer-header-aside">
            <span className="muted composer-esc-hint">Esc to close</span>
            <button type="button" className="composer-close" aria-label="Close" onClick={onClose}>
              ×
            </button>
          </div>
        </header>

        <form onSubmit={submit} className="composer-form" onKeyDown={stopKeys}>
          <div className="composer-body edit-post-body">
            <label className="composer-field-label" htmlFor="edit-post-title">
              Title
            </label>
            <input
              id="edit-post-title"
              className="composer-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onKeyDown={stopKeys}
              required
              maxLength={300}
              placeholder="Add a title"
              autoFocus
            />

            <label className="composer-field-label" htmlFor="edit-post-body">
              Body
            </label>
            <textarea
              id="edit-post-body"
              ref={bodyRef}
              className="composer-text edit-post-textarea"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              onKeyDown={stopKeys}
              placeholder="What do you want to talk about? Toggle Link, select text, apply URL, keep typing."
              rows={10}
            />

            {linkToolsOpen && (
              <>
                <SelectionLinkPanel
                  open={linkToolsOpen}
                  bodyRef={bodyRef}
                  content={content}
                  onChange={setContent}
                  onApplied={(label) => success(`“${label}” linked — keep writing in the body.`)}
                  onError={error}
                />
                <div className="composer-links-panel">
                  <label className="composer-field-label">Optional attached URLs</label>
                  <div className="composer-inline-link-row">
                    <input
                      className="composer-link"
                      style={{ borderTop: 'none', paddingTop: 0 }}
                      value={linkDraft}
                      onChange={(e) => setLinkDraft(e.target.value)}
                      onKeyDown={(e) => {
                        stopKeys(e);
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          addLink();
                        }
                      }}
                      placeholder="https:// — paste a full URL to attach"
                    />
                    <button type="button" className="btn secondary" onClick={addLink} disabled={!linkDraft.trim()}>
                      Add
                    </button>
                  </div>
                  {links.length > 0 && (
                    <ul className="composer-media-list">
                      {links.map((u) => (
                        <li key={u}>
                          <a href={u} target="_blank" rel="noreferrer" title={u}>{u}</a>
                          <button type="button" className="btn ghost" onClick={() => removeLink(u)}>
                            Remove
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </>
            )}

            {showPreview && (
              <div className="linked-text-preview">
                <span className="composer-field-label">How links look on the post</span>
                <div className="linked-text-preview-body">{renderLinkedText(content)}</div>
              </div>
            )}

            {imagePreviews.length > 0 && (
              <div className="edit-post-gallery">
                {imagePreviews.map((m) => (
                  <figure key={m.id} className="edit-post-thumb">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={m.url || mediaContentUrl(m.id)} alt={m.originalName} />
                    <button type="button" className="btn ghost" onClick={() => removeMedia(m.id)}>
                      Remove image
                    </button>
                  </figure>
                ))}
              </div>
            )}

            {otherMedia.length > 0 && (
              <ul className="composer-media-list">
                {otherMedia.map((m) => (
                  <li key={m.id}>
                    <span>{m.originalName}</span>
                    <span className="muted">{m.kind}</span>
                    <button type="button" className="btn ghost" onClick={() => removeMedia(m.id)}>
                      Remove
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <footer className="composer-footer">
            <div className="composer-tools">
              <label className="composer-tool" title="Add photo or file">
                <PaperclipIcon size={20} />
                <input
                  type="file"
                  accept="image/*,.pdf,.zip,.doc,.docx,.txt"
                  hidden
                  onChange={(e) => {
                    onUpload(e.target.files?.[0] ?? null);
                    e.target.value = '';
                  }}
                  disabled={uploading}
                />
              </label>
              <button
                type="button"
                className={`composer-tool ${linkToolsOpen ? 'active' : ''}`}
                title="Add links (toggle) — select text in body, or attach a URL"
                aria-pressed={linkToolsOpen}
                onClick={() => setLinkToolsOpen((v) => !v)}
              >
                <LinkIcon size={20} />
              </button>
            </div>
            <div className="composer-actions">
              <button type="button" className="btn ghost" onClick={onClose}>
                Cancel
              </button>
              {uploading && <SpinnerLabel label="Uploading…" />}
              <button className="btn" type="submit" disabled={busy || uploading || !title.trim()}>
                {busy ? <SpinnerLabel label="Saving…" /> : 'Save changes'}
              </button>
            </div>
          </footer>
        </form>
      </div>
    </div>
  );

  return createPortal(dialog, document.body);
}
