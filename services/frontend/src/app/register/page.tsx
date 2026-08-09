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

type Step = 'consent' | 'form';

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();
  const { success, error, info } = useToast();
  const { theme, toggleTheme } = useTheme();
  const [step, setStep] = useState<Step>('consent');
  const [eduOk, setEduOk] = useState(false);
  const [riskOk, setRiskOk] = useState(false);
  const [liabilityOk, setLiabilityOk] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const allConsented = eduOk && riskOk && liabilityOk;

  const acceptConsent = () => {
    if (!allConsented) {
      info('Please accept all three statements to continue.');
      return;
    }
    try {
      sessionStorage.setItem('threadly_signup_consent', '1');
    } catch {
      // ignore
    }
    setStep('form');
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!allConsented) {
      setStep('consent');
      error('You must accept the educational disclaimer before creating an account.');
      return;
    }
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

        {step === 'consent' ? (
          <>
            <h1>Before you sign up</h1>
            <p className="muted auth-lead">
              Threadly is an educational demo. Please read and accept each statement. You can create an
              account only after you agree.
            </p>

            <div className="consent-box" role="group" aria-labelledby="consent-heading">
              <p id="consent-heading" className="consent-heading">
                Educational consent
              </p>
              <p className="muted consent-intro">
                Full details are also on the{' '}
                <Link href="/about">About page</Link>
                . By continuing you acknowledge the following:
              </p>

              <label className="consent-item">
                <input
                  type="checkbox"
                  checked={eduOk}
                  onChange={(e) => setEduOk(e.target.checked)}
                />
                <span>
                  I understand Threadly is provided <strong>for education and demonstration only</strong>
                  , not as a commercial or production service.
                </span>
              </label>

              <label className="consent-item">
                <input
                  type="checkbox"
                  checked={riskOk}
                  onChange={(e) => setRiskOk(e.target.checked)}
                />
                <span>
                  I understand that <strong>data may not stay secure</strong> (passwords, posts, messages,
                  files, profile data) on this demo environment.
                </span>
              </label>

              <label className="consent-item">
                <input
                  type="checkbox"
                  checked={liabilityOk}
                  onChange={(e) => setLiabilityOk(e.target.checked)}
                />
                <span>
                  I agree that the project owner and operators are <strong>not responsible for any data
                  loss, leaks, or misuse</strong>, and I will not store confidential or irreplaceable information here.
                </span>
              </label>
            </div>

            <button
              className="btn"
              type="button"
              disabled={!allConsented}
              onClick={acceptConsent}
            >
              I agree — continue to sign up
            </button>

            <div className="auth-footer">
              <p className="muted">
                Already have an account? <Link href="/login">Log in</Link>
                {' · '}
                <Link href="/about">About &amp; disclaimer</Link>
              </p>
            </div>
          </>
        ) : (
          <>
            <h1>Join Threadly</h1>
            <p className="muted auth-lead">
              Consent accepted. Create an account to post, vote, chat, and follow people on this demo.
            </p>
            <p className="consent-accepted-note">
              Educational disclaimer accepted.
              {' '}
              <button type="button" className="linkish" onClick={() => setStep('consent')}>
                Review again
              </button>
            </p>

            <form className="form" onSubmit={onSubmit}>
              <label>
                Username
                <input
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  minLength={3}
                  required
                  autoComplete="username"
                />
              </label>
              <label>
                Email
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  minLength={6}
                  required
                  autoComplete="new-password"
                />
              </label>
              <button className="btn" type="submit" disabled={busy || !allConsented}>
                {busy ? <SpinnerLabel label="Creating…" /> : 'Sign up'}
              </button>
            </form>

            <SocialAuthButtons mode="signup" />

            <div className="auth-footer">
              <p className="muted">
                Already have an account? <Link href="/login">Log in</Link>
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
