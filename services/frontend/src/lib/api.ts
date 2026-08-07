export type User = {
  id: number;
  username: string;
  email: string;
  displayName: string;
  bio?: string | null;
  karma: number;
  roles?: string[];
  permissions?: string[];
};

export type RoleInfo = {
  name: string;
  displayName: string;
  description: string;
  permissions: string[];
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  user: User;
};

export type Community = {
  id: number;
  name: string;
  slug: string;
  description?: string | null;
  creatorId: number;
  memberCount: number;
  joined?: boolean;
};

export type Post = {
  id: number;
  communityId: number;
  communityName?: string;
  communitySlug?: string;
  authorId: number;
  authorUsername?: string;
  title: string;
  content?: string | null;
  postType: 'TEXT' | 'LINK' | 'IMAGE' | 'FILE';
  linkUrl?: string | null;
  mediaId?: number | null;
  score: number;
  commentCount: number;
  createdAt: string;
};

export type CommentNode = {
  id: number;
  postId: number;
  authorId: number;
  authorUsername?: string;
  parentId?: number | null;
  content: string;
  score: number;
  createdAt: string;
  replies: CommentNode[];
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type NotificationItem = {
  id: number;
  userId: number;
  type: string;
  title: string;
  body?: string | null;
  linkUrl?: string | null;
  actorUsername?: string | null;
  resourceType?: string | null;
  resourceId?: string | null;
  readAt?: string | null;
  createdAt: string;
  read: boolean;
};

export type FollowStatus = {
  userId: number;
  username: string;
  following: boolean;
  followerCount: number;
  followingCount: number;
};

export type AuditEvent = {
  id: number;
  occurredAt: string;
  serviceName: string;
  action: string;
  resourceType: string;
  resourceId?: string | null;
  actorUserId?: number | null;
  actorUsername?: string | null;
  summary?: string | null;
  requestId?: string | null;
};

export type PlatformReport = {
  totalAuditEvents: number;
  auditEventsLast24h: number;
  auditEventsLast7d: number;
  actionsLast7d: Record<string, number>;
  servicesLast7d: Record<string, number>;
  resourceActionsLast7d: { resourceType: string; action: string; count: number }[];
  activeUsers: number;
  totalPosts: number;
  totalCommunities: number;
  totalComments: number;
  generatedAt: string;
};

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const TOKEN_KEY = 'threadly_token';

export function getApiUrl() {
  return API_URL;
}

export function getStoredToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(TOKEN_KEY);
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

/**
 * Map API failures to short, actionable messages for toasts.
 */
export function friendlyError(err: unknown, fallback = 'Something went wrong. Try again.'): string {
  if (err instanceof ApiError) {
    const msg = (err.message || '').trim();
    if (err.status === 401) return 'Please sign in to continue.';
    if (err.status === 403) return 'You do not have permission for this.';
    if (err.status === 404) return 'Not found. Check the link or try again.';
    if (err.status === 409) {
      if (/email/i.test(msg)) return 'That email is already registered.';
      if (/username/i.test(msg)) return 'That username is taken.';
      return 'Already exists. Try a different value.';
    }
    if (err.status === 413 || /too large/i.test(msg)) return 'File is too large. Use a smaller file.';
    if (err.status === 429) return 'Too many requests. Wait a minute and try again.';
    if (err.status >= 500) return 'Server error. Please try again shortly.';
    // Prefer short backend messages; strip stack-ish noise
    if (msg && msg.length <= 140 && !/exception|stacktrace|at com\./i.test(msg)) {
      return msg.endsWith('.') ? msg : `${msg}.`;
    }
  }
  if (err instanceof Error && err.message) {
    const m = err.message.trim();
    if (m.length <= 140) return m.endsWith('.') ? m : `${m}.`;
  }
  return fallback;
}

const PUBLIC_PREFIXES = [
  '/api/v1/auth/login',
  '/api/v1/auth/signup',
  '/api/v1/auth/forgot-password',
  '/api/v1/auth/reset-password',
];

/**
 * All business APIs require `Authorization: Bearer <jwt>` except public auth routes.
 */
export async function api<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null
): Promise<T> {
  const headers = new Headers(options.headers || {});
  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const bearer = token !== undefined ? token : getStoredToken();
  const isPublic = PUBLIC_PREFIXES.some((p) => path.startsWith(p));
  if (bearer) {
    headers.set('Authorization', `Bearer ${bearer}`);
  } else if (!isPublic) {
    throw new ApiError(401, 'Please sign in to continue.');
  }

  let res: Response;
  try {
    res = await fetch(`${API_URL}${path}`, {
      ...options,
      headers,
      cache: 'no-store',
    });
  } catch {
    throw new ApiError(0, 'Cannot reach server. Is the API running?');
  }

  if (res.status === 401 && !isPublic && typeof window !== 'undefined') {
    localStorage.removeItem(TOKEN_KEY);
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      message = body.message || body.error || message;
    } catch {
      // ignore parse errors
    }
    if (res.status === 429) {
      message = 'Too many requests. Wait a minute and try again.';
    }
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return res.json() as Promise<T>;
}
