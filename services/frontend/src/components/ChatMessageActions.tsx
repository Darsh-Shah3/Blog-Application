'use client';

import { FormEvent, useEffect, useRef, useState } from 'react';
import { MoreVerticalIcon } from '@/components/icons';

export type ChatMessageActionsProps = {
  canEdit: boolean;
  canDeleteForEveryone: boolean;
  canDeleteForMe: boolean;
  onEdit: (body: string) => Promise<void> | void;
  onDeleteForMe: () => Promise<void> | void;
  onDeleteForEveryone: () => Promise<void> | void;
  initialBody?: string;
};

export function ChatMessageActions({
  canEdit,
  canDeleteForEveryone,
  canDeleteForMe,
  onEdit,
  onDeleteForMe,
  onDeleteForEveryone,
  initialBody = '',
}: ChatMessageActionsProps) {
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(initialBody);
  const [busy, setBusy] = useState(false);
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, [open]);

  if (!canEdit && !canDeleteForEveryone && !canDeleteForMe) {
    return null;
  }

  const run = async (fn: () => Promise<void> | void) => {
    setBusy(true);
    try {
      await fn();
      setOpen(false);
      setEditing(false);
    } finally {
      setBusy(false);
    }
  };

  const submitEdit = async (e: FormEvent) => {
    e.preventDefault();
    if (!draft.trim()) return;
    await run(() => onEdit(draft.trim()));
  };

  return (
    <div className="chat-msg-actions" ref={rootRef}>
      <button
        type="button"
        className="chat-msg-menu-btn"
        aria-label="Message options"
        aria-expanded={open}
        disabled={busy}
        onClick={() => {
          setOpen((v) => !v);
          setEditing(false);
          setDraft(initialBody);
        }}
      >
        <MoreVerticalIcon size={16} />
      </button>
      {open && !editing && (
        <div className="chat-msg-menu" role="menu">
          {canEdit && (
            <button
              type="button"
              role="menuitem"
              disabled={busy}
              onClick={() => {
                setEditing(true);
                setDraft(initialBody);
              }}
            >
              Edit
            </button>
          )}
          {canDeleteForMe && (
            <button
              type="button"
              role="menuitem"
              disabled={busy}
              onClick={() => run(onDeleteForMe)}
            >
              Delete for you
            </button>
          )}
          {canDeleteForEveryone && (
            <button
              type="button"
              role="menuitem"
              className="danger"
              disabled={busy}
              onClick={() => {
                if (!window.confirm('Delete this message for everyone?')) return;
                return run(onDeleteForEveryone);
              }}
            >
              Delete for everyone
            </button>
          )}
        </div>
      )}
      {open && editing && (
        <form className="chat-msg-edit" onSubmit={submitEdit}>
          <input
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            maxLength={2000}
            autoFocus
            disabled={busy}
          />
          <div className="chat-msg-edit-actions">
            <button type="button" className="btn ghost" disabled={busy} onClick={() => setEditing(false)}>
              Cancel
            </button>
            <button type="submit" className="btn" disabled={busy || !draft.trim()}>
              Save
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
