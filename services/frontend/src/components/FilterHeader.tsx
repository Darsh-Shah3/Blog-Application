'use client';

import { FormEvent, ReactNode } from 'react';
import { FilterIcon, SearchIcon } from '@/components/icons';

/**
 * Shared search + filter strip used on list / admin pages.
 */
export function FilterHeader({
  title,
  subtitle,
  search,
  onSearchChange,
  onSearchSubmit,
  searchPlaceholder = 'Search…',
  filters,
  trailing,
}: {
  title?: string;
  subtitle?: string;
  search: string;
  onSearchChange: (v: string) => void;
  onSearchSubmit?: () => void;
  searchPlaceholder?: string;
  filters?: ReactNode;
  trailing?: ReactNode;
}) {
  const submit = (e: FormEvent) => {
    e.preventDefault();
    onSearchSubmit?.();
  };

  return (
    <section className="filter-header" aria-label={title || 'Search and filters'}>
      <div className="filter-header-top">
        <div className="filter-header-titles">
          <span className="filter-header-icon" aria-hidden>
            <FilterIcon size={16} />
          </span>
          <div>
            {title && <h2 className="filter-header-title">{title}</h2>}
            {subtitle && <p className="filter-header-sub muted">{subtitle}</p>}
          </div>
        </div>
        {trailing}
      </div>
      <form className="filter-header-form" onSubmit={submit}>
        <label className="filter-search">
          <SearchIcon size={16} />
          <input
            className="input"
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder={searchPlaceholder}
            aria-label={searchPlaceholder}
          />
        </label>
        <button type="submit" className="btn secondary">
          Search
        </button>
        {filters}
      </form>
    </section>
  );
}
