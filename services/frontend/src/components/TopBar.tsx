'use client';

import { FormEvent, useState, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { SearchIcon } from '@/components/icons';

/**
 * Contextual search + sort tools for browse columns (stays in the main column, not over the rail).
 */
export function TopBar({
  searchPlaceholder = 'Search…',
  defaultSearch = '',
  onSearch,
  filters,
  actions,
  crumb,
}: {
  searchPlaceholder?: string;
  defaultSearch?: string;
  onSearch: (q: string) => void;
  filters?: ReactNode;
  actions?: ReactNode;
  crumb?: string;
}) {
  const router = useRouter();
  const [q, setQ] = useState(defaultSearch);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    onSearch(q);
  };

  return (
    <header className="top-bar" role="banner">
      <div className="top-bar-row top-bar-row-search">
        {crumb && <span className="top-bar-crumb">{crumb}</span>}
        <form className="top-bar-search" onSubmit={submit}>
          <SearchIcon size={16} />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder={searchPlaceholder}
            aria-label={searchPlaceholder}
          />
          <button type="submit" className="btn secondary top-bar-search-btn">Search</button>
        </form>
      </div>
      {(filters || actions) && (
        <div className="top-bar-row top-bar-row-tools">
          {filters && <div className="top-bar-filters">{filters}</div>}
          <div className="top-bar-actions">
            {actions}
            <button
              type="button"
              className="btn ghost top-bar-refresh"
              title="Reload page"
              onClick={() => router.refresh()}
            >
              ↻
            </button>
          </div>
        </div>
      )}
      {!filters && !actions && (
        <button
          type="button"
          className="btn ghost top-bar-refresh top-bar-refresh-solo"
          title="Reload page"
          onClick={() => router.refresh()}
        >
          ↻
        </button>
      )}
    </header>
  );
}
