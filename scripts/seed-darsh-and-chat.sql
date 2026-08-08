-- Darsh posts + community memberships + sample community chat
-- darsh user id (from auth_db): 1bca764e-65a5-4dde-b79d-a8fe3a7f692c
-- Run:
--   Get-Content -Raw scripts\seed-darsh-and-chat.sql | docker exec -i threadly-postgres psql -U threadly -v ON_ERROR_STOP=1

\echo '>>> Ensure darsh memberships'
\c community_db

INSERT INTO community_memberships (id, community, community_id, member, user_id, joined_at)
SELECT gen_random_uuid(),
       jsonb_build_object('id', c.id::text, 'name', c.name, 'slug', c.slug),
       c.id,
       '{"id":"1bca764e-65a5-4dde-b79d-a8fe3a7f692c","username":"darsh"}'::jsonb,
       '1bca764e-65a5-4dde-b79d-a8fe3a7f692c'::uuid,
       NOW() - INTERVAL '2 days'
FROM communities c
WHERE c.id IN (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4'
)
AND NOT EXISTS (
    SELECT 1 FROM community_memberships m
    WHERE m.community_id = c.id AND m.user_id = '1bca764e-65a5-4dde-b79d-a8fe3a7f692c'
);

UPDATE communities c
SET member_count = (
    SELECT COUNT(*) FROM community_memberships m WHERE m.community_id = c.id
);

-- Chat messages (table created by community-service Flyway V6 on restart)
CREATE TABLE IF NOT EXISTS community_messages (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id  UUID         NOT NULL REFERENCES communities (id) ON DELETE CASCADE,
    community     JSONB        NOT NULL,
    author_id     UUID         NOT NULL,
    author        JSONB        NOT NULL,
    body          VARCHAR(2000) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_community_messages_community_created
    ON community_messages (community_id, created_at DESC);

DELETE FROM community_messages WHERE id IN (
  'dddddddd-dddd-dddd-dddd-dddddddddd01',
  'dddddddd-dddd-dddd-dddd-dddddddddd02',
  'dddddddd-dddd-dddd-dddd-dddddddddd03',
  'dddddddd-dddd-dddd-dddd-dddddddddd04',
  'dddddddd-dddd-dddd-dddd-dddddddddd05',
  'dddddddd-dddd-dddd-dddd-dddddddddd06'
);

INSERT INTO community_messages (id, community_id, community, author_id, author, body, created_at) VALUES
(
  'dddddddd-dddd-dddd-dddd-dddddddddd01',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
  '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb,
  '11111111-1111-1111-1111-111111111101',
  '{"id":"11111111-1111-1111-1111-111111111101","username":"demo"}'::jsonb,
  'Anyone pairing on gateway routing tonight?',
  NOW() - INTERVAL '3 hours'
),
(
  'dddddddd-dddd-dddd-dddd-dddddddddd02',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
  '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb,
  '1bca764e-65a5-4dde-b79d-a8fe3a7f692c',
  '{"id":"1bca764e-65a5-4dde-b79d-a8fe3a7f692c","username":"darsh"}'::jsonb,
  'I am — looking at PublicRoutes for guest read access.',
  NOW() - INTERVAL '2 hours 40 minutes'
),
(
  'dddddddd-dddd-dddd-dddd-dddddddddd03',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
  '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb,
  '11111111-1111-1111-1111-111111111102',
  '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
  'Solid. Guest post detail is a huge UX win.',
  NOW() - INTERVAL '2 hours'
),
(
  'dddddddd-dddd-dddd-dddd-dddddddddd04',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
  '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3","name":"Career Growth","slug":"career-growth"}'::jsonb,
  '11111111-1111-1111-1111-111111111105',
  '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb,
  'Mock interviews this weekend — drop your Stack talk outline here.',
  NOW() - INTERVAL '5 hours'
),
(
  'dddddddd-dddd-dddd-dddd-dddddddddd05',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
  '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3","name":"Career Growth","slug":"career-growth"}'::jsonb,
  '1bca764e-65a5-4dde-b79d-a8fe3a7f692c',
  '{"id":"1bca764e-65a5-4dde-b79d-a8fe3a7f692c","username":"darsh"}'::jsonb,
  'Mine starts with user problems → microservices map → demos.',
  NOW() - INTERVAL '4 hours'
),
(
  'dddddddd-dddd-dddd-dddd-dddddddddd06',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
  '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2","name":"Design Systems","slug":"design-systems"}'::jsonb,
  '11111111-1111-1111-1111-111111111104',
  '{"id":"11111111-1111-1111-1111-111111111104","username":"carol"}'::jsonb,
  'Love the new clickable community cards. Feels much less empty.',
  NOW() - INTERVAL '1 hour'
);

\echo '>>> Posts by darsh'
\c post_db

DELETE FROM posts WHERE id IN (
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07',
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08',
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09'
);

INSERT INTO posts (
    id, community, community_id, author, author_id,
    title, content, post_type, link_urls, media,
    score, comment_count, created_by, updated_by, created_at, updated_at,
    scheduled_at, status
) VALUES
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07',
    '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1","name":"Programming","slug":"programming"}'::jsonb,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
    '{"id":"1bca764e-65a5-4dde-b79d-a8fe3a7f692c","username":"darsh"}'::jsonb,
    '1bca764e-65a5-4dde-b79d-a8fe3a7f692c',
    'What I built for Threadly first (and why)',
    E'I started with auth + communities + feed because those three unlock a real user loop.\n\nNext: comments that feel like YouTube (inline replies), then community chat so spaces do not feel empty.\n\nIf you are cloning this as a portfolio project, ship readable public pages before you add more admin features.',
    'TEXT', '[]'::jsonb, '[]'::jsonb,
    18, 2, 'darsh', 'darsh', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours',
    NULL, 'PUBLISHED'
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08',
    '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3","name":"Career Growth","slug":"career-growth"}'::jsonb,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
    '{"id":"1bca764e-65a5-4dde-b79d-a8fe3a7f692c","username":"darsh"}'::jsonb,
    '1bca764e-65a5-4dde-b79d-a8fe3a7f692c',
    'Portfolio checklist before an interview',
    E'1. One memorable home page\n2. Working login + seed data\n3. Public post detail (no awkward empty gate)\n4. A feature that surprises (chat / scheduling / media)\n5. Short architecture story you can whiteboard\n\nThreadly is my version of that checklist.',
    'TEXT', '[]'::jsonb, '[]'::jsonb,
    27, 1, 'darsh', 'darsh', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour',
    NULL, 'PUBLISHED'
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09',
    '{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4","name":"DevOps & Cloud","slug":"devops-cloud"}'::jsonb,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4',
    '{"id":"1bca764e-65a5-4dde-b79d-a8fe3a7f692c","username":"darsh"}'::jsonb,
    '1bca764e-65a5-4dde-b79d-a8fe3a7f692c',
    'When Docker says “too many clients already”',
    E'Postgres max connections get eaten by many microservices each holding Hikari pools.\n\nFixes I use: restart Postgres, reduce pool size in non-prod, and avoid blasting free psql sessions from scripts while apps are under load.',
    'TEXT', '[]'::jsonb, '[]'::jsonb,
    14, 1, 'darsh', 'darsh', NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '40 minutes',
    NULL, 'PUBLISHED'
);

\echo '>>> Comments on darsh posts'
\c comment_db

DELETE FROM comments WHERE post_id IN (
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07',
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08',
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09'
);

INSERT INTO comments (id, post, post_id, author, author_id, parent, parent_id, content, score, created_by, updated_by, created_at, updated_at) VALUES
(
    'cccccccc-cccc-cccc-cccc-cccccccccc71',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07","title":"What I built for Threadly first (and why)"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07',
    '{"id":"11111111-1111-1111-1111-111111111102","username":"alice"}'::jsonb,
    '11111111-1111-1111-1111-111111111102',
    NULL, NULL,
    'This prioritization is exactly how I coach mentees. Ship the loop first.',
    8, 'sql-seed', 'sql-seed', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc72',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07","title":"What I built for Threadly first (and why)"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07',
    '{"id":"1bca764e-65a5-4dde-b79d-a8fe3a7f692c","username":"darsh"}'::jsonb,
    '1bca764e-65a5-4dde-b79d-a8fe3a7f692c',
    '{"id":"cccccccc-cccc-cccc-cccc-cccccccccc71","authorUsername":"alice","contentPreview":"This prioritization"}'::jsonb,
    'cccccccc-cccc-cccc-cccc-cccccccccc71',
    'Thanks — public guest reading was the piece that made demos less awkward.',
    4, 'sql-seed', 'sql-seed', NOW() - INTERVAL '90 minutes', NOW() - INTERVAL '90 minutes'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc81',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08","title":"Portfolio checklist before an interview"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08',
    '{"id":"11111111-1111-1111-1111-111111111105","username":"dave"}'::jsonb,
    '11111111-1111-1111-1111-111111111105',
    NULL, NULL,
    'Saving this list. Item 4 is underrated.',
    6, 'sql-seed', 'sql-seed', NOW() - INTERVAL '45 minutes', NOW() - INTERVAL '45 minutes'
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccc91',
    '{"id":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09","title":"When Docker says “too many clients already”"}'::jsonb,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09',
    '{"id":"11111111-1111-1111-1111-111111111103","username":"bob"}'::jsonb,
    '11111111-1111-1111-1111-111111111103',
    NULL, NULL,
    'Hit this last week. Restart + smaller pools fixed it.',
    5, 'sql-seed', 'sql-seed', NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '20 minutes'
);

\echo '>>> darsh seed complete'
