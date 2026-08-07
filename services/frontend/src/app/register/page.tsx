'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';
import { useAuth } from '@/lib/auth';
import { friendlyError } from '@/lib/api';
import { useToast } from '@/lib/toast';

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();
  const { success, error } = useToast();
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
    <div className="panel" style={{ maxWidth: 460, margin: '2rem auto' }}>
      <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Join Threadly</h1>
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
        <button className="btn" type="submit" disabled={busy}>{busy ? 'Creating…' : 'Sign up'}</button>
      </form>
      <p className="muted" style={{ marginBottom: 0 }}>
        Already have an account? <Link href="/login">Log in</Link>
      </p>
    </div>
  );
}
