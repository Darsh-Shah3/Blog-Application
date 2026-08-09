'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';
import { useAuth } from '@/lib/auth';
import { friendlyError } from '@/lib/api';
import { useToast } from '@/lib/toast';
import { SpinnerLabel } from '@/components/loaders';
import { useTheme } from '@/lib/theme';
import { MoonIcon, SunIcon } from '@/components/icons';
import { SocialAuthButtons } from '@/components/SocialAuthButtons';

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const { success, error } = useToast();
  const { theme, toggleTheme } = useTheme();
  const [emailOrUsername, setEmailOrUsername] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await login(emailOrUsername, password);
      success('Signed in.');
      router.push('/');
    } catch (err) {
      error(friendlyError(err, 'Could not sign in. Check your details.'));
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
        <h1>Welcome back</h1>
        <p className="muted auth-lead">Sign in to join communities and post.</p>

        <form className="form" onSubmit={onSubmit}>
          <label>
            Email or username
            <input value={emailOrUsername} onChange={(e) => setEmailOrUsername(e.target.value)} required autoComplete="username" />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
          </label>
          <button className="btn" type="submit" disabled={busy}>
            {busy ? <SpinnerLabel label="Signing in…" /> : 'Log in'}
          </button>
        </form>

        <SocialAuthButtons mode="login" />

        <div className="auth-footer">
          <p className="muted">
            <Link href="/forgot-password">Forgot password?</Link>
          </p>
          <p className="muted">
            New here? <Link href="/register">Create an account</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
