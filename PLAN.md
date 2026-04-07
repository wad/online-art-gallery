# Implementation Plan — Online Art Gallery

This plan breaks the project into phases, each producing a buildable/testable milestone. Phases are ordered by dependency: infrastructure first, then data layer, then API, then frontend, then deployment.

---

## Phase 1: Project Skeleton & Build Infrastructure

Set up the Maven multi-module project structure so that `mvn package` produces a single JAR.

- [ ] **1.1** Create parent `pom.xml` (multi-module: `backend`, `frontend`)
- [ ] **1.2** Create `backend/pom.xml` with Spring Boot 3, Java 21, springdoc-openapi, JJWT, MapStruct, Liquibase, PostgreSQL driver, H2 (test), JaCoCo
- [ ] **1.3** Create `frontend/pom.xml` with `frontend-maven-plugin` (install Node/npm, run `npm install`, run `npm run build`), copy build output to `backend/src/main/resources/static`
- [ ] **1.4** Scaffold React + TypeScript + Vite app in `frontend/`
- [ ] **1.4a** Bundle sample artwork: ~6-8 public domain paintings (optimized/resized) in `frontend/src/assets/sample-artwork/`. Source from Met Open Access or similar. Used for admin theme preview and e2e test fixtures.
- [ ] **1.5** Create `backend/src/main/java/org/wadhome/oag/OagApplication.java` (Spring Boot main class)
- [ ] **1.6** Create `application.yml` with datasource config using env vars, JVM settings, `oag.site-domain` (from `OAG_SITE_DOMAIN`), `oag.adsense.publisher-id` (from `OAG_ADSENSE_PUBLISHER_ID` env var, optional)
- [ ] **1.7** Create `docker-compose.yml` (PostgreSQL 16 container + app container, `-Xmx512m`)
- [ ] **1.8** Create `.gitignore` for Java/Maven/Node/React artifacts
- [ ] **1.9** Verify: `mvn clean package` succeeds, app starts, serves Vite placeholder page

**Milestone**: Clean build producing a runnable JAR that serves a blank React page.

---

## Phase 2: Database Schema (Liquibase)

Define all tables via Liquibase XML changesets. No application code yet — just the schema.

- [ ] **2.1** Create Liquibase master changelog (`db/changelog/db.changelog-master.xml`)
- [ ] **2.2** Changeset: `image` table — `id` (bigserial PK), `public_id` (UUID, unique, not null), `short_id` (text, unique, not null — randomly generated zero-padded 8-digit number, e.g. "00004719", immutable after creation), `url` (text, not null), `title` (text), `artist_name` (text), `description` (text), `art_creation_date` (text, nullable — freeform: "1975", "1972-1974", etc.), `artist_comments` (text, nullable), `notes` (text, nullable), `admin_notes` (text, nullable — visible only in admin panel, never exposed to visitors), `nsfw` (boolean, not null, default false), `uploaded_at` (timestamptz, not null, default `now()` — auto-set on creation, admin-only), `base_image_id` (bigint FK self-ref, nullable)
- [ ] **2.3** Changeset: `tag` table — `id` (bigserial PK), `public_id` (UUID, unique), `name` (text, unique, not null)
- [ ] **2.4** Changeset: `image_tag` join table — `image_id` (FK), `tag_id` (FK), composite PK
- [ ] **2.5** Changeset: `gallery` table — `id` (bigserial PK), `public_id` (UUID, unique), `code` (text, unique, not null — 1-5 chars, regex `^[A-Z][A-Z0-9]{0,4}$`), `name` (text, not null), `subtitle` (text, nullable — e.g., artist name), `description` (text), `headliner_image_id` (bigint FK to image, nullable), `is_default` (boolean, default false), `visible` (boolean, not null, default true), `show_all_images_tour` (boolean, not null, default true), `bio_photo_url` (text, nullable — URL of artist photo), `bio_text` (text, nullable — biographical paragraphs, supports newlines), `theme` (text, not null, default `'LIGHT'`), `border_style` (text, nullable — null means use theme default)
- [ ] **2.5b** Changeset: `gallery_bio_link` table — `id` (bigserial PK), `gallery_id` (bigint FK to gallery, not null), `label` (text, not null), `url` (text, not null), `sort_order` (int, not null)
- [ ] **2.5a** Changeset: ad settings columns on `gallery` table — `ads_enabled` (boolean, not null, default false), `ad_landing_banner` (boolean, not null, default false), `ad_landing_banner_slot` (text, nullable), `ad_image_detail_sidebar` (boolean, not null, default false), `ad_image_detail_sidebar_slot` (text, nullable)
- [ ] **2.6** Changeset: `tour` table — `id` (bigserial PK), `public_id` (UUID, unique), `name` (text, not null), `gallery_id` (bigint FK to gallery, not null), `headliner_image_id` (bigint FK to image, nullable)
- [ ] **2.7** Changeset: `tour_image` join table — `tour_id` (FK), `image_id` (FK), `sort_order` (int, not null), composite PK on (tour_id, image_id)
- [ ] **2.7a** Changeset: `tour_tag` join table — `tour_id` (FK to tour), `tag_id` (FK to tag), composite PK. Records which tags a tour is associated with for auto-sync purposes.
- [ ] **2.8** Changeset: `thumbnail` table — `image_id` (bigint FK to image, PK), `data` (bytea, not null — JPEG binary, max 100x100px preserving aspect ratio)
- [ ] **2.8a** Changeset: `image_redirect` table — `old_short_id` (text, PK — the short ID of the deleted image), `new_short_id` (text, not null — the substitute short ID, may be `"00000000"` for "image not available")
- [ ] **2.8b** Changeset: `page_view_daily` table — `id` (bigserial PK), `view_date` (date, not null), `entity_type` (text, not null — `'GALLERY'`, `'TOUR'`, or `'IMAGE'`), `entity_id` (bigint, not null — FK to the viewed entity), `context` (text, not null — `'DIRECT'` or `'TOUR'`), `view_count` (int, not null, default 0). Unique constraint on `(view_date, entity_type, entity_id, context)`. Index on `(entity_type, entity_id, view_date)` for date-range queries.
- [ ] **2.9** Changeset: `admin_config` table — `id` (bigserial PK), `password_hash` (text, not null) — single row for the admin password
- [ ] **2.10** Add indexes: `image.public_id`, `image.short_id`, `gallery.public_id`, `gallery.code`, `tour.public_id`, `gallery.is_default`
- [ ] **2.11** Verify: app starts, Liquibase runs migrations against Dockerized PostgreSQL, schema matches design

**Milestone**: Database schema fully defined and versioned. App starts cleanly with empty tables.

---

## Phase 3: JPA Entities & Repositories

Map the schema to Java, establish the data access layer.

- [ ] **3.1** `Image` entity with self-referential `@ManyToOne` for `baseImage`, immutable `shortId` field (randomly generated zero-padded 8-digit string via `@PrePersist` — generate random number 0–99999999, zero-pad, retry on collision), `adminNotes` field (String, nullable), `nsfw` field (boolean, default false), `uploadedAt` (Instant, auto-set via `@CreationTimestamp`, admin-only)
- [ ] **3.2** `Tag` entity
- [ ] **3.3** `Image` ↔ `Tag` many-to-many mapping
- [ ] **3.4** `Gallery` entity with `code` (unique, validated: 1-5 chars, `^[A-Z][A-Z0-9]{0,4}$`), `subtitle` (String, nullable), `visible` (boolean), `showAllImagesTour` (boolean), `bioPhotoUrl` (String, nullable), `bioText` (String, nullable), `@OneToMany` to `GalleryBioLink` (ordered list), `@ManyToOne` to `Image` (headliner), `@Enumerated(STRING)` for `theme` field
- [ ] **3.4d** `GalleryBioLink` entity — `id`, `gallery` (`@ManyToOne`), `label` (String), `url` (String), `sortOrder` (int)
- [ ] **3.4a** `GalleryTheme` enum: `LIGHT`, `DARK`, `PASTEL`, `SPRING`, `WINTER`, `CYBERPUNK`, `SUNSET`, `OCEAN`, `MONOCHROME`
- [ ] **3.4b** `BorderStyle` enum: `THEME_DEFAULT`, `NONE`, `THIN_LINE`, `DOUBLE_LINE`, `SHADOW`, `ROUNDED`, `ORNATE_FRAME`, `POLAROID` — nullable on Gallery entity (null = THEME_DEFAULT)
- [ ] **3.4c** Ad settings fields on `Gallery` entity: `adsEnabled` (boolean), `adLandingBanner` (boolean), `adLandingBannerSlot` (String, nullable), `adImageDetailSidebar` (boolean), `adImageDetailSidebarSlot` (String, nullable)
- [ ] **3.5** `Tour` entity with `@ManyToOne` to `Gallery`, `@ManyToOne` to `Image` (headliner), `@ManyToMany` to `Tag` (via `tour_tag` join table — tags associated with this tour for auto-sync)
- [ ] **3.5a** `TourTag` mapping: `Tour` ↔ `Tag` many-to-many via `tour_tag` table. When tags are associated with a tour, any image with a matching tag is automatically included in the tour.
- [ ] **3.6** `TourImage` entity (or `@OrderColumn` on Tour's image list) capturing sort order
- [ ] **3.7** `Thumbnail` entity — PK is `imageId`, `data` (byte[], `@Lob`)
- [ ] **3.7a** `ImageRedirect` entity — `oldShortId` (String, PK), `newShortId` (String, not null)
- [ ] **3.7b** `PageViewDaily` entity — `id` (Long), `viewDate` (LocalDate), `entityType` (String/enum), `entityId` (Long), `context` (String/enum: `DIRECT`, `TOUR`), `viewCount` (int). Repository with UPSERT-style method: increment `viewCount` by 1 for a given `(viewDate, entityType, entityId, context)`, inserting a new row if none exists.
- [ ] **3.8** `AdminConfig` entity
- [ ] **3.9** Spring Data JPA repositories for each entity
- [ ] **3.10** Verify: unit tests with H2 (`MODE=PostgreSQL`) — CRUD operations on all entities, relationship integrity

**Milestone**: Full data access layer with passing tests.

---

## Phase 4: DTOs & MapStruct Mappers

Define the API contract (request/response shapes) and compile-time mapping.

- [ ] **4.1** DTOs for Image: `ImageResponse` (all fields including shortId, artCreationDate, artistComments, notes, adminNotes, nsfw, uploadedAt), `PublicImageResponse` (same but WITHOUT adminNotes and uploadedAt — used by public API; includes `nsfw` flag so frontend can handle display), `ImageCreateRequest` (url), `ImageUpdateRequest` (title, artistName, description, artCreationDate, artistComments, notes, adminNotes, nsfw, baseImagePublicId), `ImageBulkLoadRequest` (list of URLs)
- [ ] **4.2** DTOs for Tag: `TagResponse`, `TagCreateRequest`
- [ ] **4.3** DTOs for Gallery: `GalleryResponse` (includes `code`, `subtitle`, `visible`, `showAllImagesTour`, `bioPhotoUrl`, `bioText`, `bioLinks` list, `theme`, `borderStyle`, ad settings), `GalleryCreateRequest` (requires `code`, includes optional `subtitle`, `visible` defaults to true, `showAllImagesTour` defaults to true, optional `bioPhotoUrl`, `bioText`, `bioLinks`, optional `theme` defaults to LIGHT, optional `borderStyle`, optional ad settings all defaulting to false), `GalleryUpdateRequest` (includes optional `code`, `subtitle`, `visible`, `showAllImagesTour`, `bioPhotoUrl`, `bioText`, `bioLinks`, `theme`, `borderStyle`, ad settings), `GalleryCloneRequest` (requires new `code`)
- [ ] **4.3b** `BioLinkDto` — `label` (String), `url` (String)
- [ ] **4.3a** `GET /api/public/themes` response DTO: list of available themes with display names
- [ ] **4.4** DTOs for Tour: `TourResponse` (includes list of associated tag public IDs), `TourCreateRequest` (name, headliner, ordered image list, optional list of tag public IDs — images matching those tags are auto-added), `TourUpdateRequest` (name, headliner, image list with order, tag public IDs)
- [ ] **4.4a** DTOs for View Stats: `ViewTrackingRequest` (`type`: GALLERY/TOUR/IMAGE, `code`, optional `tourPublicId`, optional `imageShortId`, optional `context`: DIRECT/TOUR), `ViewStatsResponse` (entity name/code/shortId, total views, direct views, tour views, daily breakdown list), `DailyViewCount` (date, count)
- [ ] **4.5** DTOs for Auth: `LoginRequest`, `LoginResponse` (JWT token)
- [ ] **4.6** MapStruct mappers: Entity ↔ DTO, mapping `publicId` (not `id`), resolving FK references by public_id
- [ ] **4.7** Verify: mapper unit tests — round-trip mapping correctness

**Milestone**: Clean API contract defined. Mappers compile and pass tests.

---

## Phase 5: Authentication & Security

- [ ] **5.1** JWT utility class: generate token, validate token, extract claims (using JJWT)
- [ ] **5.2** `AuthService`: verify password against bcrypt hash in `admin_config` table
- [ ] **5.3** `AuthController`: `POST /api/auth/login` — accepts password, returns JWT
- [ ] **5.4** `JwtAuthenticationFilter` (extends `OncePerRequestFilter`): extract token from `Authorization: Bearer` header, validate, set SecurityContext
- [ ] **5.5** `SecurityConfig`: permit all on public endpoints (`/api/public/**`, `/`, static resources), require auth on `/api/admin/**`
- [ ] **5.6** Seed data: Liquibase changeset (or `CommandLineRunner`) to insert initial admin password hash
- [ ] **5.7** Verify: integration tests — login succeeds/fails, protected endpoints reject without token, accept with valid token

**Milestone**: Auth fully working. Admin endpoints are protected.

---

## Phase 6: Admin API — Images

- [ ] **6.1** `ImageService`: create, update metadata, delete, get by publicId, list all, bulk load, set/clear base image
- [ ] **6.1a** `ThumbnailService`: generate thumbnail from image URL (fetch image, scale to fit 100x100px — scale up if smaller, preserve aspect ratio, encode as JPEG, store in `thumbnail` table). Called when an image is created (including each image during bulk load). Uses a library like `java.awt.image` / Thumbnailator for resizing. Thumbnail is deleted when the image is deleted.
- [ ] **6.1b** Thumbnail in-memory cache: Caffeine cache keyed by image short ID, bounded size appropriate for 512MB heap (e.g., ~50-100MB max, LRU eviction). Cache is populated on read, invalidated when an image is deleted.
- [ ] **6.2** `AdminImageController` (`/api/admin/images`):
  - `POST /` — create single image (provide URL)
  - `POST /bulk` — bulk load (list of URLs)
  - `GET /` — list all images
  - `GET /{publicId}` — get image detail
  - `PUT /{publicId}` — update metadata
  - `PUT /{publicId}/base-image` — set base image
  - `DELETE /{publicId}/base-image` — clear base image
  - `DELETE /{publicId}?substituteShortId=XXXXXXXX` — delete image. Requires `substituteShortId` parameter (the short ID of the replacement image, or `00000000` for "image not available"). Creates an `image_redirect` entry mapping the deleted image's short ID to the substitute. If the deleted image is itself a redirect target, all existing redirects pointing to it are updated to point to the new substitute (one-hop rule). Validates no loops. Also removes the image from any tours and clears any headliner references.
- [ ] **6.3** Tag management on images:
  - `POST /{publicId}/tags` — add tag to image. **Auto-sync**: after adding, find all tours associated with this tag and add the image to them (appended at the end of tour order).
  - `DELETE /{publicId}/tags/{tagPublicId}` — remove tag from image. **Auto-sync**: after removing, find all tours associated with this tag and remove the image from them (only if the image has no other tags that the tour is also associated with).
  - `GET /tags` — list all tags with image counts
  - `POST /tags` — create a new tag
  - `DELETE /tags/{tagPublicId}` — delete a tag. Removes the tag from all images and all tour-tag associations. **Auto-sync**: for each tour that was associated with this tag, remove images that were only included via this tag (and have no other matching tour-tags).
- [ ] **6.4** Validation: prevent circular base-image references, prevent redirect loops on deletion, validate substitute short ID exists (or is `00000000`)
- [ ] **6.5** Verify: integration tests covering all endpoints, edge cases, error responses

**Milestone**: Full image management API with tests.

---

## Phase 7: Admin API — Galleries & Tours

- [ ] **7.1** `GalleryService`: create, update, delete, clone, set default, list all
- [ ] **7.2** `AdminGalleryController` (`/api/admin/galleries`):
  - `POST /` — create gallery
  - `GET /` — list all galleries
  - `GET /{publicId}` — get gallery detail
  - `PUT /{publicId}` — update gallery (name, subtitle, code, visible, description, headliner, bioPhotoUrl, bioText, bioLinks, theme, borderStyle)
  - `POST /{publicId}/clone` — clone gallery (requires new code; deep-copy tours, share images, copy theme + borderStyle + visibility + ad settings)
  - `PUT /{publicId}/default` — set as default gallery
  - `DELETE /{publicId}` — delete gallery (cascades tours)
- [ ] **7.3** `TourService`: create, update, delete, reorder images. **Tag-based auto-population**: when creating a tour with associated tags, query all images matching those tags and add them as the initial image list (ordered by image PK). **Ongoing sync** logic (called from `ImageService` and tag endpoints): when a tag is added to an image, add the image to all tours associated with that tag; when a tag is removed from an image, remove it from tours where it was included only via that tag.
- [ ] **7.4** `AdminTourController` (`/api/admin/galleries/{galleryPublicId}/tours`):
  - `POST /` — create tour (accepts optional list of tag public IDs for auto-population)
  - `GET /` — list tours in gallery
  - `GET /{tourPublicId}` — get tour detail with ordered images and associated tags
  - `PUT /{tourPublicId}` — update tour (name, headliner, image list with order, tag associations)
  - `DELETE /{tourPublicId}` — delete tour
- [ ] **7.5** Gallery clone logic: create new gallery row, iterate source tours, create new tour rows pointing to same images
- [ ] **7.6** Default gallery constraint: at most one default — setting a new default clears the old one
- [ ] **7.6a** "All Images" tour: a virtual tour (not stored in the `tour` table) dynamically generated by the public API. Contains all images in the system ordered by creation date (newest first). Included in a gallery's tour list only when `showAllImagesTour` is true. Uses a reserved `publicId` (e.g., `all-images`) that cannot be used by admin-created tours.
- [ ] **7.7** Gallery code validation: reject codes that don't match `^[A-Z][A-Z0-9]{0,4}$`, reject duplicate codes. (Note: reserved lowercase paths like `admin`, `api`, `galleries` can never collide because the regex requires all uppercase.)
- [ ] **7.9** `ViewStatsService`: query `page_view_daily` for a date range, grouped by entity. Methods: top galleries by views, top tours by views (optionally filtered by gallery), top images by views (with direct-vs-tour breakdown). Returns sorted results with totals and daily trend data.
- [ ] **7.10** `AdminStatsController` (`/api/admin/stats`):
  - `GET /galleries?from=YYYY-MM-DD&to=YYYY-MM-DD` — gallery view counts for date range, sorted by total views descending
  - `GET /tours?from=YYYY-MM-DD&to=YYYY-MM-DD&galleryPublicId={optional}` — tour view counts, optionally filtered by gallery
  - `GET /images?from=YYYY-MM-DD&to=YYYY-MM-DD` — image view counts with direct/tour breakdown, sorted by total views descending
- [ ] **7.8** Verify: integration tests — CRUD, clone (with new code), default toggle, visibility toggle, cascade delete, ordering, code uniqueness, code validation, **tag-tour sync**: create tour with tags (verify auto-populated images), add tag to image (verify added to matching tours), remove tag from image (verify removed from tours only linked via that tag), delete tag (verify tours updated), delete image (verify removed from all tours), **view stats**: record views via public endpoint then query admin stats (verify counts, date range filtering, direct-vs-tour breakdown for images)

**Milestone**: Full gallery and tour management API with tests.

---

## Phase 8: Public API

The read-only endpoints used by the visitor-facing frontend.

- [ ] **8.1** `PublicController` (`/api/public`):
  - `GET /gallery` — get the default gallery (code, name, description, headliner, tour list, theme, borderStyle, ad settings)
  - `GET /galleries` — list all **visible** galleries (code, name, description, headliner, theme)
  - `GET /galleries/by-code/{code}` — get a specific gallery by its code (works for both visible and invisible galleries — anyone with the code can access it), includes tours (with the "All Images" tour included or excluded based on `showAllImagesTour`), theme, borderStyle, ad settings
  - `GET /galleries/by-code/{code}/tours/{tourPublicId}` — get tour with ordered images
  - `GET /galleries/by-code/{code}/images/{shortId}` — get a single image by short ID
  - `GET /thumbnails/{shortId}` — serve the thumbnail binary (JPEG) for an image. Served from in-memory cache when available, falls back to database. Returns `Content-Type: image/jpeg`. Used by `<img>` tags for all thumbnail displays. (for direct-link resolution). If the short ID is not found among active images, check the `image_redirect` table: if a redirect exists, return the substitute image with a flag indicating it was redirected. If the short ID is `00000000` or redirects to `00000000`, return a sentinel response that the frontend renders as "Image not available."
  - `GET /themes` — list all available themes (enum values with display names)
  - `GET /config` — public app config (AdSense publisher ID if configured, site domain for shareable link generation)
  - `POST /views` — fire-and-forget view tracking. Accepts `{ type, code, tourPublicId?, imageShortId?, context? }`. Resolves the entity by code/publicId/shortId, performs an UPSERT on `page_view_daily` for today's date. Returns `204 No Content`. Failures are silently swallowed (tracking should never break the visitor experience). For galleries and tours, `context` is always `DIRECT`. For images, `context` is `DIRECT` (from shareable link) or `TOUR` (from tour navigation).
- [ ] **8.2** Response shaping: only include fields needed by the frontend (no internal IDs, no admin-only details). Image responses include `shortId` and `nsfw` flag, but NOT `adminNotes` or `uploadedAt` (use `PublicImageResponse`). Gallery responses include `code` but NOT `visible`. Gallery detail responses include a computed `hasNsfwContent` boolean (true if any image used in the gallery's tours is marked NSFW) — the frontend uses this to decide whether to show the "Hide NSFW images" checkbox.
- [ ] **8.3** Verify: integration tests — public endpoints return correct data, no auth required, invisible galleries excluded from `/galleries` list but accessible via `/galleries/by-code/{code}`, image accessible by short ID

**Milestone**: Public API complete. Backend is fully functional.

---

## Phase 9: Frontend — Theme System & Core Visitor Pages

- [ ] **9.1** Set up React Router, Axios/fetch API client, base layout with persistent navigation component
- [ ] **9.2** Define theme system: CSS custom properties (variables) for each theme, applied via a class on the root gallery layout. Each theme defines: `--bg-primary`, `--bg-secondary`, `--text-primary`, `--text-secondary`, `--accent`, `--card-bg`, `--card-border`, `--heading-font`, and a default image border style. Border styles are implemented as CSS classes (`border-none`, `border-thin-line`, `border-double-line`, `border-shadow`, `border-rounded`, `border-ornate-frame`, `border-polaroid`) that can override the theme default via the gallery's `borderStyle` setting. Themes:
  - **Light** — clean white/gray, dark text, blue accent, sans-serif headings
  - **Dark** — near-black backgrounds, light text, electric blue accent, sans-serif headings
  - **Pastel** — soft lavender/mint/blush backgrounds, muted text, warm pink accent, rounded friendly feel
  - **Spring** — fresh greens, warm yellows, floral accents, light airy backgrounds, serif headings
  - **Winter** — cool slate blues, icy whites, silver accents, crisp sans-serif
  - **Cyberpunk** — deep purple/black, neon magenta and cyan accents, glitch-style monospace headings
  - **Sunset** — warm gradients (peach to coral to deep orange), cream text, gold accent
  - **Ocean** — deep navy to teal, sandy highlights, wave-like accent colors, relaxed serif headings
  - **Monochrome** — pure black and white, no color accents, high-contrast, elegant serif headings
- [ ] **9.3** **Root redirect** (`/`): fetch default gallery, redirect to `/<default-gallery-code>`.
- [ ] **9.4** **Landing Page** (`/:code`): fetch gallery by code, apply its theme. Display gallery headliner image prominently, gallery name, subtitle (if present), description. If biography fields are populated, render a biography section: artist photo (from `bioPhotoUrl`), biographical text (`bioText`, rendered with paragraph breaks), and resource links (`bioLinks`, each as a clickable labeled link). Omit any empty biography fields; omit the entire section if all are empty. Below, list available tours as clickable cards — each card shows the tour's name and a thumbnail of the tour's headliner image (loaded via `/api/public/thumbnails/{shortId}`).
- [ ] **9.5** **Tour Page** (`/:code/tours/:tourPublicId`): display a grid of thumbnails for all images in the tour (loaded via the thumbnail endpoint). Clicking any thumbnail navigates to the image detail view. Navigation breadcrumbs: Gallery > Tour Name.
- [ ] **9.6** **Image Detail Page — tour context** (`/:code/tours/:tourPublicId/:imageShortId`): display full-size image with decorative border (from gallery's borderStyle override, or the theme's default border). Show all image metadata below or beside the image: title, artist name, description, art creation date, artist comments, notes. "Previous" and "Next" buttons on the left and right sides for sequential navigation through the tour. Navigation breadcrumbs: Gallery > Tour Name > Image Title. Display a **shareable link** (`https://<domain>/<code>/<imageShortId>`) with a copy button.
- [ ] **9.7** **Image Detail Page — direct link** (`/:code/:imageShortId`): same image display and metadata as tour context, but no prev/next buttons and no tour breadcrumb. Breadcrumbs: Gallery > Image Title. This is the URL used in shareable links (Discord, social media, etc.). If the API indicates a redirect, navigate to the substitute image's URL. If the short ID is `00000000` or resolves to it, render a grey placeholder box with "Image not available" text.
- [ ] **9.8** **Galleries page** (`/galleries`): list all **visible** galleries. Each card shows the gallery name, code, a thumbnail of the gallery's headliner image, and a visual preview of its theme. Click to browse one (navigates to `/<code>`).
- [ ] **9.9** Persistent navigation & footer: breadcrumb-style nav at the top of every page. Controls to return to the gallery landing page and (when in tour context) to the tour grid. Footer includes a "Browse other galleries" link when there are other visible galleries; hidden if the current gallery is the only visible one.
- [ ] **9.10** **NSFW filter**: if the gallery's `hasNsfwContent` flag is true, display a "Hide NSFW images" checkbox in the gallery header/nav area. When checked, all images with `nsfw: true` are replaced with empty placeholder boxes (same dimensions, neutral background, no image pixels loaded) in both thumbnails and full-size views. All other information (title, metadata, position in tour, shareable link, prev/next navigation) remains visible. Store the visitor's preference in `localStorage` so it persists across page navigations. If `hasNsfwContent` is false, the checkbox is not rendered.
- [ ] **9.10a** **View tracking**: each visitor page (landing, tour, image detail) fires a `POST /api/public/views` after render. The call is fire-and-forget (async, response ignored, errors swallowed). Image detail pages send `context: "TOUR"` when accessed via a tour route and `context: "DIRECT"` when accessed via a direct/shareable link.
- [ ] **9.11** Responsive design: works on desktop and mobile. Tour grid adapts column count. Image detail prev/next buttons reposition for touch.
- [ ] **9.12** Error handling: graceful 404, loading states

**Milestone**: Visitors can browse galleries, tours, and images. Themes and borders render correctly. Shareable links work.

---

## Phase 10: Frontend — AdSense Integration

- [ ] **10.1** Conditionally include `adsbygoogle.js` script (only when publisher ID is configured via `/api/public/config`)
- [ ] **10.2** Reusable `<AdUnit slotId={string}>` React component: renders `<ins class="adsbygoogle">` with `data-ad-client` (publisher ID) and `data-ad-slot` (slot ID), calls `adsbygoogle.push({})` on mount, handles cleanup on route change, uses responsive ad format
- [ ] **10.3** Ad container styled with neutral background so it doesn't clash with gallery themes
- [ ] **10.4** Placement logic reads gallery's ad settings and renders `<AdUnit>` in the appropriate positions:
  - Landing page: banner between gallery description and tour cards (when `adLandingBanner` is true)
  - Image detail: ad in sidebar or below metadata (when `adImageDetailSidebar` is true)
- [ ] **10.5** All ad placements gated on `adsEnabled` — if false, no ad components render

**Milestone**: AdSense ads render in configured placements. No ads shown when disabled.

---

## Phase 11: Frontend — Admin Panel

- [ ] **11.1** Login page (`/admin/login`): password form, store JWT in memory/localStorage
- [ ] **11.2** Protected admin routes: redirect to login if no valid token
- [ ] **11.3** Admin dashboard: overview of galleries, images, tours
- [ ] **11.4** Image management UI: list/search images, upload (URL input), edit all metadata (title, artist name, description, art creation date, artist comments, notes, admin notes), NSFW checkbox, set base image, bulk load form. **Tag management on image page**: display all existing tags as checkboxes for quick tagging (check/uncheck to add/remove tags). Include an inline "Create new tag" field so the admin can create and immediately assign a new tag without leaving the page.
- [ ] **11.5** Gallery management UI: list galleries, create/edit/delete, set default, clone. Gallery form includes: name, subtitle (optional), code (with validation feedback), visibility checkbox ("Visible to visitors"), "Show 'All Images' tour" checkbox, description, headliner image picker. Biography section: artist photo URL field, bio text (multiline textarea), bio links (add/remove/reorder list of label + URL pairs).
- [ ] **11.5a** **Gallery theme & style editor** — when creating or editing a gallery, the admin sees:
  - Theme picker dropdown (all 9 themes)
  - Border style picker (theme default, none, thin line, double line, shadow, rounded, ornate frame, polaroid)
  - **Ad settings panel**:
    - "Enable ads" — master toggle (checkbox). When unchecked, all other ad controls are greyed out.
    - For each placement (landing page banner, image detail sidebar):
      - Checkbox to enable/disable that placement
      - Text field for the AdSense slot ID (numeric string from Google's AdSense dashboard)
      - Slot ID field is required when the placement is enabled; greyed out when disabled
    - No raw JavaScript input — the app constructs standard AdSense `<ins>` tags from the publisher ID + slot IDs only
  - **Live preview panel**: miniature sample renderings of the Landing Page (headliner + tour cards), Tour Grid (thumbnail grid), and Image Detail (full image with border + metadata). Uses bundled sample artwork (from Phase 1.4a). Every change to theme, border, or ad settings instantly updates all three previews — ad placements shown as labeled placeholder boxes in the preview.
- [ ] **11.5b** **Tag management page** (`/admin/tags`): list all tags with the count of images using each tag. Admin can create new tags and delete existing tags (with confirmation — warns that deletion will update all tours associated with this tag).
- [ ] **11.6** Tour management UI: create/edit tours within a gallery, drag-and-drop image ordering, set headliner. **Tag association**: when creating or editing a tour, the admin can select zero or more tags from a checkbox list. When tags are selected at creation, all matching images are auto-populated into the tour. Associated tags are displayed on the tour detail view.
- [ ] **11.6a** **View statistics page** (`/admin/stats`): date range picker (defaults to last 30 days) with three tabs:
  - **Galleries** — sortable table of galleries with total view count and a sparkline/bar showing the daily trend
  - **Tours** — sortable table of tours with view counts, filterable by gallery dropdown
  - **Images** — sortable table of most-viewed images with columns for total views, direct views, and tour views (direct views indicate the image is being shared externally via shareable links)
- [ ] **11.7** Confirmation dialogs for destructive actions. Image deletion dialog requires the admin to select a substitute image (searchable dropdown of existing images, or the "Image not available" option `00000000`) before confirming.

**Milestone**: Admin can manage all content through the browser.

---

## Phase 12: Testing & Quality

- [ ] **12.1** Ensure JaCoCo enforces 80%+ line coverage on backend — add missing tests
- [ ] **12.2** Write end-to-end test suite (Maven `e2e` profile): full scenario as described in CLAUDE.md. Uses the bundled sample artwork images as test fixtures. Must cover: gallery code routing, direct image links via short ID, visibility filtering on `/galleries`, shareable link format correctness, image deletion with redirect (verify old link resolves to substitute), one-hop collapse, `00000000` "image not available" rendering, **tag-tour auto-sync** (create tour with tags → verify auto-populated, add tag to image → verify image appears in matching tours, remove tag from image → verify image removed from tours where it was only included via that tag, delete a tag → verify tours updated).
- [ ] **12.3** OpenAPI spec validation: ensure springdoc generates accurate docs, accessible at `/swagger-ui.html`
- [ ] **12.4** **Frontend — visitor page tests** (React Testing Library): smoke tests for landing page, tour grid, image detail (both tour-context and direct-link variants), galleries list. Verify: theme application, breadcrumb navigation, prev/next image navigation (tour context only), shareable link displayed with copy button, root `/` redirects to default gallery code, direct image link works without tour context, footer "Browse other galleries" link appears/hides based on visible gallery count, galleries page only shows visible galleries. NSFW filter: checkbox appears only when gallery has NSFW content, toggling replaces NSFW images with placeholders (thumbnails and full-size), metadata remains visible, preference persists in localStorage. View tracking: verify each page fires a POST to `/api/public/views` on render, verify image detail sends correct context (`DIRECT` vs `TOUR`), verify tracking failures don't affect page rendering.
- [ ] **12.5** **Frontend — admin panel tests** (React Testing Library + MSW for API mocking). The admin panel is the most complex part of the application and requires the most thorough testing:
  - **Auth flow**: login with correct/incorrect password, JWT stored, redirect to login when token expires or is missing, logout clears token
  - **Image management**: list/search/filter images, add image by URL, bulk load multiple URLs, edit all metadata fields (title, artist, description, art creation date, artist comments, notes, admin notes), toggle NSFW flag, verify admin notes are NOT exposed in public API responses, set/clear base image, add/remove tags, delete image (must supply substitute — test with real substitute and with `00000000`), verify redirect mapping created on delete, verify one-hop collapse (delete a substitute target and confirm all pointers update), verify loop prevention, confirm dialog requires substitute selection before enabling delete button, validation errors shown for invalid input
  - **Gallery management**: create gallery (verify defaults: LIGHT theme, no border override, ads disabled, visible, code validated), edit gallery (name, code, description, headliner), delete gallery with confirmation, clone gallery (verify new gallery has same theme/border/ad settings/visibility and tours, but requires new code), set default gallery (verify previous default is cleared), toggle visibility, code validation (reject invalid formats, reject duplicates)
  - **Theme & style editor**: select each of the 9 themes and verify live preview updates, select each border style and verify preview updates, verify theme + border combination renders correctly in all three preview panes (landing, tour grid, image detail)
  - **Ad settings**: toggle master "Enable ads" checkbox — verify all other ad controls grey out when disabled, enable each placement (landing page banner, image detail sidebar) and verify slot ID field becomes required, enter slot IDs and verify preview shows ad placeholder boxes in correct positions, disable a placement and verify its slot ID field greys out, save and reload — verify ad settings persist
  - **Tag management page**: list all tags with image counts, create a new tag, delete a tag (verify confirmation dialog warns about tour impact), verify deleted tag is removed from all images and tour associations
  - **Image page tag UI**: verify all tags shown as checkboxes, check a tag to add it, uncheck to remove it, create a new tag inline (verify it appears immediately in the checkbox list and is checked), verify tag changes trigger auto-sync to tours (covered more thoroughly in backend integration tests)
  - **Tour management**: create tour within a gallery, add images to a tour, drag-and-drop reorder images (verify sort order persists after save), set tour headliner image, remove images from a tour, delete tour with confirmation, **tag association**: select tags at tour creation and verify matching images are auto-populated, verify associated tags are displayed on tour detail
  - **Live preview fidelity**: verify all three preview panes render with sample artwork, verify preview uses the sample artwork (not real gallery images), verify border style renders correctly on images in the preview, verify ad placeholders appear/disappear as ad settings change
  - **Error handling**: API failure states (network errors, 401 unauthorized, 404 not found, 409 conflict on delete of in-use entity), form validation (required fields, slot ID format)
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
- Phases 1–8 (backend) can be built and tested without any frontend.
- Phases 9–11 (frontend) can be developed in parallel once the API contract (Phase 4) is stable.
- Image short IDs are randomly generated 8-digit numbers. The `@PrePersist` generation with collision retry works identically on both PostgreSQL and H2.
- The plan assumes we build and test incrementally — each phase is usable before the next begins.
