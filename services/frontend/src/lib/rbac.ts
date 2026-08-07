import type { User } from './api';

export const ROLES = {
  USER: 'ROLE_USER',
  MODERATOR: 'ROLE_MODERATOR',
  ADMIN: 'ROLE_ADMIN',
} as const;

export function hasRole(user: User | null | undefined, role: string): boolean {
  if (!user?.roles?.length) return false;
  const target = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
  return user.roles.some((r) => r === target || r === role);
}

export function isAdmin(user: User | null | undefined): boolean {
  return hasRole(user, ROLES.ADMIN);
}

export function isModerator(user: User | null | undefined): boolean {
  return hasRole(user, ROLES.MODERATOR) || isAdmin(user);
}

export function canDeleteContent(
  user: User | null | undefined,
  authorId: number
): boolean {
  if (!user) return false;
  if (user.id === authorId) return true;
  if (user.permissions?.includes('CONTENT_DELETE_ANY')) return true;
  return isModerator(user);
}

export function roleLabel(role: string): string {
  switch (role) {
    case ROLES.ADMIN:
      return 'Admin';
    case ROLES.MODERATOR:
      return 'Moderator';
    case ROLES.USER:
      return 'Member';
    default:
      return role.replace(/^ROLE_/, '');
  }
}
