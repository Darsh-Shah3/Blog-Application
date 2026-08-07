'use client';

import { FormEvent, useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { api, Community, friendlyError, Page, getApiUrl } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';

type PostType = 'TEXT' | 'LINK' | 'IMAGE' | 'FILE';

type MediaMeta = {
  id: number;
  originalName: string;
  contentType: string;
  kind: string;
  sizeBytes: number;
  url: string;
};

function SubmitForm() {
  const { token, user } = useAuth();
  const router = useRouter();
  const search = useSearchParams();
  const { success, error } = useToast();
  const [communities, setCommunities] = useState<Community[]>([]);
  const [communityId, setCommunityId] = useState('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [linkUrl, setLinkUrl] = useState('');
  const [postType, setPostType] = useState<PostType>('TEXT');
  const [media, setMedia] = useState<MediaMeta | null>(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    if (!token) return;
    api<Page<Community>>('/api/v1/communities?size=100', {}, token)
      .then((page) => {
        setCommunities(page.content || []);
        const preset = search.get('communityId');
        if (preset) setCommunityId(preset);
      })
      .catch((e) => error(friendlyError(e, 'Could not load communities.')));
  }, [token, search]);

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
      setMedia(meta);
      if (meta.kind === 'IMAGE') setPostType('IMAGE');
      else if (postType === 'TEXT') setPostType('FILE');
      success('File uploaded.');
    } catch (e) {
      error(friendlyError(e, 'Upload failed. Check file type and size.'));
      setMedia(null);
    } finally {
      setUploading(false);
    }
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) {
      error('Please sign in to publish.');
      return;
    }
    if ((postType === 'IMAGE' || postType === 'FILE') && !media) {
      error('Upload a file first for IMAGE/FILE posts.');
      return;
    }
    if (postType === 'LINK' && !linkUrl.trim()) {
      error('Enter a link URL for LINK posts.');
      return;
    }
    try {
      const post = await api<{ id: number }>('/api/v1/posts', {
        method: 'POST',
        body: JSON.stringify({
          communityId: Number(communityId),
          title,
          content: content || null,
          postType,
          linkUrl: postType === 'LINK' ? linkUrl : null,
          mediaId: media?.id ?? null,
        }),
      }, token);
      success('Post published.');
      router.push(`/post/${post.id}`);
    } catch (err) {
      error(friendlyError(err, 'Could not publish post.'));
    }
  };

  if (!user) {
    return <div className="panel" style={{ marginTop: '1.5rem' }}>Log in to create a post.</div>;
  }

  return (
    <div className="panel" style={{ maxWidth: 640, margin: '1.5rem auto' }}>
      <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Create post</h1>
      <p className="muted" style={{ marginTop: 0 }}>
        Text is stored in post DB; files go to media-service (local volume).
        Caps: images 5&nbsp;MB · video 50&nbsp;MB · documents/zip 20&nbsp;MB.
      </p>
      <form className="form" onSubmit={onSubmit}>
        <label>
          Community
          <select value={communityId} onChange={(e) => setCommunityId(e.target.value)} required>
            <option value="">Select community</option>
            {communities.map((c) => (
              <option key={c.id} value={c.id}>c/{c.slug}</option>
            ))}
          </select>
        </label>
        <label>
          Type
          <select value={postType} onChange={(e) => setPostType(e.target.value as PostType)}>
            <option value="TEXT">Text</option>
            <option value="LINK">Link</option>
            <option value="IMAGE">Image</option>
            <option value="FILE">File / video / zip</option>
          </select>
        </label>
        <label>
          Title
          <input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={300} />
        </label>
        {postType === 'LINK' && (
          <label>
            Link URL
            <input
              type="url"
              value={linkUrl}
              onChange={(e) => setLinkUrl(e.target.value)}
              placeholder="https://"
              required
            />
          </label>
        )}
        <label>
          Body (optional)
          <textarea value={content} onChange={(e) => setContent(e.target.value)} />
        </label>
        {(postType === 'IMAGE' || postType === 'FILE' || postType === 'TEXT') && (
          <label>
            Attachment (image, video, zip, pdf, …)
            <input
              type="file"
              onChange={(e) => onUpload(e.target.files?.[0] ?? null)}
              disabled={uploading}
            />
          </label>
        )}
        {uploading && <p className="muted">Uploading…</p>}
        {media && (
          <p className="muted">
            Attached: <strong>{media.originalName}</strong> ({media.kind}, {(media.sizeBytes / 1024).toFixed(0)} KB)
          </p>
        )}
        <button className="btn" type="submit" disabled={uploading}>Publish</button>
      </form>
    </div>
  );
}

export default function SubmitPage() {
  return (
    <Suspense fallback={<p className="muted">Loading…</p>}>
      <SubmitForm />
    </Suspense>
  );
}
