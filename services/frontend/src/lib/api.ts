export type User = {
  id: string;
  username: string;
  email: string;
  displayName: string;
  bio?: string | null;
  /** media-service id for display picture */
  avatarMediaId?: string | null;
  /** relative media content path when set */
  avatarUrl?: string | null;
  karma: number;
  roles?: string[];
  permissions?: string[];
  /** Soft-deleted: content may remain; login disabled */
  deleted?: boolean;
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

export type CaptchaConfig = {
  enabled: boolean;
  provider?: string | null;
  siteKey?: string | null;
};

export type Community = {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  creatorId: string;
  creatorUsername?: string;
  memberCount: number;
  joined?: boolean;
};

export type MediaRef = {
  id: string;
  originalName?: string;
  contentType?: string;
  mediaKind?: string;
  url?: string;
};

export type Post = {
  id: string;
  communityId: string;
  communityName?: string;
  communitySlug?: string;
  authorId: string;
  authorUsername?: string;
  title: string;
  content?: string | null;
  postType: 'TEXT' | 'LINK' | 'IMAGE' | 'FILE';
  linkUrls?: string[];
  media?: MediaRef[];
  score: number;
  commentCount: number;
  status?: 'PUBLISHED' | 'SCHEDULED';
  scheduledAt?: string | null;
  createdAt: string;
};

export type CommentNode = {
  id: string;
  postId: string;
  authorId: string;
  authorUsername?: string;
  parentId?: string | null;
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
  last?: boolean;
};

export type NotificationItem = {
  id: string;
  userId: string;
  type: string;
  title: string;
  body?: string | null;
  summary?: string | null;
  linkUrl?: string | null;
  mediaUrl?: string | null;
  actorUsername?: string | null;
  postTitle?: string | null;
  resourceType?: string | null;
  resourceId?: string | null;
  readAt?: string | null;
  createdAt: string;
  read: boolean;
};

export type FollowStatus = {
  userId: string;
  username: string;
  following: boolean;
  followerCount: number;
  followingCount: number;
};

export type AuditEvent = {
  id: string;
  occurredAt: string;
  serviceName: string;
  action: string;
  resourceType: string;
  resourceId?: string | null;
  actorUserId?: string | null;
  actorUsername?: string | null;
  summary?: string | null;
  beforeData?: string | null;
  afterData?: string | null;
  requestId?: string | null;
};

export type PlatformReport = {
  totalAuditEvents: number;
  auditEventsLast24h: number;
  auditEventsLast7d: number;
  actionsLast7d: Record<string, number>;
  servicesLast7d: Record<string, number>;
  resourceTypesLast7d?: Record<string, number>;
  resourceActionsLast7d: { resourceType: string; action: string; count: number }[];
  topActorsLast7d?: { name: string; count: number }[];
  auditEventsPerDay?: { day: string; count: number }[];
  activityByHourUtc?: { hour: number; count: number }[];
  activeUsers: number;
  totalPosts: number;
  totalCommunities: number;
  totalComments: number;
  reportDays?: number;
  generatedAt: string;
};

const TOKEN_KEY = 'threadly_token';

/**
 * Public API base URL.
 *
 * Deploy: set {@code NEXT_PUBLIC_API_URL} at **build** time (Next inlines it), e.g.
 * {@code https://api.yourdomain.com} — not {@code localhost}.
 * Dev default uses the browser host on port 8080 when env is unset.
 */
export function getApiUrl(): string {
  const env = (process.env.NEXT_PUBLIC_API_URL || '').trim().replace(/\/$/, '');
  if (env) return env;
  if (typeof window !== 'undefined') {
    const { protocol, hostname } = window.location;
    // Same machine / host default gateway — works for LAN IP as well as localhost.
    return `${protocol}//${hostname}:8080`;
  }
  return 'http://localhost:8080';
}

/** STOMP WebSocket endpoint (proxied by API gateway → community-service). */
export function getCommunityChatWsUrl(): string {
  const env = (process.env.NEXT_PUBLIC_WS_URL || '').trim().replace(/\/$/, '');
  if (env) return env;
  const api = getApiUrl();
  return api.replace(/^http/i, 'ws') + '/ws/community-chat';
}

/** Optional ops UI links — set per environment at build time. */
export function getGrafanaUrl(): string {
  return (process.env.NEXT_PUBLIC_GRAFANA_URL || 'http://localhost:3001').replace(/\/$/, '');
}

export function getPrometheusUrl(): string {
  return (process.env.NEXT_PUBLIC_PROMETHEUS_URL || 'http://localhost:9090').replace(/\/$/, '');
}

/** Direct media content URL (public GET) — use in <img> instead of blob downloads. */
export function mediaContentUrl(mediaId: string) {
  return `${getApiUrl()}/api/v1/media/${mediaId}/content`;
}

export type OpsMetrics = {
  prometheusAvailable: boolean;
  generatedAt: string;
  requestRateByService: Record<string, number>;
  p95LatencySecondsByService: Record<string, number>;
  error5xxRateByService: Record<string, number>;
  heapUsedBytesByService: Record<string, number>;
  error4xxRateByService?: Record<string, number>;
  cpuUsageByService?: Record<string, number>;
  threadsByService?: Record<string, number>;
  charts: {
    id: string;
    title: string;
    unit: string;
    lines: { label: string; points: { t: number; v: number }[] }[];
  }[];
};

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

const PUBLIC_AUTH_PREFIXES = [
  '/api/v1/auth/login',
  '/api/v1/auth/signup',
  '/api/v1/auth/forgot-password',
  '/api/v1/auth/reset-password',
  '/api/v1/auth/captcha-config',
  '/api/v1/auth/oauth/',
];

/** List/browse GETs work without a session; by-id and mutations need JWT. */
function isPublicApiPath(path: string, method = 'GET'): boolean {
  const base = path.split('?')[0];
  const m = method.toUpperCase();
  if (PUBLIC_AUTH_PREFIXES.some((p) => base.startsWith(p))) return true;
  if (m !== 'GET') return false;
  if (base === '/api/v1/posts' || base === '/api/v1/communities' || base === '/api/v1/communities/discover') {
    return true;
  }
  if (base.startsWith('/api/v1/communities/by-slug/')) return true;
  if (/^\/api\/v1\/media\/[^/]+(\/content)?$/.test(base)) return true;
  if (base.startsWith('/api/v1/profiles/') && !base.includes('/follow')) return true;
  return false;
}

export async function listAdminUsers(token: string, q = '', size = 50): Promise<Page<User>> {
  const qs = q.trim() ? `&q=${encodeURIComponent(q.trim())}` : '';
  return api<Page<User>>(`/api/v1/admin/users?size=${size}${qs}`, {}, token);
}

export async function listAdminRoles(token: string): Promise<RoleInfo[]> {
  return api<RoleInfo[]>('/api/v1/admin/roles', {}, token);
}

export async function assignAdminRoles(
  token: string,
  userId: string,
  roles: string[]
): Promise<User> {
  return api<User>(
    `/api/v1/admin/users/${userId}/roles`,
    { method: 'PUT', body: JSON.stringify({ roles }) },
    token
  );
}

export async function deleteAdminUser(token: string, userId: string): Promise<void> {
  await api(`/api/v1/admin/users/${userId}`, { method: 'DELETE' }, token);
}

export async function createAdminUser(
  token: string,
  body: {
    username: string;
    email: string;
    password: string;
    displayName?: string;
    roles?: string[];
  }
): Promise<User> {
  return api<User>(
    '/api/v1/admin/users',
    { method: 'POST', body: JSON.stringify(body) },
    token
  );
}

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
  const method = (options.method || 'GET').toUpperCase();
  const isPublic = isPublicApiPath(path, method);
  if (bearer) {
    headers.set('Authorization', `Bearer ${bearer}`);
  } else if (!isPublic) {
    throw new ApiError(401, 'Please sign in to continue.');
  }

  let res: Response;
  try {
    res = await fetch(`${getApiUrl()}${path}`, {
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
