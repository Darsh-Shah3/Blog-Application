'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { Suspense, useEffect, useRef, useState } from 'react';
import { api } from '@/lib/api';
import type { AuthResponse } from '@/lib/api';

const TOKEN_KEY = 'threadly_token';

function safeReturnPath(state: string | null): string {
  if (!state) return '/';
  // state = provider:nonce:returnTo (returnTo may contain ':')
  const idx = state.indexOf(':');
  if (idx < 0) return '/';
  const rest = state.slice(idx + 1);
  const idx2 = rest.indexOf(':');
  const returnTo = idx2 < 0 ? '' : rest.slice(idx2 + 1);
  if (!returnTo) return '/';
  try {
    const u = new URL(returnTo, typeof window !== 'undefined' ? window.location.origin : 'http://localhost:3000');
    if (typeof window !== 'undefined' && u.origin !== window.location.origin) return '/';
    return u.pathname + u.search || '/';
  } catch {
    if (returnTo.startsWith('/') && !returnTo.startsWith('//')) return returnTo;
    return '/';
  }
}

function OAuthCallbackInner() {
  const params = useParams<{ provider: string }>();
  const provider = (params?.provider || 'google').toString().toLowerCase();
  const [status, setStatus] = useState<'working' | 'error'>('working');
  const [message, setMessage] = useState('Finishing sign-in with Google…');
  const started = useRef(false);

  useEffect(() => {
    // Strict Mode runs mount→cleanup→mount; ref blocks a second code exchange (code is one-time).
    if (started.current) return;
    started.current = true;

    const run = async () => {
      const qs = typeof window !== 'undefined' ? new URLSearchParams(window.location.search) : null;
      const error = qs?.get('error');
      const code = qs?.get('code');
      const state = qs?.get('state');

      if (error) {
        setStatus('error');
        setMessage(qs?.get('error_description') || `Google denied access (${error}).`);
        return;
      }
      if (!code) {
        setStatus('error');
        setMessage('Missing authorization code. Start again from Log in.');
        return;
      }

      try {
        const res = await api<AuthResponse>(`/api/v1/auth/oauth/${encodeURIComponent(provider)}/callback`, {
          method: 'POST',
          body: JSON.stringify({ code, state: state || '' }),
        });
        // Persist even if React cleaned up the effect (Strict Mode cancel race).
        if (res?.accessToken) {
          localStorage.setItem(TOKEN_KEY, res.accessToken);
        }
        // Full navigation so AuthProvider reloads session cleanly.
        window.location.replace(safeReturnPath(state ?? null));
      } catch (e) {
        setStatus('error');
        setMessage(e instanceof Error ? e.message : 'Google sign-in failed.');
      }
    };
    void run();
  }, [provider]);

  return (
    <div className="auth-page">
      <div className="auth-card panel">
        <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>
          {status === 'working' ? 'Finishing sign-in' : 'Sign-in failed'}
        </h1>
        <p className="muted">{message}</p>
        {status === 'error' && (
          <div className="toolbar">
            <Link className="btn" href="/login">Back to log in</Link>
            <Link className="btn secondary" href="/register">Sign up</Link>
          </div>
        )}
      </div>
    </div>
  );
}

/** OAuth return surface — exchanges Google code for a Threadly session. */
export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={(
      <div className="auth-page">
        <div className="auth-card panel">
          <p className="muted">Finishing sign-in…</p>
        </div>
      </div>
    )}
    >
      <OAuthCallbackInner />
    </Suspense>
  );
}
