'use client';

import { Suspense } from 'react';
import MessagesPage from './MessagesClient';

export default function MessagesRoute() {
  return (
    <Suspense fallback={<div className="page-shell page-shell-wide"><p className="muted">Loading messages…</p></div>}>
      <MessagesPage />
    </Suspense>
  );
}
