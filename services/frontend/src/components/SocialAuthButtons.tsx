'use client';

import { useToast } from '@/lib/toast';
import { getApiUrl } from '@/lib/api';

function GoogleMark() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden>
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1Z" />
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23Z" />
      <path fill="#FBBC05" d="M5.84 14.09A6.97 6.97 0 0 1 5.48 12c0-.72.12-1.41.33-2.09V7.07H2.18A11 11 0 0 0 1 12c0 1.78.43 3.47 1.18 4.93l3.66-2.84Z" />
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53Z" />
    </svg>
  );
}

/**
 * Google continue button for login / register (academic demos: configure client id only).
 */
export function SocialAuthButtons({ mode }: { mode: 'login' | 'signup' }) {
  const { error, info } = useToast();

  const startGoogle = async () => {
    const api = getApiUrl();
    const returnTo = typeof window !== 'undefined'
      ? `${window.location.origin}/login?oauth=google`
      : '';
    try {
      const res = await fetch(
        `${api}/api/v1/auth/oauth/google/start?returnTo=${encodeURIComponent(returnTo)}`,
        { method: 'GET', headers: { Accept: 'application/json' } }
      );
      const body = await res.json().catch(() => ({}));
      if (res.ok && body.authorizationUrl) {
        window.location.href = body.authorizationUrl as string;
        return;
      }
      if (res.status === 501 || res.status === 503) {
        info(
          body.message
          || 'Google sign-in is almost ready — add OAUTH_GOOGLE_CLIENT_ID on the server. Email login still works.'
        );
        return;
      }
      error(body.message || 'Could not start Google sign-in.');
    } catch {
      error('Could not reach auth service for Google.');
    }
  };

  const action = mode === 'signup' ? 'Sign up' : 'Log in';

  return (
    <div className="social-auth social-auth-below">
      <div className="social-auth-divider" role="separator">
        <span>or {mode === 'signup' ? 'sign up' : 'log in'} with</span>
      </div>
      <div className="social-auth-grid social-auth-grid-single">
        <button
          type="button"
          className="social-auth-btn social-auth-google"
          onClick={startGoogle}
          aria-label={`${action} with Google`}
        >
          <GoogleMark />
          <span>Continue with Google</span>
        </button>
      </div>
      <p className="muted social-auth-footnote">
        One-tap access · we never post without your permission
      </p>
    </div>
  );
}
