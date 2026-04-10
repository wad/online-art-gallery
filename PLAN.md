# Implementation Plan — Online Art Gallery

This plan breaks the project into phases, each producing a buildable/testable milestone. Phases are ordered by dependency: infrastructure first, then data layer, then admin API & UI, then public/visitor layer, then deployment.

---

## Phase 1: Project Skeleton & Build Infrastructure

Set up the Maven multi-module project structure so that `mvn package` produces a single JAR.

- [x] **1.1** Create parent `pom.xml` (multi-module: `backend`, `frontend`)
- [x] **1.2** Create `backend/pom.xml` with Spring Boot 3, Java 21, springdoc-openapi, JJWT, MapStruct, Liquibase, PostgreSQL driver, H2 (test), JaCoCo, Bucket4j (rate limiting), Caffeine (caching)
- [x] **1.3** Create `frontend/pom.xml` with `frontend-maven-plugin` (install Node/npm, run `npm install`, run `npm run build`), copy build output to `backend/src/main/resources/static`
- [x] **1.4** Scaffold React + TypeScript + Vite app in `frontend/`
- [x] **1.4a** Bundle sample artwork: ~6-8 public domain paintings (optimized/resized) in `frontend/src/assets/sample-artwork/`. Source from Met Open Access or similar. Used for admin theme preview and e2e test fixtures.
- [x] **1.5** Create `backend/src/main/java/org/wadhome/oag/OagApplication.java` (Spring Boot main class)
- [x] **1.6** Create `application.yml` with datasource config using env vars, JVM settings, `oag.site-domain` (from `OAG_SITE_DOMAIN`), `oag.adsense.publisher-id` (from `OAG_ADSENSE_PUBLISHER_ID` env var, optional)
- [x] **1.7** Create `docker-compose.yml` (PostgreSQL 16 container + app container, `-Xmx512m`)
- [x] **1.8** Create `.gitignore` for Java/Maven/Node/React artifacts
- [x] **1.9** Verify: `mvn clean package` succeeds, app starts, serves Vite placeholder page

**Milestone**: Clean build producing a runnable JAR that serves a blank React page.

---

## Phase 2: Database Schema (Liquibase)

Define all tables via Liquibase XML changesets. No application code yet — just the schema.

- [x] **2.1** Create Liquibase master changelog (`db/changelog/db.changelog-master.xml`)
- [x] **2.2** Changeset: `image` table — `id` (bigserial PK), `public_id` (UUID, unique, not null), `short_id` (text, unique, not null — randomly generated zero-padded 8-digit number, e.g. "00004719", immutable after creation), `url` (text, not null), `title` (text), `artist_name` (text), `description` (text), `art_creation_date` (text, nullable — freeform: "1975", "1972-1974", etc.), `artist_comments` (text, nullable), `notes` (text, nullable), `admin_notes` (text, nullable — visible only in admin panel, never exposed to visitors), `nsfw` (boolean, not null, default false), `uploaded_at` (timestamptz, not null, default `now()` — auto-set on creation, admin-only), `base_image_id` (bigint FK self-ref, nullable)
- [x] **2.3** Changeset: `tag` table — `id` (bigserial PK), `public_id` (UUID, unique), `name` (text, unique, not null)
- [x] **2.4** Changeset: `image_tag` join table — `image_id` (FK), `tag_id` (FK), composite PK
- [x] **2.5** Changeset: `gallery` table — `id` (bigserial PK), `public_id` (UUID, unique), `code` (text, unique, not null — 1-5 chars, regex `^[A-Z][A-Z0-9]{0,4}$`), `name` (text, not null), `subtitle` (text, nullable — e.g., artist name), `description` (text), `headliner_image_id` (bigint FK to image, nullable), `is_default` (boolean, default false), `visible` (boolean, not null, default true), `show_all_images_tour` (boolean, not null, default true), `sort_order` (int, not null, default 0), `bio_photo_url` (text, nullable — URL of artist photo), `bio_text` (text, nullable — biographical paragraphs, supports newlines), `theme` (text, not null, default `'LIGHT'`), `border_style` (text, nullable — null means use theme default)
- [x] **2.5a** Changeset: ad settings columns on `gallery` table — `ads_enabled` (boolean, not null, default false), `ad_landing_banner` (boolean, not null, default false), `ad_landing_banner_slot` (text, nullable), `ad_image_detail_sidebar` (boolean, not null, default false), `ad_image_detail_sidebar_slot` (text, nullable)
- [x] **2.5b** Changeset: `gallery_bio_link` table — `id` (bigserial PK), `gallery_id` (bigint FK to gallery, not null), `label` (text, not null), `url` (text, not null), `sort_order` (int, not null)
- [x] **2.5c** Changeset: `gallery_image` join table — `gallery_id` (FK to gallery), `image_id` (FK to image), composite PK. Scopes images to galleries — an image can belong to multiple galleries but tours only see images associated with their gallery.
- [x] **2.6** Changeset: `tour` table — `id` (bigserial PK), `public_id` (UUID, unique), `name` (text, not null), `description` (text, nullable), `gallery_id` (bigint FK to gallery, not null), `headliner_image_id` (bigint FK to image, nullable), `sort_order` (int, not null, default 0)
- [x] **2.7** Changeset: `tour_image` join table — `tour_id` (FK), `image_id` (FK), `sort_order` (int, not null), composite PK on (tour_id, image_id)
- [x] **2.7a** Changeset: `tour_tag` join table — `tour_id` (FK to tour), `tag_id` (FK to tag), composite PK. Records which tags a tour is associated with for auto-sync purposes.
- [x] **2.8** Changeset: `thumbnail` table — `image_id` (bigint FK to image, PK), `data` (bytea, not null — JPEG binary, max 100x100px preserving aspect ratio)
- [x] **2.8a** Changeset: `image_redirect` table — `old_short_id` (text, PK — the short ID of the deleted image), `new_short_id` (text, not null — the substitute short ID, may be `"00000000"` for "image not available")
- [x] **2.8b** Changeset: `page_view_daily` table — `id` (bigserial PK), `view_date` (date, not null), `entity_type` (text, not null — `'GALLERY'`, `'TOUR'`, or `'IMAGE'`), `entity_id` (bigint, not null — **no FK constraint**, just a plain bigint referencing the entity), `context` (text, not null — `'DIRECT'` or `'TOUR'`), `view_count` (int, not null, default 0). Unique constraint on `(view_date, entity_type, entity_id, context)`. Index on `(entity_type, entity_id, view_date)` for date-range queries. **No ON DELETE cascade** — when an entity is deleted, its historical view stats rows are kept as orphaned data. Stats queries must LEFT JOIN against the entity tables and filter out or label rows where the entity no longer exists.
- [x] **2.8c** Changeset: `async_job` table — `id` (UUID PK), `type` (text, not null — e.g. `'REGEN_ALL_THUMBNAILS'`), `status` (text, not null — `'RUNNING'`, `'COMPLETED'`, `'FAILED'`), `total_items` (int), `processed_items` (int, default 0), `failed_items` (int, default 0), `error_messages` (text, nullable — JSON array of per-item errors), `started_at` (timestamptz, not null), `completed_at` (timestamptz, nullable). Used for tracking async bulk operations.
- [x] **2.9** Changeset: `admin_config` table — `id` (bigserial PK), `password_hash` (text, not null) — single row for the admin password
- [x] **2.10** Add indexes: `image.public_id`, `image.short_id`, `gallery.public_id`, `gallery.code`, `tour.public_id`, `gallery.is_default`, `gallery.sort_order`, `tour.sort_order`
- [x] **2.11** Verify: app starts, Liquibase runs migrations against Dockerized PostgreSQL, schema matches design

**Milestone**: Database schema fully defined and versioned. App starts cleanly with empty tables.

---

## Phase 3: JPA Entities & Repositories

Map the schema to Java, establish the data access layer.

- [x] **3.1** `Image` entity with self-referential `@ManyToOne` for `baseImage`, immutable `shortId` field (randomly generated zero-padded 8-digit string via `@PrePersist` — generate random number 0–99999999, zero-pad, retry on collision), `adminNotes` field (String, nullable), `nsfw` field (boolean, default false), `uploadedAt` (Instant, auto-set via `@CreationTimestamp`, admin-only), `url` (String, mutable — can be updated by admin)
- [x] **3.2** `Tag` entity
- [x] **3.3** `Image` ↔ `Tag` many-to-many mapping
- [x] **3.4** `Gallery` entity with `code` (unique, validated: 1-5 chars, `^[A-Z][A-Z0-9]{0,4}$`), `subtitle` (String, nullable), `visible` (boolean), `showAllImagesTour` (boolean), `sortOrder` (int), `bioPhotoUrl` (String, nullable), `bioText` (String, nullable), `@OneToMany` to `GalleryBioLink` (ordered list), `@ManyToOne` to `Image` (headliner), `@Enumerated(STRING)` for `theme` field
- [x] **3.4a** `GalleryTheme` enum: `LIGHT`, `DARK`, `PASTEL`, `SPRING`, `WINTER`, `CYBERPUNK`, `SUNSET`, `OCEAN`, `MONOCHROME`
- [x] **3.4b** `BorderStyle` enum: `THEME_DEFAULT`, `NONE`, `THIN_LINE`, `DOUBLE_LINE`, `SHADOW`, `ROUNDED`, `ORNATE_FRAME`, `POLAROID` — nullable on Gallery entity (null = THEME_DEFAULT)
- [x] **3.4c** Ad settings fields on `Gallery` entity: `adsEnabled` (boolean), `adLandingBanner` (boolean), `adLandingBannerSlot` (String, nullable), `adImageDetailSidebar` (boolean), `adImageDetailSidebarSlot` (String, nullable)
- [x] **3.4d** `GalleryBioLink` entity — `id`, `gallery` (`@ManyToOne`), `label` (String), `url` (String), `sortOrder` (int)
- [x] **3.4e** `GalleryImage` — `Gallery` ↔ `Image` many-to-many via `gallery_image` table. Scopes which images belong to which galleries.
- [x] **3.5** `Tour` entity with `@ManyToOne` to `Gallery`, `@ManyToOne` to `Image` (headliner), `description` (String, nullable), `sortOrder` (int), `@ManyToMany` to `Tag` (via `tour_tag` join table — tags associated with this tour for auto-sync)
- [x] **3.5a** `TourTag` mapping: `Tour` ↔ `Tag` many-to-many via `tour_tag` table. When tags are associated with a tour, any image with a matching tag is automatically included in the tour.
- [x] **3.6** `TourImage` entity (or `@OrderColumn` on Tour's image list) capturing sort order
- [x] **3.7** `Thumbnail` entity — PK is `imageId`, `data` (byte[], `@Lob`)
- [x] **3.7a** `ImageRedirect` entity — `oldShortId` (String, PK), `newShortId` (String, not null)
- [x] **3.7b** `PageViewDaily` entity — `id` (Long), `viewDate` (LocalDate), `entityType` (String/enum), `entityId` (Long), `context` (String/enum: `DIRECT`, `TOUR`), `viewCount` (int). Repository with UPSERT-style method: increment `viewCount` by 1 for a given `(viewDate, entityType, entityId, context)`, inserting a new row if none exists.
- [x] **3.7c** `AsyncJob` entity — `id` (UUID), `type` (String), `status` (enum: `RUNNING`, `COMPLETED`, `FAILED`), `totalItems` (Integer), `processedItems` (int), `failedItems` (int), `errorMessages` (String, nullable — JSON), `startedAt` (Instant), `completedAt` (Instant, nullable).
- [x] **3.8** `AdminConfig` entity
- [x] **3.9** Spring Data JPA repositories for each entity. `ImageRepository` includes: paginated `findAll` with sorting, text search across title/artistName/description, filtering by tag and NSFW flag, `findOrphans()` — images with no `gallery_image` rows.
- [x] **3.10** Verify: unit tests with H2 (`MODE=PostgreSQL`) — CRUD operations on all entities, relationship integrity, gallery-image scoping, orphan detection query

**Milestone**: Full data access layer with passing tests.

---

## Phase 4: DTOs & MapStruct Mappers

Define the API contract (request/response shapes) and compile-time mapping.

- [x] **4.1** DTOs for Image: `ImageResponse` (all fields including shortId, artCreationDate, artistComments, notes, adminNotes, nsfw, uploadedAt, url, `thumbnailUrl` — always `/api/v1/public/thumbnails/{shortId}`, galleryPublicIds list), `PublicImageResponse` (same but WITHOUT adminNotes, uploadedAt, galleryPublicIds — used by public API; includes `nsfw` flag so frontend can handle display), `ImageCreateRequest` (url, optional `galleryPublicId` — if provided, image is immediately associated with that gallery), `ImageUpdateRequest` (url, title, artistName, description, artCreationDate, artistComments, notes, adminNotes, nsfw, baseImagePublicId — changing `url` triggers thumbnail regeneration), `ImageBulkLoadRequest` (list of URLs, required `galleryPublicId`), `ImageBulkLoadResponse` (list of results per URL: success with imagePublicId, or failure with error message; `aborted` boolean + reason if first-3 rule triggered), `ImageUsageResponse` (list of `{ galleryPublicId, galleryName, galleryCode, tours: [{ tourPublicId, tourName }] }`)
- [x] **4.1a** `BulkMetadataUpdateRequest` — list of `{ imagePublicId, title?, artistName?, description?, artCreationDate?, artistComments?, notes?, adminNotes?, nsfw? }` for batch updates. `BulkMetadataUpdateResponse` — list of per-image `{ imagePublicId, success, error? }`.
- [x] **4.2** DTOs for Tag: `TagResponse` (includes publicId, name, imageCount), `TagCreateRequest` (name)
- [x] **4.3** DTOs for Gallery: `GalleryResponse` (includes `code`, `subtitle`, `visible`, `showAllImagesTour`, `sortOrder`, `bioPhotoUrl`, `bioText`, `bioLinks` list, `theme`, `borderStyle`, ad settings), `GalleryCreateRequest` (requires `code`, includes optional `subtitle`, `visible` defaults to true, `showAllImagesTour` defaults to true, optional `sortOrder`, optional `bioPhotoUrl`, `bioText`, `bioLinks`, optional `theme` defaults to LIGHT, optional `borderStyle`, optional ad settings all defaulting to false), `GalleryUpdateRequest` (includes optional `code`, `subtitle`, `visible`, `showAllImagesTour`, `sortOrder`, `bioPhotoUrl`, `bioText`, `bioLinks`, `theme`, `borderStyle`, ad settings)
- [x] **4.3a** `GET /api/v1/public/themes` response DTO: list of available themes with display names
- [x] **4.3b** `BioLinkDto` — `label` (String), `url` (String)
- [x] **4.3c** Gallery export/import DTOs. `GalleryExportDto`:
  ```json
  {
    "formatVersion": 1,
    "gallery": {
      "name": "Frank Frazetta",
      "subtitle": "Master of Fantasy Art",
      "description": "...",
      "theme": "DARK",
      "borderStyle": "ORNATE_FRAME",
      "visible": true,
      "showAllImagesTour": true,
      "adsEnabled": false,
      "adLandingBanner": false,
      "adLandingBannerSlot": null,
      "adImageDetailSidebar": false,
      "adImageDetailSidebarSlot": null,
      "bioPhotoUrl": "https://...",
      "bioText": "...",
      "bioLinks": [
        { "label": "Website", "url": "https://..." }
      ],
      "headlinerImageShortId": "00004719",
      "imageShortIds": ["00004719", "83210042", "00138572"]
    },
    "tours": [
      {
        "name": "Greatest Hits",
        "description": "The most iconic paintings",
        "sortOrder": 0,
        "headlinerImageShortId": "83210042",
        "imageShortIds": ["83210042", "00004719", "00138572"],
        "tagNames": ["fantasy", "oil"]
      }
    ]
  }
  ```
  `GalleryImportRequest`: the export DTO above + a required `code` field (the new gallery code). `GalleryImportResponse`: the created gallery response + `warnings` list (e.g., `"Image 00138572 not found — skipped"`).
- [x] **4.4** DTOs for Tour: `TourResponse` (includes description, sortOrder, list of associated tag public IDs), `TourCreateRequest` (name, description, headlinerImagePublicId, ordered imagePublicIds list, optional sortOrder, optional list of tag public IDs — images matching those tags are auto-added), `TourUpdateRequest` (name, description, headlinerImagePublicId, imagePublicIds list with order, sortOrder, tag public IDs)
- [x] **4.4a** DTOs for View Stats: `ViewTrackingRequest` (`type`: GALLERY/TOUR/IMAGE, `code`, optional `tourPublicId`, optional `imageShortId`, optional `context`: DIRECT/TOUR), `ViewStatsResponse` (entity name/code/shortId, total views, direct views, tour views, daily breakdown list), `DailyViewCount` (date, count)
- [x] **4.5** DTOs for Auth: `LoginRequest` (password), `LoginResponse` (token), `PasswordChangeRequest` (currentPassword, newPassword — newPassword must be >= 8 characters)
- [x] **4.5a** `AsyncJobResponse` — `jobId` (UUID), `type`, `status`, `totalItems`, `processedItems`, `failedItems`, `errorMessages` (list of strings), `startedAt`, `completedAt`
- [x] **4.6** MapStruct mappers: Entity ↔ DTO, mapping `publicId` (not `id`), resolving FK references by public_id
- [x] **4.7** Verify: mapper unit tests — round-trip mapping correctness

**Milestone**: Clean API contract defined. Mappers compile and pass tests.

---

## Phase 5: Authentication & Security

- [x] **5.1** JWT utility class: generate token (24-hour expiration), validate token, extract claims (using JJWT). No refresh tokens — when the token expires, the admin re-authenticates. The frontend detects 401 responses and redirects to the login page.
- [x] **5.2** `AuthService`: verify password against bcrypt hash in `admin_config` table, change password (verify current, validate new >= 8 chars, store new bcrypt hash)
- [x] **5.3** `AuthController`:
  - `POST /api/v1/auth/login` — accepts password, returns JWT
  - `PUT /api/v1/admin/password` — change admin password (requires current password + new password, minimum 8 characters)
- [x] **5.4** `JwtAuthenticationFilter` (extends `OncePerRequestFilter`): extract token from `Authorization: Bearer` header, validate, set SecurityContext
- [x] **5.5** `SecurityConfig`: permit all on public endpoints (`/api/v1/public/**`, `/`, static resources), require auth on `/api/v1/admin/**`
- [x] **5.6** Seed data: Liquibase changeset (or `CommandLineRunner`) to insert initial admin password hash (bcrypt of `WorstPassword666!`)
- [x] **5.7** Rate limiting: apply per-IP rate limit to `POST /api/v1/public/views` (e.g., 60 requests/minute per IP using Bucket4j or Spring filter). Prevents stat inflation from abuse.
- [x] **5.8** Verify: integration tests — login succeeds/fails, password change (success, wrong current password, too-short new password), protected endpoints reject without token, accept with valid token, rate limiting on views endpoint

**Milestone**: Auth fully working. Admin endpoints are protected.

---

## Phase 6: Admin API — Images, Tags, Thumbnails

- [x] **6.1** `ImageService`: create (with optional gallery assignment), update metadata (including URL — triggers thumbnail regen), delete, get by publicId, list all (paginated, searchable, filterable, sortable), list orphans (images not in any gallery), bulk load (requires gallery), set/clear base image, get image usage (which galleries and tours reference an image)
- [x] **6.1a** `ThumbnailService`: generate thumbnail from image URL (fetch image, scale to fit 100x100px — scale up if smaller, preserve aspect ratio, encode as JPEG, store in `thumbnail` table). Called when an image is created, when its URL is updated, or when admin triggers manual regeneration. Uses a library like `java.awt.image` / Thumbnailator for resizing. Thumbnail is deleted when the image is deleted. **Error handling**: if the URL is unreachable or returns a non-image response, throw a descriptive exception (include HTTP status or connection error message). The caller decides how to handle it (see bulk load logic and regen-all logic).
- [x] **6.1b** Thumbnail in-memory cache: Caffeine cache keyed by image short ID, bounded size appropriate for 512MB heap (e.g., ~50-100MB max, LRU eviction). Cache is populated on read, invalidated when an image is deleted or thumbnail is regenerated.
- [x] **6.2** `AdminImageController` (`/api/v1/admin/images`):
  - `POST /` — create single image (provide URL, optional `galleryPublicId`). If thumbnail generation fails, return 201 with a warning field (image created but thumbnail missing) and the error message.
  - `POST /bulk` — bulk load (list of URLs, required `galleryPublicId`). Processes sequentially. Each image: attempt create + thumbnail + gallery association. On thumbnail failure: image is still created and associated with the gallery, but flagged with a warning in the response. **Abort rule**: if the first 3 consecutive images all fail thumbnail generation, abort the entire bulk load and return the errors (likely a systemic issue like bad base URL). Response: `ImageBulkLoadResponse` with per-URL results and `aborted` flag.
  - `GET /` — list all images, **paginated** (`page`, `size`, default 20). Supports query params: `q` (text search across title, artist name, description), `tag` (filter by tag publicId), `nsfw` (boolean filter), `orphan` (boolean filter — if true, only images not in any gallery), `sort` (field name: `title`, `artistName`, `uploadedAt`, default `uploadedAt`), `dir` (`asc`/`desc`, default `desc`). Each result includes a thumbnail URL for display in the admin list.
  - `GET /{publicId}` — get image detail
  - `GET /{publicId}/usage` — get image usage report: list of galleries and tours (with names and public IDs) that reference this image
  - `PUT /{publicId}` — update metadata. If `url` field is changed, triggers thumbnail regeneration (returns warning if regen fails — image URL is still updated).
  - `PUT /bulk-metadata` — bulk metadata update. Accepts `BulkMetadataUpdateRequest` (list of image updates). Each entry specifies an image publicId and the fields to update. Only non-null fields are applied. Returns `BulkMetadataUpdateResponse` with per-image success/failure.
  - `PUT /{publicId}/base-image` — set base image (body: `{ "baseImagePublicId": "..." }`)
  - `DELETE /{publicId}/base-image` — clear base image
  - `POST /{publicId}/regenerate-thumbnail` — regenerate thumbnail for a single image. Returns 200 with success or error message.
  - `POST /regenerate-all-thumbnails` — regenerate thumbnails for all images. Creates an `AsyncJob` row, kicks off processing on a background thread (`@Async` — requires `@EnableAsync` on a Spring config class), returns `202 Accepted` with `{ jobId }`. The background thread iterates all images, regenerates each thumbnail, updates `processedItems`/`failedItems`/`errorMessages` on the `AsyncJob` row as it goes, and sets `status` to `COMPLETED` or `FAILED` when done.
  - `GET /jobs/{jobId}` — poll async job status. Returns `AsyncJobResponse`. The admin UI polls this every few seconds while status is `RUNNING`.
  - `DELETE /{publicId}?substituteShortId=XXXXXXXX` — delete image. Requires `substituteShortId` parameter (the short ID of the replacement image, or `00000000` for "image not available"). Creates an `image_redirect` entry mapping the deleted image's short ID to the substitute. If the deleted image is itself a redirect target, all existing redirects pointing to it are updated to point to the new substitute (one-hop rule). Validates no loops. Also removes the image from any tours, any gallery-image associations, and clears any headliner references.
- [x] **6.3** Image-tag operations on `AdminImageController`:
  - `POST /{publicId}/tags` — add tag to image. **Auto-sync**: after adding, find all tours associated with this tag (within galleries that contain the image) and add the image to them (appended at the end of tour order).
  - `DELETE /{publicId}/tags/{tagPublicId}` — remove tag from image. **Auto-sync**: after removing, find all tours associated with this tag and remove the image from them (only if the image has no other tags that the tour is also associated with).
- [x] **6.3a** `AdminTagController` (`/api/v1/admin/tags`) — tags are a top-level resource, not scoped to images:
  - `GET /` — list all tags with image counts
  - `POST /` — create a new tag
  - `DELETE /{tagPublicId}` — delete a tag. Removes the tag from all images and all tour-tag associations. **Auto-sync**: for each tour that was associated with this tag, remove images that were only included via this tag (and have no other matching tour-tags).
- [x] **6.4** Validation: prevent circular base-image references, prevent redirect loops on deletion, validate substitute short ID exists (or is `00000000`)
- [x] **6.5** Verify: integration tests covering all endpoints, edge cases, error responses, pagination, search/filter, orphan filter, bulk load with gallery assignment, bulk load abort logic, thumbnail regeneration (single and all with async job polling), image URL update, image usage report, bulk metadata update

**Milestone**: Full image and tag management API with tests.

---

## Phase 7: Admin API — Galleries & Tours

- [x] **7.1** `GalleryService`: create, update, delete, set default, list all (ordered by `sortOrder`), reorder galleries, add/remove images to/from gallery, export, import
- [x] **7.2** `AdminGalleryController` (`/api/v1/admin/galleries`):
  - `POST /` — create gallery
  - `GET /` — list all galleries (ordered by `sortOrder`)
  - `GET /{publicId}` — get gallery detail (includes image count and tour count)
  - `PUT /{publicId}` — update gallery (name, subtitle, code, visible, description, headlinerImagePublicId, sortOrder, bioPhotoUrl, bioText, bioLinks, theme, borderStyle, ad settings)
  - `PUT /reorder` — batch update sort orders for all galleries (accepts list of `{ publicId, sortOrder }`)
  - `PUT /{publicId}/default` — set as default gallery
  - `POST /{publicId}/images` — add image(s) to gallery (body: list of image public IDs)
  - `DELETE /{publicId}/images/{imagePublicId}` — remove image from gallery (also removes from gallery's tours)
  - `DELETE /{publicId}` — delete gallery (cascades tours, gallery-image associations)
  - `GET /{publicId}/export` — export gallery definition as JSON (`GalleryExportDto`). Does not include image metadata — images are referenced by short ID.
  - `POST /import` — import gallery from JSON. Requires `code` in the body (the new gallery code). Processing order: (1) create the gallery with settings from the JSON, (2) resolve `gallery.imageShortIds` — for each short ID found, create a `gallery_image` association; short IDs not found are skipped with a warning, (3) for each tour, resolve `imageShortIds` against the images that were successfully associated with the gallery — images not in the gallery are skipped with a warning, (4) resolve `tagNames` for tour-tag associations — tags not found are skipped with a warning, (5) set headliner images if their short IDs resolved. Returns the created gallery response + warnings list.
- [x] **7.3** `TourService`: create, update, delete, reorder images, reorder tours within a gallery. **Tag-based auto-population**: when creating a tour with associated tags, query all images matching those tags (scoped to the gallery via `gallery_image`) and add them as the initial image list (ordered by image PK). **Ongoing sync** logic (called from `ImageService` and tag endpoints): when a tag is added to an image, add the image to all tours associated with that tag (within galleries that contain the image); when a tag is removed from an image, remove it from tours where it was included only via that tag.
- [x] **7.4** `AdminTourController` (`/api/v1/admin/galleries/{galleryPublicId}/tours`):
  - `POST /` — create tour (accepts name, description, optional list of tag public IDs for auto-population, optional sortOrder)
  - `GET /` — list tours in gallery (ordered by `sortOrder`)
  - `GET /{tourPublicId}` — get tour detail with ordered images and associated tags
  - `PUT /{tourPublicId}` — update tour (name, description, headlinerImagePublicId, imagePublicIds list with order, sortOrder, tag associations)
  - `PUT /reorder` — batch update sort orders for tours in this gallery
  - `DELETE /{tourPublicId}` — delete tour
- [x] **7.5** Default gallery constraint: at most one default — setting a new default clears the old one
- [x] **7.5a** "All Images" tour: a virtual tour (not stored in the `tour` table) dynamically generated by the public API. Contains all images associated with the gallery (via `gallery_image`) ordered by creation date (newest first). Included in a gallery's tour list only when `showAllImagesTour` is true. Uses a reserved `publicId` (e.g., `all-images`) that cannot be used by admin-created tours. Images in this tour are not manually reorderable.
- [x] **7.6** Gallery code validation: reject codes that don't match `^[A-Z][A-Z0-9]{0,4}$`, reject duplicate codes. (Note: reserved lowercase paths like `admin`, `api`, `galleries` can never collide because the regex requires all uppercase.)
- [x] **7.7** `ViewStatsService`: query `page_view_daily` for a date range, grouped by entity. Methods: top galleries by views, top tours by views (optionally filtered by gallery), top images by views (with direct-vs-tour breakdown). Returns sorted results with totals and daily trend data.
- [x] **7.8** `AdminStatsController` (`/api/v1/admin/stats`):
  - `GET /galleries?from=YYYY-MM-DD&to=YYYY-MM-DD` — gallery view counts for date range, sorted by total views descending
  - `GET /tours?from=YYYY-MM-DD&to=YYYY-MM-DD&galleryPublicId={optional}` — tour view counts, optionally filtered by gallery
  - `GET /images?from=YYYY-MM-DD&to=YYYY-MM-DD` — image view counts with direct/tour breakdown, sorted by total views descending
- [x] **7.9** Verify: integration tests — CRUD, default toggle, visibility toggle, cascade delete, sort ordering (galleries and tours), code uniqueness, code validation, gallery-image scoping (images not in gallery don't appear in its tours), export/import round-trip (including warnings for missing images and tags), **tag-tour sync**: create tour with tags (verify auto-populated images scoped to gallery), add tag to image (verify added to matching tours in galleries containing the image), remove tag from image (verify removed from tours only linked via that tag), delete tag (verify tours updated), delete image (verify removed from all tours and gallery associations), **view stats**: record views via public endpoint then query admin stats (verify counts, date range filtering, direct-vs-tour breakdown for images)

**Milestone**: Full gallery and tour management API with tests.

---

## Phase 8: Frontend — Admin Panel

The admin panel uses a **fixed simplistic color scheme** (light gray backgrounds, white cards, blue accents, dark text) — it never applies gallery themes. This keeps the admin UI consistent and avoids confusion between "what the admin sees" and "what visitors see." Theme preview is handled by isolated preview components only.

- [ ] **8.0** Set up React Router, Axios API client (`src/api/`), and admin layout shell. Configure route structure for admin (`/admin/*`) and visitor routes (to be implemented in Phase 10). Add a global 401 interceptor on the Axios client that redirects to the login page when a JWT expires.
- [ ] **8.1** Login page (`/admin/login`): password form, store JWT in memory/localStorage
- [ ] **8.2** Protected admin routes: redirect to login if no valid token
- [ ] **8.3** Admin dashboard: overview of galleries (count, default), images (count, orphan count), tours (count). Quick links to common tasks.
- [ ] **8.3a** **Password change** (`/admin/password`): form with current password, new password, confirm new password. Minimum 8 characters enforced client-side and server-side. Accessible from admin nav bar.
- [ ] **8.4** Image management UI (`/admin/images`): **paginated list** (default 20 per page) with thumbnail column, title, artist, tags, NSFW badge, upload date, gallery badges. **Search bar** for text search across title/artist/description. **Filter controls**: tag dropdown, NSFW toggle, **orphan toggle** (show only images not assigned to any gallery). **Sortable columns**: title, artist, upload date. Each row links to the image detail/edit page. Bulk actions: select multiple images via checkboxes for bulk tag assignment or bulk metadata edit.
- [ ] **8.4a** Image detail/edit page (`/admin/images/:publicId`): edit all metadata (title, artist name, description, art creation date, artist comments, notes, admin notes), **URL field** (editable — changing it triggers thumbnail regeneration with success/error feedback), NSFW checkbox, set base image, **tag checkboxes** (all existing tags shown, check/uncheck to add/remove), inline "Create new tag" field. **Thumbnail preview** with a "Regenerate thumbnail" button. **Image usage report**: panel showing which galleries and tours reference this image (with links). If image is an orphan, show a prominent "Not in any gallery" warning.
- [ ] **8.4b** Bulk load page (`/admin/images/bulk`): **gallery picker** (required — select which gallery to add images to), textarea for pasting URLs (one per line). Submit triggers sequential processing. Progress indicator shows per-URL status (success/failed + error message). If first 3 consecutive images fail, the load aborts and displays the reason. Successfully loaded images are listed with links to their edit pages.
- [ ] **8.4c** Bulk metadata edit view: table of selected images with inline-editable cells for title, artist, description, tags. "Save all" button submits `PUT /bulk-metadata`. Per-row success/failure indicators.
- [ ] **8.4d** **Regenerate all thumbnails** button (in image management toolbar): triggers `POST /regenerate-all-thumbnails`, shows a progress bar that polls `GET /jobs/{jobId}` every 3 seconds. Displays processed/total count, and on completion shows summary (success count + list of failures with error messages).
- [ ] **8.5** Gallery management UI (`/admin/galleries`): list galleries in `sortOrder`. **Drag-and-drop reorder** (updates sort orders via `PUT /reorder`). Create/edit/delete, set default. Gallery form includes: name, subtitle (optional), code (with validation feedback), visibility checkbox ("Visible to visitors"), "Show 'All Images' tour" checkbox, description, headliner image picker. Biography section: artist photo URL field, bio text (multiline textarea), bio links (add/remove/reorder list of label + URL pairs).
- [ ] **8.5a** **Gallery theme & style editor** — when creating or editing a gallery, the admin sees:
  - Theme picker dropdown (all 9 themes)
  - Border style picker (theme default, none, thin line, double line, shadow, rounded, ornate frame, polaroid)
  - **Ad settings panel**:
    - "Enable ads" — master toggle (checkbox). When unchecked, all other ad controls are greyed out.
    - For each placement (landing page banner, image detail sidebar):
      - Checkbox to enable/disable that placement
      - Text field for the AdSense slot ID (numeric string from Google's AdSense dashboard)
      - Slot ID field is required when the placement is enabled; greyed out when disabled
    - No raw JavaScript input — the app constructs standard AdSense `<ins>` tags from the publisher ID + slot IDs only
  - **Live preview panel**: miniature sample renderings of the Landing Page (headliner + tour cards), Tour Grid (thumbnail grid), and Image Detail (full image with border + metadata). Uses bundled sample artwork (from Phase 1.4a). Every change to theme, border, or ad settings instantly updates all three previews — ad placements shown as labeled placeholder boxes in the preview. **Note**: only the preview applies the gallery theme — the surrounding admin chrome remains in the admin color scheme.
- [ ] **8.5b** **Tag management page** (`/admin/tags`): list all tags with the count of images using each tag. Admin can create new tags and delete existing tags (with confirmation — warns that deletion will update all tours associated with this tag).
- [ ] **8.5c** **Gallery image management**: within the gallery edit page, a panel showing images associated with this gallery. Admin can add images (searchable picker from the global image pool) or remove images from the gallery. Removing an image from a gallery also removes it from all of that gallery's tours.
- [ ] **8.5d** **Gallery export/import**: "Export" button on gallery detail page downloads a JSON file. "Import gallery" button on gallery list page accepts a JSON file upload and a new gallery code. Displays warnings for any images not found by short ID or tags not found by name.
- [ ] **8.6** Tour management UI (`/admin/galleries/:publicId/tours`): list tours in `sortOrder`. **Drag-and-drop reorder** tours (updates sort orders via `PUT /reorder`). Create/edit tours within a gallery: name, description (textarea), drag-and-drop image ordering, set headliner. **Tag association**: when creating or editing a tour, the admin can select zero or more tags from a checkbox list. When tags are selected at creation, all matching images (scoped to the gallery) are auto-populated into the tour. Associated tags are displayed on the tour detail view.
- [ ] **8.6a** **View statistics page** (`/admin/stats`): date range picker (defaults to last 30 days) with three tabs:
  - **Galleries** — sortable table of galleries with total view count and a sparkline/bar showing the daily trend
  - **Tours** — sortable table of tours with view counts, filterable by gallery dropdown
  - **Images** — sortable table of most-viewed images with columns for total views, direct views, and tour views (direct views indicate the image is being shared externally via shareable links)
- [ ] **8.7** Confirmation dialogs for destructive actions. Image deletion dialog requires the admin to select a substitute image (searchable dropdown of existing images, or the "Image not available" option `00000000`) before confirming.

**Milestone**: Admin can manage all content through the browser.

---

## Phase 9: Public API

The read-only endpoints used by the visitor-facing frontend.

- [ ] **9.1** `PublicController` (`/api/v1/public`):
  - `GET /gallery` — get the default gallery (code, name, description, headliner, tour list ordered by `sortOrder`, theme, borderStyle, ad settings)
  - `GET /galleries` — list all **visible** galleries ordered by `sortOrder` (code, name, description, headliner, theme)
  - `GET /galleries/by-code/{code}` — get a specific gallery by its code (works for both visible and invisible galleries — anyone with the code can access it), includes tours ordered by `sortOrder` (with the "All Images" tour included or excluded based on `showAllImagesTour`), theme, borderStyle, ad settings
  - `GET /galleries/by-code/{code}/tours/{tourPublicId}` — get tour with description and ordered images (scoped to gallery's images)
  - `GET /galleries/by-code/{code}/images/{shortId}` — get a single image by short ID
  - `GET /thumbnails/{shortId}` — serve the thumbnail binary (JPEG) for an image. Served from in-memory cache when available, falls back to database. Returns `Content-Type: image/jpeg`. If the short ID is not found among active images, check the `image_redirect` table: if a redirect exists, return the substitute image with a flag indicating it was redirected. If the short ID is `00000000` or redirects to `00000000`, return a sentinel response that the frontend renders as "Image not available."
  - `GET /themes` — list all available themes (enum values with display names)
  - `GET /config` — public app config (AdSense publisher ID if configured, site domain for shareable link generation)
  - `POST /views` — fire-and-forget view tracking (rate-limited per IP). Accepts `{ type, code, tourPublicId?, imageShortId?, context? }`. Resolves the entity by code/publicId/shortId, performs an UPSERT on `page_view_daily` for today's date. Returns `204 No Content`. Failures are silently swallowed (tracking should never break the visitor experience). For galleries and tours, `context` is always `DIRECT`. For images, `context` is `DIRECT` (from shareable link) or `TOUR` (from tour navigation).
- [ ] **9.2** Response shaping: only include fields needed by the frontend (no internal IDs, no admin-only details). Image responses include `shortId` and `nsfw` flag, but NOT `adminNotes` or `uploadedAt` (use `PublicImageResponse`). Gallery responses include `code` but NOT `visible`. Gallery detail responses include a computed `hasNsfwContent` boolean (true if any image associated with the gallery is marked NSFW) — the frontend uses this to decide whether to show the "Hide NSFW images" checkbox.
- [ ] **9.3** Verify: integration tests — public endpoints return correct data, no auth required, invisible galleries excluded from `/galleries` list but accessible via `/galleries/by-code/{code}`, image accessible by short ID, galleries and tours returned in sort order, gallery-image scoping enforced on public endpoints

**Milestone**: Public API complete. Backend is fully functional.

---

## Phase 10: Frontend — Theme System & Core Visitor Pages

- [ ] **10.1** Extend the React Router configuration (set up in Phase 8 for admin routes) with visitor routes. Add the public API client functions in `src/api/`. Create the visitor base layout with persistent navigation component (separate from the admin layout).
- [ ] **10.2** Define theme system: CSS custom properties (variables) for each theme, applied via a class on the root gallery layout. Each theme defines: `--bg-primary`, `--bg-secondary`, `--text-primary`, `--text-secondary`, `--accent`, `--card-bg`, `--card-border`, `--heading-font`, and a default image border style. Border styles are implemented as CSS classes (`border-none`, `border-thin-line`, `border-double-line`, `border-shadow`, `border-rounded`, `border-ornate-frame`, `border-polaroid`) that can override the theme default via the gallery's `borderStyle` setting. Themes:
  - **Light** — clean white/gray, dark text, blue accent, sans-serif headings
  - **Dark** — near-black backgrounds, light text, electric blue accent, sans-serif headings
  - **Pastel** — soft lavender/mint/blush backgrounds, muted text, warm pink accent, rounded friendly feel
  - **Spring** — fresh greens, warm yellows, floral accents, light airy backgrounds, serif headings
  - **Winter** — cool slate blues, icy whites, silver accents, crisp sans-serif
  - **Cyberpunk** — deep purple/black, neon magenta and cyan accents, glitch-style monospace headings
  - **Sunset** — warm gradients (peach to coral to deep orange), cream text, gold accent
  - **Ocean** — deep navy to teal, sandy highlights, wave-like accent colors, relaxed serif headings
  - **Monochrome** — pure black and white, no color accents, high-contrast, elegant serif headings
- [ ] **10.3** **Root redirect** (`/`): fetch default gallery, redirect to `/<default-gallery-code>`.
- [ ] **10.4** **Landing Page** (`/:code`): fetch gallery by code, apply its theme. Display gallery headliner image prominently, gallery name, subtitle (if present), description. If biography fields are populated, render a biography section: artist photo (from `bioPhotoUrl`), biographical text (`bioText`, rendered with paragraph breaks), and resource links (`bioLinks`, each as a clickable labeled link). Omit any empty biography fields; omit the entire section if all are empty. Below, list available tours (in `sortOrder`) as clickable cards — each card shows the tour's name, description (truncated if long), and a thumbnail of the tour's headliner image (loaded via `/api/v1/public/thumbnails/{shortId}`).
- [ ] **10.5** **Tour Page** (`/:code/tours/:tourPublicId`): display tour name and description at top. Below, a grid of thumbnails **5 columns wide**, as many rows as needed to show all images. Clicking any thumbnail navigates to the image detail view. Navigation breadcrumbs: Gallery > Tour Name.
- [ ] **10.6** **Image Detail Page — tour context** (`/:code/tours/:tourPublicId/:imageShortId`): display full-size image with decorative border (from gallery's borderStyle override, or the theme's default border). Show all image metadata below or beside the image: title, artist name, description, art creation date, artist comments, notes. "Previous" and "Next" buttons on the left and right sides for sequential navigation through the tour. Navigation breadcrumbs: Gallery > Tour Name > Image Title. Display a **shareable link** (`https://<domain>/<code>/<imageShortId>`) with a copy button.
- [ ] **10.7** **Image Detail Page — direct link** (`/:code/:imageShortId`): same image display and metadata as tour context, but no prev/next buttons and no tour breadcrumb. Breadcrumbs: Gallery > Image Title. This is the URL used in shareable links (Discord, social media, etc.). If the API indicates a redirect, navigate to the substitute image's URL. If the short ID is `00000000` or resolves to it, render a grey placeholder box with "Image not available" text.
- [ ] **10.8** **Galleries page** (`/galleries`): list all **visible** galleries in `sortOrder`. Each card shows the gallery name, code, a thumbnail of the gallery's headliner image, and a visual preview of its theme. Click to browse one (navigates to `/<code>`).
- [ ] **10.9** Persistent navigation & footer: breadcrumb-style nav at the top of every page. Controls to return to the gallery landing page and (when in tour context) to the tour grid. Footer includes a "Browse other galleries" link when there are other visible galleries; hidden if the current gallery is the only visible one.
- [ ] **10.10** **NSFW filter**: if the gallery's `hasNsfwContent` flag is true, display a "Hide NSFW images" checkbox in the gallery header/nav area. When checked, all images with `nsfw: true` are replaced with empty placeholder boxes (same dimensions, neutral background, no image pixels loaded) in both thumbnails and full-size views. All other information (title, metadata, position in tour, shareable link, prev/next navigation) remains visible. Store the visitor's preference in `localStorage` so it persists across page navigations. If `hasNsfwContent` is false, the checkbox is not rendered.
- [ ] **10.10a** **View tracking**: each visitor page (landing, tour, image detail) fires a `POST /api/v1/public/views` after render. The call is fire-and-forget (async, response ignored, errors swallowed). Image detail pages send `context: "TOUR"` when accessed via a tour route and `context: "DIRECT"` when accessed via a direct/shareable link.
- [ ] **10.11** Responsive design: works on desktop and mobile. Tour grid adapts column count (5 on desktop, fewer on smaller screens). Image detail prev/next buttons reposition for touch.
- [ ] **10.12** Error handling: graceful 404, loading states

**Milestone**: Visitors can browse galleries, tours, and images. Themes and borders render correctly. Shareable links work.

---

## Phase 11: Frontend — AdSense Integration

- [ ] **11.1** Conditionally include `adsbygoogle.js` script (only when publisher ID is configured via `/api/v1/public/config`)
- [ ] **11.2** Reusable `<AdUnit slotId={string}>` React component: renders `<ins class="adsbygoogle">` with `data-ad-client` (publisher ID) and `data-ad-slot` (slot ID), calls `adsbygoogle.push({})` on mount, handles cleanup on route change, uses responsive ad format
- [ ] **11.3** Ad container styled with neutral background so it doesn't clash with gallery themes
- [ ] **11.4** Placement logic reads gallery's ad settings and renders `<AdUnit>` in the appropriate positions:
  - Landing page: banner between gallery description and tour cards (when `adLandingBanner` is true)
  - Image detail: ad in sidebar or below metadata (when `adImageDetailSidebar` is true)
- [ ] **11.5** All ad placements gated on `adsEnabled` — if false, no ad components render

**Milestone**: AdSense ads render in configured placements. No ads shown when disabled.

---

## Phase 12: Testing & Quality

- [ ] **12.1** Ensure JaCoCo enforces 80%+ line coverage on backend — add missing tests
- [ ] **12.2** Write end-to-end test suite (Maven `e2e` profile): full scenario as described in CLAUDE.md. Uses the bundled sample artwork images as test fixtures. Must cover: gallery code routing, direct image links via short ID, visibility filtering on `/galleries`, shareable link format correctness, gallery and tour sort ordering, image deletion with redirect (verify old link resolves to substitute), one-hop collapse, `00000000` "image not available" rendering, gallery-image scoping (image in gallery A not visible in gallery B's tours), orphan detection (image not in any gallery shows in orphan filter), **tag-tour auto-sync** (create tour with tags → verify auto-populated from gallery-scoped images, add tag to image → verify image appears in matching tours, remove tag from image → verify image removed from tours where it was only included via that tag, delete a tag → verify tours updated), gallery export/import round-trip (including partial import with missing images).
- [ ] **12.3** OpenAPI spec validation: ensure springdoc generates accurate docs, accessible at `/swagger-ui.html`
- [ ] **12.4** **Frontend — visitor page tests** (React Testing Library): smoke tests for landing page, tour grid (verify 5-column layout), image detail (both tour-context and direct-link variants), galleries list (verify sort order). Verify: theme application, breadcrumb navigation, prev/next image navigation (tour context only), shareable link displayed with copy button, root `/` redirects to default gallery code, direct image link works without tour context, footer "Browse other galleries" link appears/hides based on visible gallery count, galleries page only shows visible galleries in sort order, tour description displayed on tour page and landing page cards. NSFW filter: checkbox appears only when gallery has NSFW content, toggling replaces NSFW images with placeholders (thumbnails and full-size), metadata remains visible, preference persists in localStorage. View tracking: verify each page fires a POST to `/api/v1/public/views` on render, verify image detail sends correct context (`DIRECT` vs `TOUR`), verify tracking failures don't affect page rendering.
- [ ] **12.5** **Frontend — admin panel tests** (React Testing Library + MSW for API mocking). The admin panel is the most complex part of the application and requires the most thorough testing:
  - **Admin styling**: verify admin pages use the fixed admin color scheme, NOT gallery themes. Verify theme preview is isolated within the preview component.
  - **Auth flow**: login with correct/incorrect password, JWT stored, redirect to login when token expires or is missing, logout clears token
  - **Password change**: change password successfully, reject wrong current password, reject new password under 8 characters, verify validation messages
  - **Image management**: paginated list with thumbnails renders correctly, page navigation works, search by text filters results, filter by tag, filter by NSFW, **filter by orphan** (verify only unassigned images shown), sort by columns (title, artist, upload date), verify admin notes are NOT exposed in public API responses
  - **Image detail/edit**: edit all metadata fields (title, artist, description, art creation date, artist comments, notes, admin notes), change URL and verify thumbnail regeneration feedback (success and failure), toggle NSFW flag, set/clear base image, add/remove tags via checkboxes, create new tag inline (verify it appears immediately), regenerate thumbnail button, image usage report panel shows correct galleries and tours, **orphan warning** displayed when image is not in any gallery
  - **Bulk load**: **gallery picker required**, paste URLs, verify progress indicator, verify per-URL success/failure messages, verify abort on 3 consecutive failures with error display
  - **Bulk metadata edit**: select multiple images, verify inline-editable table, save all and verify per-row feedback
  - **Regenerate all thumbnails**: trigger and verify progress bar polls job status, verify completion summary with success/failure counts
  - **Gallery management**: create gallery (verify defaults: LIGHT theme, no border override, ads disabled, visible, code validated), edit gallery (name, code, description, headliner), delete gallery with confirmation, set default gallery (verify previous default is cleared), toggle visibility, code validation (reject invalid formats, reject duplicates), **drag-and-drop reorder** galleries (verify sort order persists)
  - **Gallery image management**: add images to gallery from global pool, remove image from gallery (verify removed from gallery's tours), verify images scoped correctly, verify orphan status updates when image added/removed from galleries
  - **Gallery export/import**: export gallery, import with new code, verify warnings for missing images and tags
  - **Theme & style editor**: select each of the 9 themes and verify live preview updates, select each border style and verify preview updates, verify theme + border combination renders correctly in all three preview panes (landing, tour grid, image detail), verify preview is isolated from admin chrome
  - **Ad settings**: toggle master "Enable ads" checkbox — verify all other ad controls grey out when disabled, enable each placement (landing page banner, image detail sidebar) and verify slot ID field becomes required, enter slot IDs and verify preview shows ad placeholder boxes in correct positions, disable a placement and verify its slot ID field greys out, save and reload — verify ad settings persist
  - **Tag management page**: list all tags with image counts, create a new tag, delete a tag (verify confirmation dialog warns about tour impact), verify deleted tag is removed from all images and tour associations
  - **Image page tag UI**: verify all tags shown as checkboxes, check a tag to add it, uncheck to remove it, create a new tag inline (verify it appears immediately in the checkbox list and is checked), verify tag changes trigger auto-sync to tours (covered more thoroughly in backend integration tests)
  - **Tour management**: create tour within a gallery (with name and description), add images to a tour, drag-and-drop reorder images (verify sort order persists after save), set tour headliner image, remove images from a tour, delete tour with confirmation, **drag-and-drop reorder** tours within gallery (verify sort order persists), **tag association**: select tags at tour creation and verify matching images are auto-populated (scoped to gallery), verify associated tags are displayed on tour detail, verify tour description field saves and displays
  - **Live preview fidelity**: verify all three preview panes render with sample artwork, verify preview uses the sample artwork (not real gallery images), verify border style renders correctly on images in the preview, verify ad placeholders appear/disappear as ad settings change
  - **Error handling**: API failure states (network errors, 401 unauthorized, 404 not found, 409 conflict on delete of in-use entity), form validation (required fields, slot ID format, password length)
  - **View statistics page**: verify date range picker defaults to last 30 days, verify galleries/tours/images tabs render with correct data, verify gallery filter on tours tab, verify direct-vs-tour column on images tab, verify sorting works on all columns, verify empty state when no views exist
  - **Cross-cutting concerns**: verify no admin API calls are made without the JWT token, verify confirmation dialogs block destructive actions until confirmed

**Milestone**: All quality gates pass. `mvn verify` is green.

---

## Phase 13: Deployment & Operations

- [ ] **13.1** Finalize `docker-compose.yml`: production-ready config, env var references, restart policies
- [ ] **13.2** Apache2 config: `ProxyPass /images !`, `ProxyPass / http://localhost:8080/`
- [ ] **13.3** DB backup cron script (`pg_dump` from Docker container)
- [ ] **13.4** Deployment script or instructions: build JAR, push to EC2, `docker-compose up -d`
- [ ] **13.5** Smoke test: app serves pages, images load from Apache, admin login works

**Milestone**: Application live and serving traffic.

---

## Notes

- Each phase should be committed separately for clean git history.
- Phases 1–7 and 9 (backend) can be built and tested without any frontend.
- Phase 8 (admin frontend) and Phases 10–11 (visitor frontend) can be developed in parallel once the API contract (Phase 4) is stable.
- Image short IDs are randomly generated 8-digit numbers. The `@PrePersist` generation with collision retry works identically on both PostgreSQL and H2.
- The plan assumes we build and test incrementally — each phase is usable before the next begins.
- Images are global resources but are scoped to galleries via `gallery_image`. An image can belong to multiple galleries. Tours only operate on images within their gallery.
- All API paths are versioned under `/api/v1/`. See CLAUDE.md for API conventions (pagination envelope, error format, naming).
