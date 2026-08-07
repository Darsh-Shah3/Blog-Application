'use client';

import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { FormEvent, Suspense, useState } from 'react';
import { api, friendlyError } from '@/lib/api';
import { useToast } from '@/lib/toast';

function ResetForm() {
  const search = useSearchParams();
  const router = useRouter();
  const { success, error } = useToast();
  const tokenFromUrl = search.get('token') || '';
  const [token, setToken] = useState(tokenFromUrl);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (password.length < 6) {
      error('Password must be at least 6 characters.');
      return;
    }
    if (password !== confirm) {
      error('Passwords do not match.');
      return;
    }
    if (!token.trim()) {
      error('Reset link is missing or incomplete.');
      return;
    }
    setBusy(true);
    try {
      await api('/api/v1/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({ token: token.trim(), newPassword: password }),
      }, null);
      success('Password updated. You can sign in now.');
      router.push('/login');
    } catch (err) {
      error(friendlyError(err, 'Reset failed. Request a new link.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="panel" style={{ maxWidth: 420, margin: '2rem auto' }}>
      <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Set new password</h1>
      <form className="form" onSubmit={onSubmit}>
        {!tokenFromUrl && (
          <label>
            Reset token
            <input value={token} onChange={(e) => setToken(e.target.value)} required />
          </label>
        )}
        <label>
          New password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={6} required />
        </label>
        <label>
          Confirm password
          <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} minLength={6} required />
        </label>
        <button className="btn" type="submit" disabled={busy}>
          {busy ? 'Saving…' : 'Update password'}
        </button>
      </form>
      <p className="muted" style={{ marginBottom: 0 }}>
        <Link href="/forgot-password">Request a new link</Link>
      </p>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<p className="muted" style={{ marginTop: '1.5rem' }}>Loading…</p>}>
      <ResetForm />
    </Suspense>
  );
}
