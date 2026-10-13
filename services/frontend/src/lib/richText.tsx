import { Fragment, type ReactNode } from 'react';

/** Markdown-style inline links: [label](https://…) */
const INLINE_LINK_RE = /\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g;

export function normalizeHref(raw: string) {
  const t = raw.trim();
  if (!t) return '';
  return /^https?:\/\//i.test(t) ? t : `https://${t}`;
}

/** Wrap a textarea selection (or insert at cursor) with a hyperlink. */
export function wrapSelectionWithLink(
  content: string,
  start: number,
  end: number,
  url: string,
  fallbackLabel = 'link',
) {
  const href = normalizeHref(url);
  if (!href) return { next: content, cursor: end };

  const from = Math.max(0, Math.min(start, end, content.length));
  const to = Math.max(0, Math.min(Math.max(start, end), content.length));
  const selected = content.slice(from, to);
  const label = (selected || fallbackLabel).trim() || fallbackLabel;
  const md = `[${label}](${href})`;
  const next = `${content.slice(0, from)}${md}${content.slice(to)}`;
  return { next, cursor: from + md.length };
}

/** Renders [label](url) as clickable hyperlinks (hover shows full URL). */
export function renderLinkedText(text: string, className?: string): ReactNode {
  if (!text) return null;
  const nodes: ReactNode[] = [];
  let last = 0;
  let match: RegExpExecArray | null;
  const re = new RegExp(INLINE_LINK_RE.source, 'g');
  let i = 0;
  while ((match = re.exec(text)) !== null) {
    if (match.index > last) {
      nodes.push(<Fragment key={`t-${i++}`}>{text.slice(last, match.index)}</Fragment>);
    }
    nodes.push(
      <a
        key={`a-${i++}`}
        href={match[2]}
        title={match[2]}
        target="_blank"
        rel="noreferrer"
        className={className || 'inline-text-link'}
        onClick={(e) => e.stopPropagation()}
      >
        {match[1]}
      </a>,
    );
    last = match.index + match[0].length;
  }
  if (last < text.length) {
    nodes.push(<Fragment key={`t-${i++}`}>{text.slice(last)}</Fragment>);
  }
  return nodes.length ? nodes : text;
}

export function hasInlineLinks(text: string) {
  return new RegExp(INLINE_LINK_RE.source, 'g').test(text || '');
}
