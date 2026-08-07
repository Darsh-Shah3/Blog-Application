'use client';

import Link from 'next/link';
import { FormEvent, useState } from 'react';
import { api, friendlyError } from '@/lib/api';
import { useToast } from '@/lib/toast';

export default function ForgotPasswordPage() {
  const { success, error } = useToast();
  const [email, setEmail] = useState('');
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await api('/api/v1/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email }),
      }, null);
      success('If that email is registered, a reset link was sent.');
    } catch (err) {
      error(friendlyError(err, 'Could not send reset email.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="panel" style={{ maxWidth: 420, margin: '2rem auto' }}>
      <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Forgot password</h1>
      <p className="muted">Enter your account email. We will send a reset link if it exists.</p>
      <form className="form" onSubmit={onSubmit}>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <button className="btn" type="submit" disabled={busy}>
          {busy ? 'Sending…' : 'Send reset link'}
        </button>
      </form>
      <p className="muted" style={{ marginBottom: 0 }}>
        <Link href="/login">Back to login</Link>
      </p>
    </div>
  );
}
