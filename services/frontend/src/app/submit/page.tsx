'use client';

import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense } from 'react';
import { FormSkeleton } from '@/components/loaders';

/** Old /submit URLs open the Posts page with the modal flow. */
function RedirectBody() {
  const router = useRouter();
  const search = useSearchParams();
  useEffect(() => {
    const cid = search.get('communityId');
    router.replace(cid ? `/feed` : '/feed');
  }, [router, search]);
  return (
    <div className="panel" style={{ marginTop: '1.5rem' }}>
      <p className="muted">Opening the post composer on Posts…</p>
    </div>
  );
}

export default function SubmitPage() {
  return (
    <Suspense fallback={<FormSkeleton lines={3} />}>
      <RedirectBody />
    </Suspense>
  );
}
