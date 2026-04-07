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

## Key Domain Rules

**Galleries** have a unique code (`^[A-Z][A-Z0-9]{0,4}$`) used in all visitor URLs. At most one gallery is the default (visitors arriving at `/` redirect to `/<code>`). Galleries can be visible or invisible (invisible = accessible only by direct URL). Each gallery has a theme (9 options), optional border style override, optional biography section, and per-gallery ad settings.

**Images** are a global resource (not owned by a gallery). The app stores only the image URL — files are hosted externally. Each image has an immutable randomly-generated 8-digit **short ID** (e.g., `00004719`) used in shareable links: `https://<domain>/<code>/<shortId>`. All entities use UUID `public_id` in APIs — internal auto-increment PKs are never exposed.

**Image metadata**: title, artist name, description, art creation date (freeform string), artist comments, notes, admin notes (admin-only), upload timestamp (admin-only), NSFW flag, optional base image reference (self-referential). Tags are managed via checkboxes on the admin image page.

**Image deletion** requires a substitute short ID (or `00000000` = "image not available"). Creates a redirect so old shareable links still work. Rules: one hop only (no chaining), no loops.

**Thumbnails**: fixed 100x100px max (scale up if smaller), JPEG, stored in DB, cached in-memory (Caffeine).

**NSFW filter**: if a gallery has NSFW images, visitors see a "Hide NSFW" checkbox. When checked, NSFW images are replaced with placeholders (no pixels loaded), but metadata remains visible. Preference stored in localStorage.

**Tours** are ordered sets of images within a gallery. Tours can be associated with tags — matching images are auto-added at creation, and ongoing sync keeps them updated (add/remove tag on image updates matching tours). Each gallery has a virtual "All Images" tour (toggleable).

**Ads**: Google AdSense publisher ID set via env var. Per-gallery: master toggle, landing page banner slot, image detail sidebar slot. No raw JS — `<AdUnit>` component uses publisher ID + slot ID only.

**View statistics**: the frontend fires a POST to `/api/public/views` after each page render (fire-and-forget). Views are stored as daily aggregated counts per entity (gallery, tour, image) with a context field (`DIRECT` vs `TOUR` for images). Admin stats page shows most-viewed galleries/tours/images over a date range, with a direct-vs-tour breakdown for images (reveals which images are shared externally). Daily granularity kept indefinitely.

## Visitor URL Structure

- `/` → redirect to `/<default-code>`
- `/<code>` → gallery landing page
- `/<code>/tours/<tourPublicId>` → tour grid
- `/<code>/tours/<tourPublicId>/<shortId>` → image detail (with prev/next nav)
- `/<code>/<shortId>` → image detail (direct link, no tour context)
- `/galleries` → visible gallery listing
