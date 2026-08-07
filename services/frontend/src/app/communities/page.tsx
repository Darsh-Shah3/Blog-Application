'use client';

import Link from 'next/link';
import { FormEvent, useEffect, useState } from 'react';
import { api, Community, friendlyError, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';

export default function CommunitiesPage() {
  const { token, user } = useAuth();
  const { success, error } = useToast();
  const [communities, setCommunities] = useState<Community[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    api<Page<Community>>('/api/v1/communities?size=50', {}, token)
      .then((page) => setCommunities(page.content || []))
      .catch((e) => error(friendlyError(e, 'Could not load communities.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [token]);

  const create = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) {
      error('Please sign in to create a community.');
      return;
    }
    try {
      await api('/api/v1/communities', {
        method: 'POST',
        body: JSON.stringify({ name, description }),
      }, token);
      setName('');
      setDescription('');
      success('Community created.');
      load();
    } catch (err) {
      error(friendlyError(err, 'Could not create community.'));
    }
  };

  return (
    <div className="grid-2" style={{ marginTop: '1.5rem' }}>
      <section className="stack">
        <h1 style={{ fontFamily: 'var(--font-display)' }}>Communities</h1>
        {loading && <p className="muted">Loading…</p>}
        {!loading && communities.length === 0 && (
          <div className="panel muted">No communities yet. Create the first one.</div>
        )}
        {communities.map((c) => (
          <article key={c.id} className="panel">
            <h2 style={{ margin: '0 0 0.35rem', fontFamily: 'var(--font-display)' }}>
              <Link href={`/c/${c.slug}`}>c/{c.slug}</Link>
            </h2>
            <p className="muted" style={{ margin: 0 }}>{c.description || 'No description yet.'}</p>
            <p className="muted" style={{ marginBottom: 0 }}>{c.memberCount} members</p>
          </article>
        ))}
      </section>
      <aside className="panel">
        <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Create community</h2>
        {!user && <p className="muted">Log in to create and join communities.</p>}
        <form className="form" onSubmit={create}>
          <label>
            Name
            <input value={name} onChange={(e) => setName(e.target.value)} required minLength={3} />
          </label>
          <label>
            Description
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} />
          </label>
          <button className="btn" type="submit" disabled={!user}>Create</button>
        </form>
      </aside>
    </div>
  );
}
