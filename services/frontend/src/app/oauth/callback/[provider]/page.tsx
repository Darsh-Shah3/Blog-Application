'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';

/** OAuth return surface — providers redirect here after consent. */
export default function OAuthCallbackPage() {
  const params = useParams<{ provider: string }>();
  const provider = (params?.provider || 'provider').toString();

  return (
    <div className="auth-page">
      <div className="auth-card panel">
        <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Finishing sign-in</h1>
        <p className="muted">
          {provider.charAt(0).toUpperCase() + provider.slice(1)} hand-off is ready on the UI.
          Complete the server callback (client secret exchange) to issue a Threadly session.
        </p>
        <div className="toolbar">
          <Link className="btn" href="/login">Back to log in</Link>
          <Link className="btn secondary" href="/">Home</Link>
        </div>
      </div>
    </div>
  );
}
