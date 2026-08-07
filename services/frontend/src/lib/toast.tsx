'use client';

import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';

export type ToastKind = 'success' | 'error' | 'info';

type Toast = {
  id: number;
  kind: ToastKind;
  message: string;
};

type ConfirmOptions = {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
};

type ToastContextValue = {
  toast: (message: string, kind?: ToastKind) => void;
  success: (message: string) => void;
  error: (message: string) => void;
  info: (message: string) => void;
  confirm: (options: ConfirmOptions | string) => Promise<boolean>;
};

const ToastContext = createContext<ToastContextValue | null>(null);

let nextId = 1;

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [confirmState, setConfirmState] = useState<{
    options: ConfirmOptions;
    resolve: (v: boolean) => void;
  } | null>(null);

  const remove = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const toast = useCallback((message: string, kind: ToastKind = 'info') => {
    const clean = (message || 'Something went wrong').trim().slice(0, 160);
    const id = nextId++;
    setToasts((prev) => [...prev.slice(-4), { id, kind, message: clean }]);
    window.setTimeout(() => remove(id), 4200);
  }, [remove]);

  const confirm = useCallback((options: ConfirmOptions | string) => {
    const opts: ConfirmOptions = typeof options === 'string'
      ? { message: options }
      : options;
    return new Promise<boolean>((resolve) => {
      setConfirmState({ options: opts, resolve });
    });
  }, []);

  const value = useMemo<ToastContextValue>(() => ({
    toast,
    success: (m) => toast(m, 'success'),
    error: (m) => toast(m, 'error'),
    info: (m) => toast(m, 'info'),
    confirm,
  }), [toast, confirm]);

  const closeConfirm = (result: boolean) => {
    if (!confirmState) return;
    confirmState.resolve(result);
    setConfirmState(null);
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.kind}`} role="status">
            <span>{t.message}</span>
            <button type="button" className="toast-close" onClick={() => remove(t.id)} aria-label="Dismiss">
              ×
            </button>
          </div>
        ))}
      </div>
      {confirmState && (
        <div className="modal-backdrop" role="presentation" onClick={() => closeConfirm(false)}>
          <div
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-title"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="confirm-title" className="modal-title">
              {confirmState.options.title || 'Please confirm'}
            </h2>
            <p className="modal-body">{confirmState.options.message}</p>
            <div className="modal-actions">
              <button type="button" className="btn secondary" onClick={() => closeConfirm(false)}>
                {confirmState.options.cancelLabel || 'Cancel'}
              </button>
              <button type="button" className="btn" onClick={() => closeConfirm(true)}>
                {confirmState.options.confirmLabel || 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return ctx;
}
