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

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();
  const { success, error } = useToast();
  const { theme, toggleTheme } = useTheme();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await register(username, email, password);
      success('Account created. Welcome!');
      router.push('/');
    } catch (err) {
      error(friendlyError(err, 'Could not create account.'));
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
        <h1>Join Threadly</h1>
        <p className="muted auth-lead">Create an account to post, vote, and follow people.</p>
        <form className="form" onSubmit={onSubmit}>
          <label>
            Username
            <input value={username} onChange={(e) => setUsername(e.target.value)} minLength={3} required />
          </label>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={6} required />
          </label>
          <button className="btn" type="submit" disabled={busy}>
            {busy ? <SpinnerLabel label="Creating…" /> : 'Sign up'}
          </button>
        </form>
        <p className="muted" style={{ marginBottom: 0 }}>
          Already have an account? <Link href="/login">Log in</Link>
        </p>
      </div>
    </div>
  );
}
