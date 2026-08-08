'use client';

import Link from 'next/link';
import { FormEvent, useState } from 'react';
import { api, friendlyError } from '@/lib/api';
import { useToast } from '@/lib/toast';
import { SpinnerLabel } from '@/components/loaders';
import { useTheme } from '@/lib/theme';
import { MoonIcon, SunIcon } from '@/components/icons';

export default function ForgotPasswordPage() {
  const { success, error } = useToast();
  const { theme, toggleTheme } = useTheme();
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
    <div className="auth-page">
      <button
        type="button"
        className="auth-theme-btn"
        onClick={toggleTheme}
        aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
      >
        {theme === 'dark' ? <SunIcon size={18} /> : <MoonIcon size={18} />}
      </button>
      <div className="auth-card panel">
        <Link href="/" className="auth-brand">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/brand-mark.svg" alt="" width={44} height={44} />
          <span>Thread<span className="accent">ly</span></span>
        </Link>
        <h1>Forgot password</h1>
        <p className="muted auth-lead">Enter your account email. We will send a reset link if it exists.</p>
        <form className="form" onSubmit={onSubmit}>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <button className="btn" type="submit" disabled={busy}>
            {busy ? <SpinnerLabel label="Sending…" /> : 'Send reset link'}
          </button>
        </form>
        <p className="muted" style={{ marginBottom: 0 }}>
          <Link href="/login">Back to login</Link>
        </p>
      </div>
    </div>
  );
}
