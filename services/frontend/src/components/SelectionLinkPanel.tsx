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
      onError?.('Paste the URL to open when someone clicks the selected text.');
      return;
    }

    // Re-read selection in case the user changed it after opening the panel.
    const live = readSelection(bodyRef.current) || selection;
    const start = live?.start ?? content.length;
    const end = live?.end ?? start;
    const liveSelected = live?.text?.trim() || '';
    const label = liveSelected || manualLabel.trim();

    if (!label) {
      onError?.('Select text in the body first, or type the words to link.');
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
      <div className="selection-link-panel-head">
        <strong>Add link to selected text</strong>
        <span className="muted">Select words in the body, paste a URL, Apply — then keep writing</span>
      </div>

      {hasSelection ? (
        <p className="selection-link-selected">
          Selected:{' '}
          <mark className="selection-link-mark">&ldquo;{selectedText}&rdquo;</mark>
          <button type="button" className="btn ghost" style={{ marginLeft: '0.35rem' }} onClick={refreshSelection}>
            Refresh
          </button>
        </p>
      ) : (
        <>
          <p className="muted" style={{ margin: 0, fontSize: '0.88rem' }}>
            Highlight text in the body above, or type the words here:
          </p>
          <input
            className="input"
            value={manualLabel}
            onChange={(e) => setManualLabel(e.target.value)}
            onKeyDown={onKey}
            placeholder="Words to link (e.g. github)"
            maxLength={120}
          />
        </>
      )}

      <label className="composer-field-label" htmlFor="selection-link-url">
        Link URL
      </label>
      <div className="composer-inline-link-row">
        <input
          id="selection-link-url"
          className="input"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          onKeyDown={onKey}
          onFocus={refreshSelection}
          placeholder="https://github.com/your-profile"
        />
        <button type="button" className="btn" disabled={!url.trim()} onClick={() => apply()}>
          Apply link
        </button>
      </div>
    </div>
  );
}
