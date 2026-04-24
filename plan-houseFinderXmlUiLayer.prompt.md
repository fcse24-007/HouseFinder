## Plan: XML UI Layer for HouseFinder

This draft builds a complete XML-first UI architecture on top of your existing Room data model, starting from authentication and role-based flows, then listings, filters, alerts, payments, and reservations. It keeps your current database (`AppDatabase`, DAOs, entities) as the source of truth, introduces Fragments + Navigation + RecyclerView screens, and aligns required app behavior (email login, account creation, smart alerts, deposit simulation, and room reservation) with your existing `User`, `Listing`, `UserPreference`, `Receipt`, and `Reservation` models.

### Steps
1. Define app navigation graph and role-based start flow in [AndroidManifest.xml](app/src/main/AndroidManifest.xml), [activity_main.xml](app/src/main/res/layout/activity_main.xml), and new `nav_graph.xml` with `MainActivity` host.
2. Add authentication UI screens (XML + Fragment) for email login and account creation in `ui/auth/*`, wired to `UserDao.login` and role routing.
3. Build listings home and detail screens using RecyclerView XML layouts in `ui/listings/*`, backed by `ListingDao.getAllAvailable`, `ListingImageDao.getCoverImage`, and `ListingDao.getById`.
4. Implement filter bottom sheet/dialog XML for price, location, and availability date; map inputs to `ListingDao.filter` and extend query for date condition.
5. Create preferences and smart alerts UI in `ui/preferences/*` tied to `UserPreferenceDao.upsert`, then trigger alerts from matching `ListingDao.findNewMatchingListings`.
6. Design deposit payment + reservation flow screens in `ui/payment/*` and `ui/reservation/*`, chaining simulated payment -> `ReceiptDao.insert` -> `ReservationDao.insert` -> status update.

### Further Considerations
1. Login key decision: keep `studentId`-based auth or switch `UserDao.login` to email/password hash? Option A email-only / Option B both accepted / Option C role-specific.
2. Availability handling: use `Listing.availabilityDate` exact date or “available on/before selected date” semantics for filter and alerts?
3. Reservation transaction boundary: enforce atomic payment-reserve operation in one use-case to prevent paid-without-reservation edge cases?
