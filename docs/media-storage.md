# Where posts and files are stored

## Short answer

| What user creates | Where it lives | Database |
|---|---|---|
| Post title, body, link, type, score | **post-service** | Postgres `post_db.posts` |
| Uploaded image / video / zip / pdf / other | **media-service** disk volume | Metadata only in Postgres `media_db.media_files` |
| Comments | **comment-service** | `comment_db` |
| Votes | **vote-service** | `vote_db` |

**Binaries are never stored in Postgres.** Only metadata (name, MIME type, size, kind, uploader) goes in `media_db`. The file bytes are on disk (Docker volume `media_data` → `/app/uploads`).

```
Browser
  │
  ├─ POST /api/v1/media/uploads  ──► media-service ──► disk + media_files row ──► { id, url }
  │
  └─ POST /api/v1/posts { mediaId } ──► post-service ──► posts.media_id = that id
```

## Flow when creating a post with an attachment

1. User picks a file on **Create post**.
2. Frontend calls `POST /api/v1/media/uploads` with JWT + `multipart/form-data`.
3. **media-service**:
   - Validates type (blocks executables/scripts)
   - Enforces size caps by kind
   - Writes file under UUID name on the volume
   - Inserts metadata row → returns `{ id, kind, url, sizeBytes, … }`
4. Frontend creates the post with `mediaId` + `postType` (`IMAGE` or `FILE`).
5. **post-service** stores text fields in `posts` and only the **integer** `media_id` (pointer).
6. Readers open `GET /api/v1/media/{id}/content` (through the gateway) to stream the file.

Text-only posts skip steps 2–3; `media_id` is null.

## Free stack (no paid SaaS required)

| Layer | Choice | Why |
|---|---|---|
| Metadata DB | Existing Postgres + `media_db` | Already free in Docker; no extra product |
| Bytes | Docker named volume / local disk | Zero cost on a VPS free tier |
| Optional upgrade | **MinIO** in Compose, or **Cloudflare R2** free tier | Same `ObjectStorage` port; swap adapter later |
| Avoid | Storing files as BYTEA in Postgres | Bloats DB, slow backups, expensive IOPS |

You do **not** need another database for files. One more table column family (`media_kind`) is enough.

## Size caps (defaults)

| Kind | Max size |
|---|---|
| IMAGE | 5 MB |
| VIDEO | 50 MB |
| AUDIO | 15 MB |
| DOCUMENT / ARCHIVE (pdf, zip, office) | 20 MB |
| OTHER | 10 MB |
| Absolute ceiling (any file) | 50 MB |

Spring multipart limit is also 50 MB (`MEDIA_MAX_MULTIPART`). Override with env:

```env
MEDIA_MAX_FILE_BYTES=52428800
MEDIA_MAX_IMAGE_BYTES=5242880
MEDIA_MAX_VIDEO_BYTES=52428800
MEDIA_MAX_DOCUMENT_BYTES=20971520
MEDIA_MAX_OTHER_BYTES=10485760
MEDIA_MAX_MULTIPART=50MB
```

## Security policy

Allowed: images, common video/audio, zip/7z/rar/tar, pdf, office docs, plain text, etc.

Blocked intentionally (not “any random executable”):

- `.exe`, `.bat`, `.sh`, `.jar`, `.php`, `.js`, installers, etc.

That is safer for a public blog than literal “upload anything”.

## Best practice (industry + free)

1. **Keep the port** `ObjectStorage` — disk now, MinIO/R2 later without rewriting controllers.
2. **Metadata in SQL, blobs out of SQL** — already done.
3. **Size caps + auth** on upload — JWT required.
4. **Do not serve untrusted HTML** as media without `Content-Disposition: attachment` — docs/zip use attachment; images/video/audio use inline.
5. Later free upgrades: antivirus scan sidecar, CDN in front of media URLs, soft-delete + lifecycle jobs if disk fills.

## Related APIs

| Method | Path | Role |
|---|---|---|
| POST | `/api/v1/media/uploads` | Upload file |
| GET | `/api/v1/media/{id}` | Metadata |
| GET | `/api/v1/media/{id}/content` | Stream bytes |
| POST | `/api/v1/posts` | Create post; optional `mediaId` |
