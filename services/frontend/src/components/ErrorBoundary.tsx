'use client';

import React from 'react';

type Props = { children: React.ReactNode };

type State = { hasError: boolean };

/**
 * Catches render failures so a single broken section does not blank the whole app.
 */
export class ErrorBoundary extends React.Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error) {
    console.error('UI error boundary:', error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="panel" style={{ marginTop: '1.5rem' }}>
          <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Something went wrong</h2>
          <p className="muted">Refresh the page or go back home. Your data is safe.</p>
          <button className="btn" type="button" onClick={() => this.setState({ hasError: false })}>
            Try again
          </button>
          {' '}
          <a className="btn secondary" href="/">Home</a>
        </div>
      );
    }
    return this.props.children;
  }
}
