'use client';

import { KeyboardEvent, RefObject, useEffect, useState } from 'react';
import { normalizeHref, wrapSelectionWithLink } from '@/lib/richText';

type Selection = { start: number; end: number; text: string };

function readSelection(el: HTMLTextAreaElement | null): Selection | null {
  if (!el) return null;
  const start = el.selectionStart ?? 0;
  const end = el.selectionEnd ?? 0;
  return {
    start,
    end,
    text: el.value.slice(Math.min(start, end), Math.max(start, end)),
  };
}

/**
 * Google Docs–style: select words in the body, attach a URL, keep writing.
 * Stays open while the parent Link toggle is on.
 */
export function SelectionLinkPanel({
  open,
  bodyRef,
  content,
  onChange,
  onApplied,
  onError,
}: {
  open: boolean;
  bodyRef: RefObject<HTMLTextAreaElement | null>;
  content: string;
  onChange: (next: string) => void;
  onApplied?: (label: string) => void;
  onError?: (message: string) => void;
}) {
  const [selection, setSelection] = useState<Selection | null>(null);
  const [url, setUrl] = useState('');
  const [manualLabel, setManualLabel] = useState('');

  const refreshSelection = () => {
    setSelection(readSelection(bodyRef.current));
  };

  useEffect(() => {
    if (!open) return;
    refreshSelection();
    setUrl('');
    setManualLabel('');
  }, [open, bodyRef]);

  if (!open) return null;

  const selectedText = selection?.text?.trim() || '';
  const hasSelection = selectedText.length > 0;

  const apply = () => {
    const href = normalizeHref(url);
    if (!href) {
      onError?.('Enter a URL.');
      return;
    }

    // Re-read selection in case the user changed it after opening the panel.
    const live = readSelection(bodyRef.current) || selection;
    const start = live?.start ?? content.length;
    const end = live?.end ?? start;
    const liveSelected = live?.text?.trim() || '';
    const label = liveSelected || manualLabel.trim();

    if (!label) {
      onError?.('Select or type text to link.');
      return;
    }

    const result = liveSelected
      ? wrapSelectionWithLink(content, start, end, href)
      : wrapSelectionWithLink(content, end, end, href, label);

    onChange(result.next);
    onApplied?.(label);
    setUrl('');
    setManualLabel('');
    setSelection({ start: result.cursor, end: result.cursor, text: '' });
    requestAnimationFrame(() => {
      const area = bodyRef.current;
      if (!area) return;
      area.focus();
      area.setSelectionRange(result.cursor, result.cursor);
    });
  };

  const onKey = (e: KeyboardEvent) => {
    e.stopPropagation();
    if (e.key === 'Enter') {
      e.preventDefault();
      apply();
    }
  };

  return (
    <div
      className="selection-link-panel"
      onKeyDown={(e) => e.stopPropagation()}
      onFocus={refreshSelection}
      onClick={refreshSelection}
    >
      {hasSelection ? (
        <p className="selection-link-selected">
          <mark className="selection-link-mark">&ldquo;{selectedText}&rdquo;</mark>
        </p>
      ) : (
        <input
          className="input"
          value={manualLabel}
          onChange={(e) => setManualLabel(e.target.value)}
          onKeyDown={onKey}
          placeholder="Text to link"
          maxLength={120}
        />
      )}

      <div className="composer-inline-link-row">
        <input
          id="selection-link-url"
          className="input"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          onKeyDown={onKey}
          onFocus={refreshSelection}
          placeholder="https://"
        />
        <button type="button" className="btn" disabled={!url.trim()} onClick={() => apply()}>
          Apply
        </button>
      </div>
    </div>
  );
}
