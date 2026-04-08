# Online Art Gallery

A web application for presenting art galleries with curated tours, multiple themes, and optional Google AdSense integration.

## Prerequisites

- **Java 21** (for building)
- **Maven 3.9+**
- **Docker** and **Docker Compose** (for running PostgreSQL and the application)
- **Node.js 20+** (installed automatically by the Maven build, but required if developing the frontend separately)

## Building

The project builds into a single deployable JAR that includes both the backend API and the React frontend.

```bash
mvn clean package
```

This will:
1. Install Node.js and npm (via `frontend-maven-plugin`)
2. Build the React frontend
3. Compile the Java backend
4. Copy the frontend build output into the JAR's static resources
5. Run unit and integration tests
6. Produce `backend/target/oag.jar`

To skip tests during the build:

```bash
mvn clean package -DskipTests
```

## Database Setup

The application uses PostgreSQL 16. The schema is managed automatically by Liquibase on startup — you do not need to create tables manually.

### Option 1: Docker Compose (Recommended)

The included `docker-compose.yml` runs both PostgreSQL and the application:

```bash
docker-compose up -d
```

This creates a `oag-db` container with PostgreSQL and an `oag-app` container with the application. The database data is persisted in a Docker volume.

### Option 2: External PostgreSQL

If you prefer to run PostgreSQL outside of Docker (e.g., Amazon RDS, an existing server), create an empty database and provide the connection details via environment variables:

```bash
export OAG_DB_HOST=your-db-host
export OAG_DB_PORT=5432
export OAG_DB_NAME=oag
export OAG_DB_USERNAME=oag
export OAG_DB_PASSWORD=your-secure-password
```

Then run the application JAR directly:

```bash
java -Xmx512m -jar backend/target/oag.jar
```

Liquibase will create all tables on first startup.

### Database Backups

Set up a cron job to back up the database regularly. If using the Docker Compose setup:

```bash
# Add to crontab (e.g., daily at 2 AM)
0 2 * * * docker exec oag-db pg_dump -U oag oag > /path/to/backups/oag-$(date +\%Y\%m\%d).sql
```

To restore from a backup:

```bash
cat /path/to/backups/oag-20260407.sql | docker exec -i oag-db psql -U oag oag
```

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `OAG_DB_HOST` | Yes | `localhost` | PostgreSQL hostname |
| `OAG_DB_PORT` | No | `5432` | PostgreSQL port |
| `OAG_DB_NAME` | Yes | `oag` | Database name |
| `OAG_DB_USERNAME` | Yes | `oag` | Database user |
| `OAG_DB_PASSWORD` | Yes | | Database password |
| `OAG_JWT_SECRET` | Yes | | Secret key for signing JWT tokens (use a long random string) |
| `OAG_SITE_DOMAIN` | Yes | | The public domain of the site (e.g., `https://example.com`). Used to generate shareable image links. |
| `OAG_ADSENSE_PUBLISHER_ID` | No | | Google AdSense publisher ID (`ca-pub-XXXXX`). If not set, all ad functionality is disabled. |

## Image Hosting

The application stores only the URL of each image — it does not serve image files itself. You need to host your images separately and provide URLs when adding them through the admin panel.

### Option A: Static Web Server (Apache2, Nginx)

The simplest approach. Place image files in a directory served by your web server.

**Apache2 example** (on the same server as the application):

```apache
# In your Apache site config for your domain:

# Serve images directly from the filesystem — bypass the app
ProxyPass /images !
Alias /images /var/www/your-site/images

<Directory /var/www/your-site/images>
    Options -Indexes
    AllowOverride None
    Require all granted
</Directory>

# Proxy everything else to the Spring Boot app
ProxyPass / http://localhost:8080/
ProxyPassReverse / http://localhost:8080/
```

Then place your images in `/var/www/your-site/images/` and add them in the admin panel using URLs like `/images/painting-name.jpg`.

**Nginx example:**

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /images/ {
        alias /var/www/your-site/images/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Option B: Amazon S3 + CloudFront

Better for scalability, global distribution, and if you don't want to manage image files on your server.

1. **Create an S3 bucket:**

   ```bash
   aws s3 mb s3://your-gallery-images
   ```

2. **Configure the bucket for public read access** (for serving images directly) or use a CloudFront distribution with an Origin Access Identity for private buckets.

3. **Upload images:**

   ```bash
   aws s3 cp painting.jpg s3://your-gallery-images/painting.jpg \
     --content-type image/jpeg \
     --cache-control "public, max-age=2592000"
   ```

4. **Add images in the admin panel** using the full S3/CloudFront URL:
   - Direct S3: `https://your-gallery-images.s3.amazonaws.com/painting.jpg`
   - CloudFront (recommended): `https://d1234abcd.cloudfront.net/painting.jpg`

**CloudFront setup** (recommended for production):

- Create a CloudFront distribution pointing to your S3 bucket as the origin
- This provides caching at edge locations worldwide, HTTPS, and custom domain support
- Typical cost: a few dollars/month for a site with ~25K visits/month

### Option C: Any Other CDN or Hosting

Since the application only stores URLs, you can host images anywhere accessible via HTTP/HTTPS: DigitalOcean Spaces, Cloudflare R2, Backblaze B2, Google Cloud Storage, or any web-accessible location. Just provide the full URL when adding images in the admin panel.

## Deployment

### Single Server with Docker Compose

This is the recommended setup for a small-to-medium site.

1. **Copy files to your server:**

   ```bash
   scp backend/target/oag.jar docker-compose.yml .env your-server:/opt/oag/
   ```

2. **Create a `.env` file** on the server with your environment variables:

   ```bash
   OAG_DB_HOST=oag-db
   OAG_DB_PORT=5432
   OAG_DB_NAME=oag
   OAG_DB_USERNAME=oag
   OAG_DB_PASSWORD=your-secure-db-password
   OAG_JWT_SECRET=your-long-random-secret-string
   OAG_SITE_DOMAIN=https://example.com
   # Optional:
   # OAG_ADSENSE_PUBLISHER_ID=ca-pub-XXXXX
   ```

3. **Start the application:**

   ```bash
   cd /opt/oag
   docker-compose up -d
   ```

4. **Configure your reverse proxy** (Apache2 or Nginx) to forward traffic to port 8080. See the image hosting section above for example configs.

5. **Verify:**
   - Visit your domain — you should see the landing page
   - Visit `/admin` — you should see the login page
   - Log in with the default admin password (see below)

### Updating

To deploy a new version:

```bash
# Build the new JAR
mvn clean package

# Copy to server
scp backend/target/oag.jar your-server:/opt/oag/

# Restart the app container (database stays running)
ssh your-server "cd /opt/oag && docker-compose restart oag-app"
```

Liquibase will automatically apply any new database migrations on startup.

## Admin Guide

### First Login

1. Navigate to `/admin`
2. Log in with the default password: **`WorstPassword666!`**
3. **Change the password immediately** — go to the password change page (accessible from the admin navigation bar) and set a secure password. Passwords must be at least 8 characters long.

### Changing the Admin Password

1. Click the password/account link in the admin navigation bar
2. Enter your current password
3. Enter and confirm your new password (minimum 8 characters)
4. Click "Change Password"

If you forget your password, you will need to reset it directly in the database by updating the `password_hash` column in the `admin_config` table with a new bcrypt hash.

### Setting Up a Gallery

1. **Add images** — Go to image management and either add images one at a time (by URL) or use bulk load to add many at once. The admin image list is paginated and shows thumbnails, making it easy to browse your collection. Fill in metadata (title, artist, description, etc.) afterward — you can also use bulk metadata editing to update multiple images at once. Each image is automatically assigned a permanent eight-digit short ID (e.g., `00004719`). There is also an "Admin notes" field visible only to admins — use it for internal notes that visitors should not see.
2. **Create a gallery** — Give it a name, a unique gallery code (1-5 characters, starting with a capital letter, e.g., `MAIN`, `FF25`), a description, choose a theme, and optionally customize the image border style. Use the live preview to see how it will look. Thumbnails (100x100px max) are automatically generated when images are added to the system.
3. **Add images to the gallery** — In the gallery editor, use the image management panel to add images from the global pool to this gallery. An image can belong to multiple galleries. Tours within the gallery will only see images that have been added to it.
4. **Set visibility** — By default, galleries are visible to all visitors. Uncheck "Visible to visitors" to hide a gallery from the galleries listing page. Visitors can still access it via direct URL if they know the gallery code. This is useful for testing a new gallery before making it public.
5. **Create tours** — Within your gallery, create tours. Each tour has a name, optional description, a headliner image, and an ordered set of images drawn from the gallery. You can optionally associate tags with a tour to auto-populate it with matching images.
6. **Set the default gallery** — Mark one gallery as the default. When visitors arrive at the site root, they are redirected to this gallery's page (e.g., `https://example.com/MAIN`).
7. **Reorder galleries and tours** — Drag and drop galleries and tours to set their display order. This controls the order visitors see on the galleries page and on each gallery's landing page.

### Managing Images

- **Search and filter** — Use the search bar to find images by title, artist, or description. Filter by tag or NSFW status. Sort by title, artist name, or upload date.
- **Update image URL** — If an image moves to a new hosting location, edit the image and update its URL. The thumbnail will be automatically regenerated.
- **Regenerate thumbnails** — Use the "Regenerate thumbnail" button on any image's edit page, or use "Regenerate all thumbnails" from the image list toolbar.
- **Check image usage** — Each image's edit page shows which galleries and tours reference it, helping you understand the impact before making changes.
- **Export/import galleries** — Export a gallery's configuration (tours, image references, settings) as a JSON file. Import it on another instance or as a template for a new gallery.

### Shareable Image Links

Every image displays a shareable link in the format `https://<your-domain>/<gallery-code>/<image-short-id>` (e.g., `https://example.com/MAIN/00004719`). Visitors can copy this link and share it in Discord, social media, or anywhere else. The link takes the recipient directly to that image within the specified gallery, with the gallery's theme applied. Make sure `OAG_SITE_DOMAIN` is set correctly for these links to work.

### Viewing Statistics

The admin panel includes a **Statistics** page (`/admin/stats`) that shows which galleries, tours, and images are getting the most views. Select a date range and browse three tabs:

- **Galleries** — total views per gallery with daily trend
- **Tours** — views per tour, filterable by gallery
- **Images** — most-viewed images, with a breakdown of direct views (from shareable links) vs tour views (from browsing). A high direct view count means the image is being shared externally.

Views are tracked automatically — no setup required.

### Google AdSense Setup

1. Set the `OAG_ADSENSE_PUBLISHER_ID` environment variable to your publisher ID (`ca-pub-XXXXX`) and restart the application.
2. In the admin panel, edit a gallery and open the ad settings section.
3. Check "Enable ads" to activate advertising for that gallery.
4. For each ad placement you want, check the box and enter the ad slot ID from your AdSense dashboard.
5. Use the live preview to see where ads will appear.

Ad placements available:
- **Landing page banner** — between gallery description and tour cards
- **Image detail sidebar** — beside or below image metadata

## Running Tests

```bash
# Unit and integration tests
mvn test

# End-to-end tests (requires a running instance)
mvn test -Pe2e -Doag.base-url=http://localhost:8080
```

## API Documentation

When the application is running, OpenAPI documentation is available at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
