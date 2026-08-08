'use client';

import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { FormEvent, Suspense, useState } from 'react';
import { api, friendlyError } from '@/lib/api';
import { useToast } from '@/lib/toast';
import { FormSkeleton, SpinnerLabel } from '@/components/loaders';
import { useTheme } from '@/lib/theme';
import { MoonIcon, SunIcon } from '@/components/icons';

function ResetForm() {
  const search = useSearchParams();
  const router = useRouter();
  const { success, error } = useToast();
  const { theme, toggleTheme } = useTheme();
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
        <h1>Set new password</h1>
        <p className="muted auth-lead">Choose a strong password for your account.</p>
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
            {busy ? <SpinnerLabel label="Saving…" /> : 'Update password'}
          </button>
        </form>
        <p className="muted" style={{ marginBottom: 0 }}>
          <Link href="/forgot-password">Request a new link</Link>
        </p>
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="auth-page"><FormSkeleton lines={3} /></div>}>
      <ResetForm />
    </Suspense>
  );
}
