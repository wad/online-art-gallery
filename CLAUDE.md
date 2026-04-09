# Online Art Gallery

A web application for presenting art in curated galleries with tours, themes, and optional AdSense ads. See `PLAN.md` for the phased implementation plan with full schema and API details.

## Tech Stack

- **Frontend**: React + TypeScript + Vite
- **Backend**: Java 21, Spring Boot 3, JPA/Hibernate, MapStruct (compile-time DTOs)
- **Database**: PostgreSQL 16, Liquibase XML changesets
- **Auth**: Stateless JWT (JJWT), single admin password (bcrypt)
- **Build**: Maven multi-module (`frontend` + `backend`), single deployable JAR
- **Package base**: `org.wadhome.oag`
- **Tests**: JUnit 5 + H2 (`MODE=PostgreSQL`), JaCoCo 80%+, React Testing Library + MSW, e2e via Maven profile
- **Deployment**: Docker Compose on EC2, JVM heap capped at 512MB

## API Conventions

**Versioning**: all API paths are prefixed with `/api/v1/`. Admin endpoints under `/api/v1/admin/`, public under `/api/v1/public/`, auth under `/api/v1/auth/`.

**Public IDs in URLs, not request bodies.** Entity references in URL paths use the entity's UUID `publicId`. Request bodies never contain a `publicId` for the entity being created or updated — it's always in the path. When a request body references *another* entity (e.g., setting a headliner image), use that entity's `publicId` as a field in the body (named `<entity>PublicId`, e.g., `headlinerImagePublicId`). **Exception**: bulk operations (e.g., `PUT /bulk-metadata`) that act on multiple entities include `publicId` per item in the body, since the targets can't be expressed in the URL.

**Pagination envelope** (for all paginated endpoints):
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8
}
```
Page is zero-indexed. Default size is 20. Query params: `page`, `size`.

**Error responses** use Spring's RFC 7807 `ProblemDetail` format:
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Gallery code must match ^[A-Z][A-Z0-9]{0,4}$",
  "instance": "/api/v1/admin/galleries"
}
```
Validation errors include a `fieldErrors` extension with per-field messages.

**Naming conventions**:
- Java: standard camelCase fields, PascalCase classes. Entities in `org.wadhome.oag.entity`, repositories in `.repository`, services in `.service`, controllers in `.controller`, DTOs in `.dto`, mappers in `.mapper`, enums in `.entity` alongside their owning entity.
- REST endpoints: plural nouns (`/images`, `/galleries`, `/tours`), kebab-case for multi-word paths (`/bulk-metadata`, `/regenerate-thumbnail`).
- Database: snake_case tables and columns. Liquibase changeset IDs: `<table>-create`, `<table>-add-<column>`, etc.
- Frontend: PascalCase components, camelCase functions/variables, kebab-case CSS classes. One component per file. API client functions in `src/api/`.

**Standard responses**: `201 Created` for resource creation (with `Location` header), `204 No Content` for deletes and fire-and-forget, `200 OK` for reads and updates, `202 Accepted` for async operations. Bulk operations return `200` with per-item results.

**JWT tokens** expire after 24 hours. There is no refresh token — when the token expires, the admin re-authenticates via the login page. The frontend detects 401 responses and redirects to login.

**Thumbnail URLs** are always `/api/v1/public/thumbnails/{shortId}`. Admin image list responses include this as `thumbnailUrl` so the frontend can render thumbnails without constructing URLs.

## Key Domain Rules

**Galleries** have a unique code (`^[A-Z][A-Z0-9]{0,4}$`) used in all visitor URLs. At most one gallery is the default (visitors arriving at `/` redirect to `/<code>`). Galleries can be visible or invisible (invisible = accessible only by direct URL). Each gallery has a theme (9 options), optional border style override, optional biography section, per-gallery ad settings, and an admin-defined sort order controlling display on the `/galleries` page and in the admin list.

**Images** are a global resource but are scoped to galleries via a `gallery_image` association table. Each gallery only sees its own images. When creating an image, a gallery can optionally be specified — if provided, the image is immediately associated with that gallery. Images not assigned to any gallery are "orphans" — the admin panel highlights these. Each image has an immutable randomly-generated 8-digit **short ID** (e.g., `00004719`) used in shareable links: `https://<domain>/<code>/<shortId>`. All entities use UUID `public_id` in APIs — internal auto-increment PKs are never exposed. The admin can update an image's URL, which triggers thumbnail regeneration.

**Image metadata**: title, artist name, description, art creation date (freeform string), artist comments, notes, admin notes (admin-only), upload timestamp (admin-only), NSFW flag, optional base image reference (self-referential). Tags are managed via checkboxes on the admin image page.

**Image deletion** requires a substitute short ID (or `00000000` = "image not available"). Creates a redirect so old shareable links still work. Rules: one hop only (no chaining), no loops.

**Thumbnails**: fixed 100x100px max (scale up if smaller), JPEG, stored in DB, cached in-memory (Caffeine). Generated on image creation; admin can regenerate per-image or all at once (async with progress polling). If thumbnail generation fails (unreachable URL), the error is shown to the admin. During bulk load, failures skip to the next image — unless the first three consecutive images all fail, in which case the bulk load aborts.

**NSFW filter**: if a gallery has NSFW images, visitors see a "Hide NSFW" checkbox. When checked, NSFW images are replaced with placeholders (no pixels loaded), but metadata remains visible. Preference stored in localStorage.

**Tours** are ordered sets of images within a gallery. Tours have a name, description, headliner, and an admin-defined sort order within their gallery. Tours can be associated with tags — matching images are auto-added at creation, and ongoing sync keeps them updated (add/remove tag on image updates matching tours). Each gallery has a virtual "All Images" tour (toggleable). The tour grid displays thumbnails 5-wide.

**Ads**: Google AdSense publisher ID set via env var. Per-gallery: master toggle, landing page banner slot, image detail sidebar slot. No raw JS — `<AdUnit>` component uses publisher ID + slot ID only.

**View statistics**: the frontend fires a POST to `/api/v1/public/views` after each page render (fire-and-forget, rate-limited per IP). Views are stored as daily aggregated counts per entity (gallery, tour, image) with a context field (`DIRECT` vs `TOUR` for images). Admin stats page shows most-viewed galleries/tours/images over a date range, with a direct-vs-tour breakdown for images (reveals which images are shared externally). Daily granularity kept indefinitely.

**Admin panel**: uses a fixed simplistic color scheme (light gray/white/blue) — never applies gallery themes. Supports password change (min 8 characters), paginated image list with thumbnails, image search/filter (text + tags + NSFW), sortable columns, bulk metadata editing, image usage report ("which tours use this image?"), orphan image detection, and gallery configuration export/import (JSON).

**Auth**: default admin password is `WorstPassword666!` (documented in README). Passwords must be at least 8 characters; no other rules.

## Visitor URL Structure

- `/` → redirect to `/<default-code>`
- `/<code>` → gallery landing page
- `/<code>/tours/<tourPublicId>` → tour grid
- `/<code>/tours/<tourPublicId>/<shortId>` → image detail (with prev/next nav)
- `/<code>/<shortId>` → image detail (direct link, no tour context)
- `/galleries` → visible gallery listing
