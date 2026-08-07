'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';
import { useAuth } from '@/lib/auth';
import { friendlyError } from '@/lib/api';
import { useToast } from '@/lib/toast';

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const { success, error } = useToast();
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
    <div className="panel" style={{ maxWidth: 420, margin: '2rem auto' }}>
      <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Welcome back</h1>
      <form className="form" onSubmit={onSubmit}>
        <label>
          Email or username
          <input value={emailOrUsername} onChange={(e) => setEmailOrUsername(e.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="btn" type="submit" disabled={busy}>{busy ? 'Signing in…' : 'Log in'}</button>
      </form>
      <p className="muted">
        <Link href="/forgot-password">Forgot password?</Link>
      </p>
      <p className="muted" style={{ marginBottom: 0 }}>
        New here? <Link href="/register">Create an account</Link>
      </p>
    </div>
  );
}
