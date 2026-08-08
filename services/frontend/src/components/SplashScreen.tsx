'use client';

import { useAuth } from '@/lib/auth';
import { useEffect, useState } from 'react';

/**
 * Brand splash over the shell while the session is restoring.
 * Hides with a short fade once auth is ready (or after a safety timeout).
 */
export function SplashScreen() {
  const { loading } = useAuth();
  const [visible, setVisible] = useState(true);
  const [exiting, setExiting] = useState(false);

  useEffect(() => {
    if (loading) return;
    setExiting(true);
    const t = window.setTimeout(() => setVisible(false), 420);
    return () => window.clearTimeout(t);
  }, [loading]);

  // Safety: never block the UI forever
  useEffect(() => {
    const t = window.setTimeout(() => {
      setExiting(true);
      window.setTimeout(() => setVisible(false), 420);
    }, 8000);
    return () => window.clearTimeout(t);
  }, []);

  if (!visible) return null;

  return (
    <div
      className={`splash-screen ${exiting ? 'is-leaving' : ''}`}
      role="status"
      aria-live="polite"
      aria-label="Loading Threadly"
    >
      <div className="splash-inner">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src="/brand-mark.svg" alt="" className="splash-logo" width={72} height={72} />
        <p className="splash-brand">
          Thread<span>ly</span>
        </p>
        <div className="splash-bar" aria-hidden="true">
          <span className="splash-bar-fill" />
        </div>
        <p className="splash-caption muted">Loading your space…</p>
      </div>
    </div>
  );
}
