'use client';

import { useEffect, useId, useRef, useState } from 'react';

declare global {
  interface Window {
    grecaptcha?: {
      render: (
        container: HTMLElement,
        parameters: {
          sitekey: string;
          theme?: 'light' | 'dark';
          callback?: (token: string) => void;
          'expired-callback'?: () => void;
          'error-callback'?: () => void;
        },
      ) => number;
      reset: (widgetId?: number) => void;
      getResponse: (widgetId?: number) => string;
    };
    __threadlyRecaptchaLoaders?: Array<() => void>;
    onThreadlyRecaptchaLoad?: () => void;
  }
}

const SCRIPT_ID = 'threadly-recaptcha-v2';
const SCRIPT_SRC =
  'https://www.google.com/recaptcha/api.js?onload=onThreadlyRecaptchaLoad&render=explicit';

function ensureRecaptchaScript(): Promise<void> {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('No window'));
  }
  if (window.grecaptcha?.render) {
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    const queue = window.__threadlyRecaptchaLoaders ?? [];
    window.__threadlyRecaptchaLoaders = queue;
    queue.push(() => resolve());

    window.onThreadlyRecaptchaLoad = () => {
      const loaders = window.__threadlyRecaptchaLoaders ?? [];
      window.__threadlyRecaptchaLoaders = [];
      loaders.forEach((fn) => fn());
    };

    if (document.getElementById(SCRIPT_ID)) {
      return;
    }
    const script = document.createElement('script');
    script.id = SCRIPT_ID;
    script.src = SCRIPT_SRC;
    script.async = true;
    script.defer = true;
    script.onerror = () => reject(new Error('Failed to load reCAPTCHA'));
    document.head.appendChild(script);
  });
}

type Props = {
  siteKey: string;
  theme?: 'light' | 'dark';
  onToken: (token: string | null) => void;
};

/** Google reCAPTCHA v2 checkbox. Calls onToken when solved / expired. */
export function RecaptchaV2({ siteKey, theme = 'light', onToken }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const widgetIdRef = useRef<number | null>(null);
  const onTokenRef = useRef(onToken);
  const [error, setError] = useState<string | null>(null);
  const uid = useId();

  useEffect(() => {
    onTokenRef.current = onToken;
  }, [onToken]);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    onTokenRef.current(null);

    ensureRecaptchaScript()
      .then(() => {
        if (cancelled || !containerRef.current || !window.grecaptcha) return;
        // Avoid double-render on Strict Mode: clear previous DOM children
        containerRef.current.innerHTML = '';
        widgetIdRef.current = window.grecaptcha.render(containerRef.current, {
          sitekey: siteKey,
          theme,
          callback: (token) => onTokenRef.current(token),
          'expired-callback': () => onTokenRef.current(null),
          'error-callback': () => {
            onTokenRef.current(null);
            setError('CAPTCHA failed to load. Refresh and try again.');
          },
        });
      })
      .catch(() => {
        if (!cancelled) setError('Could not load CAPTCHA. Check your network.');
      });

    return () => {
      cancelled = true;
      widgetIdRef.current = null;
      onTokenRef.current(null);
    };
  }, [siteKey, theme]);

  return (
    <div className="recaptcha-field" data-recaptcha-instance={uid}>
      <div ref={containerRef} className="recaptcha-widget" />
      {error ? <p className="field-error muted">{error}</p> : null}
    </div>
  );
}

export function resetRecaptcha() {
  try {
    window.grecaptcha?.reset();
  } catch {
    // ignore
  }
}
