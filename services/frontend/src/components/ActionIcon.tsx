'use client';

import type { ButtonHTMLAttributes, ReactNode } from 'react';
import Link from 'next/link';
import {
  EyeIcon,
  PenIcon,
  PlusIcon,
  TrashIcon,
} from '@/components/icons';

type Tone = 'neutral' | 'danger' | 'accent' | 'teal';

/** Icon-first control for create / view / edit / delete. */
export function IconAction({
  label,
  tone = 'neutral',
  className,
  children,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  label: string;
  tone?: Tone;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      className={`icon-action icon-action-${tone}${className ? ` ${className}` : ''}`}
      title={label}
      aria-label={label}
      {...rest}
    >
      {children}
    </button>
  );
}

export function IconLink({
  href,
  label,
  tone = 'neutral',
  children,
  onClick,
  className,
}: {
  href: string;
  label: string;
  tone?: Tone;
  children: ReactNode;
  onClick?: (e: React.MouseEvent) => void;
  className?: string;
}) {
  return (
    <Link
      href={href}
      className={`icon-action icon-action-${tone}${className ? ` ${className}` : ''}`}
      title={label}
      aria-label={label}
      onClick={onClick}
    >
      {children}
    </Link>
  );
}

export function DeleteIconBtn(
  props: Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & { label?: string }
) {
  const { label = 'Delete', ...rest } = props;
  return (
    <IconAction label={label} tone="danger" {...rest}>
      <TrashIcon size={16} />
    </IconAction>
  );
}

export function ViewIconLink({
  href,
  label = 'View',
  onClick,
}: {
  href: string;
  label?: string;
  onClick?: (e: React.MouseEvent) => void;
}) {
  return (
    <IconLink href={href} label={label} tone="teal" onClick={onClick}>
      <EyeIcon size={16} />
    </IconLink>
  );
}

export function EditIconBtn(
  props: Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & { label?: string }
) {
  const { label = 'Edit', ...rest } = props;
  return (
    <IconAction label={label} tone="accent" {...rest}>
      <PenIcon size={16} />
    </IconAction>
  );
}

export function CreateIconBtn(
  props: Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & { label?: string }
) {
  const { label = 'Create', ...rest } = props;
  return (
    <IconAction label={label} tone="accent" {...rest}>
      <PlusIcon size={16} />
    </IconAction>
  );
}
