-- =============================================================================
-- Threadly demo data (one script, all domain DBs)
-- =============================================================================
-- Password for demo / alice / bob / carol / dave: password123
-- BCrypt: $2a$10$.ric6k1oL9I5MVyrpOYtvOR53CWZLlTunPwGKCr1DDnBU7pt6jm4m
--
-- Run (Docker Postgres):
--   Get-Content scripts\seed-demo-data.sql -Raw | docker exec -i threadly-postgres psql -U threadly -v ON_ERROR_STOP=1
--
-- Or: docker exec -i threadly-postgres psql -U threadly -v ON_ERROR_STOP=1 < scripts/seed-demo-data.sql
-- =============================================================================

\echo '>>> Seeding auth_db users'
\c auth_db

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Fixed demo user UUIDs (stable across community / post / comment rows)
-- demo  ...01  alice ...02  bob ...03  carol ...04  dave ...05

DELETE FROM user_roles
WHERE user_id IN (
    '11111111-1111-1111-1111-111111111101',
    '11111111-1111-1111-1111-111111111102',
    '11111111-1111-1111-1111-111111111103',
    '11111111-1111-1111-1111-111111111104',
    '11111111-1111-1111-1111-111111111105'
);
DELETE FROM user_follows
WHERE user_id IN (
    '11111111-1111-1111-1111-111111111101',
    '11111111-1111-1111-1111-111111111102',
    '11111111-1111-1111-1111-111111111103',
    '11111111-1111-1111-1111-111111111104',
    '11111111-1111-1111-1111-111111111105'
);
DELETE FROM users
WHERE id IN (
    '11111111-1111-1111-1111-111111111101',
    '11111111-1111-1111-1111-111111111102',
    '11111111-1111-1111-1111-111111111103',
    '11111111-1111-1111-1111-111111111104',
    '11111111-1111-1111-1111-111111111105'
)
   OR username IN ('demo', 'alice', 'bob', 'carol', 'dave');

INSERT INTO users (
    id, username, email, password_hash, display_name, bio, karma,
    created_at, updated_at, created_by, updated_by, deleted_at
) VALUES
(
    '11111111-1111-1111-1111-111111111101',
    'demo', 'demo@threadly.local',
    '$2a$10$.ric6k1oL9I5MVyrpOYtvOR53CWZLlTunPwGKCr1DDnBU7pt6jm4m',
    'Demo User', 'Portfolio demo account for Threadly.', 120,
    NOW() - INTERVAL '30 days', NOW() - INTERVAL '1 day', 'sql-seed', 'sql-seed', NULL
),
(
    '11111111-1111-1111-1111-111111111102',
    'alice', 'alice@threadly.local',
    '$2a$10$.ric6k1oL9I5MVyrpOYtvOR53CWZLlTunPwGKCr1DDnBU7pt6jm4m',
    'Alice Chen', 'Backend engineer who blogs about interviews.', 86,
    NOW() - INTERVAL '28 days', NOW() - INTERVAL '2 days', 'sql-seed', 'sql-seed', NULL
),
(
    '11111111-1111-1111-1111-111111111103',
    'bob', 'bob@threadly.local',
    '$2a$10$.ric6k1oL9I5MVyrpOYtvOR53CWZLlTunPwGKCr1DDnBU7pt6jm4m',
    'Bob Okonkwo', 'Full-stack builder. Loves clear UX.', 54,
    NOW() - INTERVAL '21 days', NOW() - INTERVAL '3 days', 'sql-seed', 'sql-seed', NULL
),
(
    '11111111-1111-1111-1111-111111111104',
    'carol', 'carol@threadly.local',
    '$2a$10$.ric6k1oL9I5MVyrpOYtvOR53CWZLlTunPwGKCr1DDnBU7pt6jm4m',
    'Carol Mitra', 'Design systems and product craft.', 67,
    NOW() - INTERVAL '18 days', NOW() - INTERVAL '1 day', 'sql-seed', 'sql-seed', NULL
),
(
    '11111111-1111-1111-1111-111111111105',
    'dave', 'dave@threadly.local',
    '$2a$10$.ric6k1oL9I5MVyrpOYtvOR53CWZLlTunPwGKCr1DDnBU7pt6jm4m',
    'Dave Rossi', 'Career coach. Ships weekly notes.', 41,
    NOW() - INTERVAL '14 days', NOW() - INTERVAL '5 hours', 'sql-seed', 'sql-seed', NULL
);

INSERT INTO user_roles (id, user_id, role_id, username, role_name)
SELECT gen_random_uuid(), u.id, r.id, u.username, r.name
FROM users u
CROSS JOIN roles r
WHERE r.name = 'ROLE_USER'
  AND u.id IN (
    '11111111-1111-1111-1111-111111111101',
    '11111111-1111-1111-1111-111111111102',
    '11111111-1111-1111-1111-111111111103',
    '11111111-1111-1111-1111-111111111104',
    '11111111-1111-1111-1111-111111111105'
  );

INSERT INTO user_follows (id, user_id, username, followers, following, updated_at) VALUES
(
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111101', 'demo',
    '[{"id":"11111111-1111-1111-1111-111111111102","username":"alice"},{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}]'::jsonb,
    '[{"id":"11111111-1111-1111-1111-111111111102","username":"alice"},{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}]'::jsonb,
    NOW()
),
(
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111102', 'alice',
    '[{"id":"11111111-1111-1111-1111-111111111101","username":"demo"},{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}]'::jsonb,
    '[{"id":"11111111-1111-1111-1111-111111111101","username":"demo"},{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}]'::jsonb,
    NOW()
),
(
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111103', 'bob',
    '[{"id":"11111111-1111-1111-1111-111111111101","username":"demo"},{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}]'::jsonb,
    '[{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}]'::jsonb,
    NOW()
),
(
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111104', 'carol',
    '[{"id":"11111111-1111-1111-1111-111111111101","username":"demo"},{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}]'::jsonb,
    '[{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}]'::jsonb,
    NOW()
),
(
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111105', 'dave',
    '[{"id":"11111111-1111-1111-1111-111111111102","username":"alice"},{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}]'::jsonb,
    '[{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}]'::jsonb,
    NOW()
);

\echo '>>> Seeding community_db'
\c community_db

CREATE EXTENSION IF NOT EXISTS pgcrypto;

TRUNCATE TABLE community_memberships, communities RESTART IDENTITY CASCADE;

INSERT INTO communities (
    id, name, slug, description, creator, creator_id, member_count,
    created_by, updated_by, created_at, updated_at
) VALUES
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
    'Programming', 'programming',
    'Code, systems, languages, and shipping software.',
    '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb,
    '11111111-1111-1111-1111-111111111101', 5,
    'sql-seed', 'sql-seed', NOW() - INTERVAL '25 days', NOW() - INTERVAL '1 day'
),
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
    'Design Systems', 'design-systems',
    'UI craft, product design, and design-engineering collaboration.',
    '{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}'::jsonb,
    '11111111-1111-1111-1111-111111111104', 4,
    'sql-seed', 'sql-seed', NOW() - INTERVAL '22 days', NOW() - INTERVAL '2 days'
),
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
    'Career Growth', 'career-growth',
    'Interviews, portfolios, promotions, and learning in public.',
    '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
    '11111111-1111-1111-1111-111111111102', 5,
    'sql-seed', 'sql-seed', NOW() - INTERVAL '20 days', NOW() - INTERVAL '3 hours'
),
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4',
    'DevOps & Cloud', 'devops-cloud',
    'Containers, CI/CD, observability, and reliable deploys.',
    '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb,
    '11111111-1111-1111-1111-111111111103', 3,
    'sql-seed', 'sql-seed', NOW() - INTERVAL '12 days', NOW() - INTERVAL '6 hours'
);

INSERT INTO community_memberships (id, community, community_id, member, user_id, joined_at) VALUES
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb, '11111111-1111-1111-1111-111111111101', NOW() - INTERVAL '20 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb, '11111111-1111-1111-1111-111111111102', NOW() - INTERVAL '18 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb, '11111111-1111-1111-1111-111111111103', NOW() - INTERVAL '15 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}'::jsonb, '11111111-1111-1111-1111-111111111104', NOW() - INTERVAL '12 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb, '11111111-1111-1111-1111-111111111105', NOW() - INTERVAL '10 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2","name":"Design Systems","slug":"design-systems"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb, '11111111-1111-1111-1111-111111111101', NOW() - INTERVAL '19 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2","name":"Design Systems","slug":"design-systems"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb, '11111111-1111-1111-1111-111111111103', NOW() - INTERVAL '14 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2","name":"Design Systems","slug":"design-systems"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}'::jsonb, '11111111-1111-1111-1111-111111111104', NOW() - INTERVAL '21 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2","name":"Design Systems","slug":"design-systems"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb, '11111111-1111-1111-1111-111111111105', NOW() - INTERVAL '8 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3","name":"Career Growth","slug":"career-growth"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb, '11111111-1111-1111-1111-111111111101', NOW() - INTERVAL '17 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3","name":"Career Growth","slug":"career-growth"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb, '11111111-1111-1111-1111-111111111102', NOW() - INTERVAL '19 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3","name":"Career Growth","slug":"career-growth"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb, '11111111-1111-1111-1111-111111111103', NOW() - INTERVAL '11 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3","name":"Career Growth","slug":"career-growth"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}'::jsonb, '11111111-1111-1111-1111-111111111104', NOW() - INTERVAL '9 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3","name":"Career Growth","slug":"career-growth"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb, '11111111-1111-1111-1111-111111111105', NOW() - INTERVAL '7 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4","name":"DevOps & Cloud","slug":"devops-cloud"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb, '11111111-1111-1111-1111-111111111101', NOW() - INTERVAL '11 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4","name":"DevOps & Cloud","slug":"devops-cloud"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb, '11111111-1111-1111-1111-111111111103', NOW() - INTERVAL '12 days'),
(gen_random_uuid(), '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4","name":"DevOps & Cloud","slug":"devops-cloud"}'::jsonb, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb, '11111111-1111-1111-1111-111111111105', NOW() - INTERVAL '6 days');

\echo '>>> Seeding post_db'
\c post_db

CREATE EXTENSION IF NOT EXISTS pgcrypto;

TRUNCATE TABLE posts RESTART IDENTITY CASCADE;

INSERT INTO posts (
    id, community, community_id, author, author_id,
    title, content, post_type, link_urls, media,
    score, comment_count, created_by, updated_by, created_at, updated_at,
    scheduled_at, status
) VALUES
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
    '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
    '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb,
    '11111111-1111-1111-1111-111111111101',
    'Welcome to Threadly — microservices demo feed',
    E'Browse as a guest, sign in to post, vote, join communities, and explore scheduled posts.\n\nThis environment is wired with an API gateway, JWT auth, Flyway migrations, and Docker Compose so you can walk the full path from register → feed → comments.\n\nTry joining Programming, voting on this post, and reading the comment thread for an architectural overview.',
    'TEXT', '[]'::jsonb, '[]'::jsonb,
    24, 4, 'sql-seed', 'sql-seed', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days',
    NULL, 'PUBLISHED'
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02',
    '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
    '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
    '11111111-1111-1111-1111-111111111102',
    'Spring Boot + Next.js stack walkthrough',
    E'The backend is a set of Spring Boot services (auth, community, post, comment, vote, media, notification, audit) behind a single gateway.\n\nThe frontend is Next.js with a Reddit-inspired layout — feed, communities, profiles, and YouTube-style comments on post detail.\n\nKey ops pieces: Postgres (DB-per-service), Prometheus/Grafana, Swagger on each service.',
    'TEXT', '[]'::jsonb, '[]'::jsonb,
    38, 5, 'sql-seed', 'sql-seed', NOW() - INTERVAL '36 hours', NOW() - INTERVAL '36 hours',
    NULL, 'PUBLISHED'
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03',
    '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2","name":"Design Systems","slug":"design-systems"}'::jsonb,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
    '{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}'::jsonb,
    '11111111-1111-1111-1111-111111111104',
    'Calm UI without the default AI look',
    E'Design goals for Threadly:\n- One strong composition per page (not a dashboard chrome soup)\n- Serif display type, restrained brand marks\n- Hoverable post rows that open the full article\n- Comment threads that feel like YouTube (avatar, name, time, reply) instead of plain text dumps\n\nDark mode is first-class; light mode keeps the same density.',
    'TEXT', '[]'::jsonb, '[]'::jsonb,
    29, 3, 'sql-seed', 'sql-seed', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day',
    NULL, 'PUBLISHED'
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04',
    '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3","name":"Career Growth","slug":"career-growth"}'::jsonb,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
    '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
    '11111111-1111-1111-1111-111111111102',
    'How I explain this project in interviews',
    E'Lead with user problems (communities, feed, auth), then architecture (bounded services, events), then trade-offs (consistency vs ownership).\n\nInterview structure I use:\n1. Walk the happy path (register → join → post → comment → vote)\n2. Show boundary boxes on a whiteboard\n3. Defend UUID + JSON snapshots for cross-service reads\n4. Mention failure modes (gateway down, partial comments) without overselling\n\nHiring managers care that you can ship a coherent story, not that you named every package.',
    'TEXT', '[]'::jsonb, '[]'::jsonb,
    51, 6, 'sql-seed', 'sql-seed', NOW() - INTERVAL '90 minutes', NOW() - INTERVAL '90 minutes',
    NULL, 'PUBLISHED'
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05',
    '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2","name":"Design Systems","slug":"design-systems"}'::jsonb,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
    '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb,
    '11111111-1111-1111-1111-111111111103',
    'Composer form vs classic submit page',
    E'The submit flow collects community, title, body, multi-link, multi-media, optional schedule, and notification blurb.\n\nUX tip: keep the first field as “where am I posting?” and only reveal advanced options after that choice feels solid.',
    'TEXT', '[]'::jsonb, '[]'::jsonb,
    16, 2, 'sql-seed', 'sql-seed', NOW() - INTERVAL '8 hours', NOW() - INTERVAL '8 hours',
    NULL, 'PUBLISHED'
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06',
    '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4","name":"DevOps & Cloud","slug":"devops-cloud"}'::jsonb,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4',
    '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb,
    '11111111-1111-1111-1111-111111111103',
    'Docker Compose layout that stays debuggable',
    E'One Postgres, many databases. Gateway on 8080, frontend on 3000, host Postgres remapped to 5433.\n\nWhen something fails: check service health, Flyway version in the target DB, then gateway routes. Prefer short compose rebuilds for the single service you changed.',
    'TEXT', '[]'::jsonb, '[]'::jsonb,
    22, 3, 'sql-seed', 'sql-seed', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours',
    NULL, 'PUBLISHED'
);

\echo '>>> Seeding comment_db'
\c comment_db

CREATE EXTENSION IF NOT EXISTS pgcrypto;

TRUNCATE TABLE comments RESTART IDENTITY CASCADE;

-- Helpers via fixed comment UUIDs (cc01..)
-- Post 01: 4 comments (1 reply)
INSERT INTO comments (id, post, post_id, author, author_id, parent, parent_id, content, score, created_by, updated_by, created_at, updated_at) VALUES
(
    'cccccccc-cccc-cccc-cccc-cccccccccc01',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01","title":"Welcome to Threadly — microservices demo feed"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
    '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
    '11111111-1111-1111-1111-111111111102',
    NULL, NULL,
    'Great starter post — the guest feed → login wall journey is clear.',
    12, 'sql-seed', 'sql-seed', NOW() - INTERVAL '46 hours', NOW() - INTERVAL '46 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc02',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01","title":"Welcome to Threadly — microservices demo feed"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
    '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb,
    '11111111-1111-1111-1111-111111111103',
    NULL, NULL,
    'I joined Programming and Career Growth — membership feels instant.',
    8, 'sql-seed', 'sql-seed', NOW() - INTERVAL '40 hours', NOW() - INTERVAL '40 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc03',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01","title":"Welcome to Threadly — microservices demo feed"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
    '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb,
    '11111111-1111-1111-1111-111111111101',
    '{"id":"cccccccc-cccc-cccc-cccc-cccccccccc01","authorUsername":"alice","contentPreview":"Great starter post"}'::jsonb,
    'cccccccc-cccc-cccc-cccc-cccccccccc01',
    'Thanks Alice — next up we will add more sample media on posts.',
    5, 'sql-seed', 'sql-seed', NOW() - INTERVAL '38 hours', NOW() - INTERVAL '38 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc04',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01","title":"Welcome to Threadly — microservices demo feed"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
    '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb,
    '11111111-1111-1111-1111-111111111105',
    NULL, NULL,
    'Solid portfolio piece. Bookmarks for interview prep.',
    6, 'sql-seed', 'sql-seed', NOW() - INTERVAL '30 hours', NOW() - INTERVAL '30 hours'
);

-- Post 02: 5 comments
INSERT INTO comments (id, post, post_id, author, author_id, parent, parent_id, content, score, created_by, updated_by, created_at, updated_at) VALUES
(
    'cccccccc-cccc-cccc-cccc-cccccccccc11',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02","title":"Spring Boot + Next.js stack walkthrough"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02',
    '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb,
    '11111111-1111-1111-1111-111111111101',
    NULL, NULL,
    'How are service-to-service calls authenticated internally?',
    15, 'sql-seed', 'sql-seed', NOW() - INTERVAL '34 hours', NOW() - INTERVAL '34 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc12',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02","title":"Spring Boot + Next.js stack walkthrough"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02',
    '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
    '11111111-1111-1111-1111-111111111102',
    '{"id":"cccccccc-cccc-cccc-cccc-cccccccccc11","authorUsername":"demo","contentPreview":"How are service-to-service"}'::jsonb,
    'cccccccc-cccc-cccc-cccc-cccccccccc11',
    'Internal API key + user header on gateway-proxied traffic. Keep keys in compose env.',
    11, 'sql-seed', 'sql-seed', NOW() - INTERVAL '33 hours', NOW() - INTERVAL '33 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc13',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02","title":"Spring Boot + Next.js stack walkthrough"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02',
    '{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}'::jsonb,
    '11111111-1111-1111-1111-111111111104',
    NULL, NULL,
    'Frontend split feels clean. Any Storybook for the UI kit?',
    4, 'sql-seed', 'sql-seed', NOW() - INTERVAL '28 hours', NOW() - INTERVAL '28 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc14',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02","title":"Spring Boot + Next.js stack walkthrough"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02',
    '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb,
    '11111111-1111-1111-1111-111111111103',
    NULL, NULL,
    'Gateway routes map 1:1 with service prefixes — easy to reason about.',
    7, 'sql-seed', 'sql-seed', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc15',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02","title":"Spring Boot + Next.js stack walkthrough"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02',
    '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb,
    '11111111-1111-1111-1111-111111111105',
    NULL, NULL,
    'Swagger on 8081–8088 helped me explore endpoints without guessing.',
    3, 'sql-seed', 'sql-seed', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours'
);

-- Post 03: 3 comments
INSERT INTO comments (id, post, post_id, author, author_id, parent, parent_id, content, score, created_by, updated_by, created_at, updated_at) VALUES
(
    'cccccccc-cccc-cccc-cccc-cccccccccc21',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03","title":"Calm UI without the default AI look"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03',
    '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb,
    '11111111-1111-1111-1111-111111111103',
    NULL, NULL,
    'The hover state on post cards is a nice affordance — more clickable now.',
    9, 'sql-seed', 'sql-seed', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc22',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03","title":"Calm UI without the default AI look"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03',
    '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb,
    '11111111-1111-1111-1111-111111111101',
    NULL, NULL,
    'Dropping the u/ and c/ prefixes makes names feel less noisy.',
    6, 'sql-seed', 'sql-seed', NOW() - INTERVAL '18 hours', NOW() - INTERVAL '18 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc23',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03","title":"Calm UI without the default AI look"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03',
    '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb,
    '11111111-1111-1111-1111-111111111105',
    '{"id":"cccccccc-cccc-cccc-cccc-cccccccccc21","authorUsername":"bob","contentPreview":"The hover state"}'::jsonb,
    'cccccccc-cccc-cccc-cccc-cccccccccc21',
    'Agreed — looks closer to a product people would actually ship.',
    4, 'sql-seed', 'sql-seed', NOW() - INTERVAL '16 hours', NOW() - INTERVAL '16 hours'
);

-- Post 04 (career): 6 comments incl replies
INSERT INTO comments (id, post, post_id, author, author_id, parent, parent_id, content, score, created_by, updated_by, created_at, updated_at) VALUES
(
    'cccccccc-cccc-cccc-cccc-cccccccccc31',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04","title":"How I explain this project in interviews"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04',
    '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb,
    '11111111-1111-1111-1111-111111111105',
    NULL, NULL,
    'This structure is gold. I always open with the problem, not the stack.',
    18, 'sql-seed', 'sql-seed', NOW() - INTERVAL '80 minutes', NOW() - INTERVAL '80 minutes'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc32',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04","title":"How I explain this project in interviews"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04',
    '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb,
    '11111111-1111-1111-1111-111111111103',
    NULL, NULL,
    'Do interviewers dig into Flyway multi-DB much, or just high-level architecture?',
    7, 'sql-seed', 'sql-seed', NOW() - INTERVAL '70 minutes', NOW() - INTERVAL '70 minutes'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc33',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04","title":"How I explain this project in interviews"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04',
    '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
    '11111111-1111-1111-1111-111111111102',
    '{"id":"cccccccc-cccc-cccc-cccc-cccccccccc32","authorUsername":"bob","contentPreview":"Do interviewers dig"}'::jsonb,
    'cccccccc-cccc-cccc-cccc-cccccccccc32',
    'Usually high-level; have one concrete migration story ready if they ask “how do schemas evolve?”',
    9, 'sql-seed', 'sql-seed', NOW() - INTERVAL '65 minutes', NOW() - INTERVAL '65 minutes'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc34',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04","title":"How I explain this project in interviews"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04',
    '{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}'::jsonb,
    '11111111-1111-1111-1111-111111111104',
    NULL, NULL,
    'I would also mention accessibility briefly — keyboard open on post cards helps.',
    5, 'sql-seed', 'sql-seed', NOW() - INTERVAL '50 minutes', NOW() - INTERVAL '50 minutes'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc35',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04","title":"How I explain this project in interviews"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04',
    '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb,
    '11111111-1111-1111-1111-111111111101',
    NULL, NULL,
    'Saved. The four-point talk track is easy to memorize under stress.',
    4, 'sql-seed', 'sql-seed', NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '40 minutes'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc36',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04","title":"How I explain this project in interviews"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04',
    '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb,
    '11111111-1111-1111-1111-111111111103',
    '{"id":"cccccccc-cccc-cccc-cccc-cccccccccc31","authorUsername":"dave","contentPreview":"This structure is gold"}'::jsonb,
    'cccccccc-cccc-cccc-cccc-cccccccccc31',
    'Same — problem → architecture → trade-offs beats a laundry list of frameworks.',
    3, 'sql-seed', 'sql-seed', NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes'
);

-- Post 05: 2 comments
INSERT INTO comments (id, post, post_id, author, author_id, parent, parent_id, content, score, created_by, updated_by, created_at, updated_at) VALUES
(
    'cccccccc-cccc-cccc-cccc-cccccccccc41',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05","title":"Composer form vs classic submit page"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05',
    '{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}'::jsonb,
    '11111111-1111-1111-1111-111111111104',
    NULL, NULL,
    'Progressive disclosure on schedule/media is the right call.',
    6, 'sql-seed', 'sql-seed', NOW() - INTERVAL '7 hours', NOW() - INTERVAL '7 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc42',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05","title":"Composer form vs classic submit page"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05',
    '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
    '11111111-1111-1111-1111-111111111102',
    NULL, NULL,
    'Multi-link posts are underrated for portfolio write-ups.',
    2, 'sql-seed', 'sql-seed', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours'
);

-- Post 06: 3 comments
INSERT INTO comments (id, post, post_id, author, author_id, parent, parent_id, content, score, created_by, updated_by, created_at, updated_at) VALUES
(
    'cccccccc-cccc-cccc-cccc-cccccccccc51',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06","title":"Docker Compose layout that stays debuggable"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06',
    '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb,
    '11111111-1111-1111-1111-111111111101',
    NULL, NULL,
    'host port 5433 tip saved me from fighting Windows Postgres.',
    10, 'sql-seed', 'sql-seed', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc52',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06","title":"Docker Compose layout that stays debuggable"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06',
    '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb,
    '11111111-1111-1111-1111-111111111105',
    NULL, NULL,
    'Per-service rebuilds cut my feedback loop a lot.',
    5, 'sql-seed', 'sql-seed', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc53',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06","title":"Docker Compose layout that stays debuggable"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06',
    '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
    '11111111-1111-1111-1111-111111111102',
    '{"id":"cccccccc-cccc-cccc-cccc-cccccccccc51","authorUsername":"demo","contentPreview":"host port 5433"}'::jsonb,
    'cccccccc-cccc-cccc-cccc-cccccccccc51',
    'Same. Document that early in README and people stop filing Postgres bugs.',
    4, 'sql-seed', 'sql-seed', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'
);

\echo '>>> Demo seed complete'
\echo 'Login any of: demo, alice, bob, carol, dave  password: password123'
