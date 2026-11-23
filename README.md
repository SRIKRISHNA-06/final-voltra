# VOLTRA: EV charging marketplace (dark neon interface)

Same Spring Boot backend, new frontend (`src/main/resources/static`): a dark teal and neon-green design with a sidebar app shell, role-specific dashboards (driver, host, admin), and a map-and-list charger search.

## Run locally (H2, seeded data)
1. Double-click `RUN_DEV_LOCAL.bat`
2. Open http://localhost:8080
   * Driver: `driver@voltra.in` / `DriverPass123`
   * Host: `host@voltra.in` / `HostPass123`
   * Admin: `admin@voltra.in` / `AdminPass123`

   On localhost, the sign-in page shows quick-fill buttons for these accounts.

## Production (Docker Compose: app + PostgreSQL + Redis)
Set real values for `JWT_SECRET`, `DB_PASSWORD`, `DEVICE_API_KEY`, `ADMIN_BOOTSTRAP_PASSWORD`
(and the Razorpay keys if used) before `docker-compose up --build`. The defaults in `docker-compose.yml` are for development only.

## Notes on the UI
* Only `index.html`, `styles.css` and `app.js` are served publicly (see `SecurityConfig`). Keep those filenames.
* The Content-Security-Policy blocks web fonts, so the UI uses system fonts. The car, charger and skyline art is inline SVG, so no images are needed. Map scripts and tiles load from unpkg and OpenStreetMap, which the CSP allows.

* The login background is `src/main/resources/static/images/login-bg.webp`. Replace it with your own image (same name) to change it. The server allows files in `/images/` (see `SecurityConfig`).
