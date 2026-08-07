-- Expand roles for RBAC: moderator sits between USER and ADMIN.
ALTER TABLE roles ADD COLUMN IF NOT EXISTS description VARCHAR(255);

INSERT INTO roles (name, description)
SELECT 'ROLE_MODERATOR', 'Moderates content: delete any post or comment'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_MODERATOR');

UPDATE roles SET description = 'Default member: create content, vote, manage own posts/comments'
WHERE name = 'ROLE_USER';

UPDATE roles SET description = 'Platform administrator: manage users, assign roles, full content control'
WHERE name = 'ROLE_ADMIN';

UPDATE roles SET description = 'Moderates content: delete any post or comment'
WHERE name = 'ROLE_MODERATOR';
