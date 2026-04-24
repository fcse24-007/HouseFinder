# HouseFinder XML UI Scaffold

This scaffold gives you editable XML screens and sample fragment wiring so you can continue with Android Studio drag-and-drop.

## What is included

- Single-activity setup with Navigation Component host in `app/src/main/res/layout/activity_main.xml`
- Navigation graph in `app/src/main/res/navigation/nav_graph.xml`
- Authentication flow:
  - `app/src/main/java/com/example/housefinder/ui/auth/LoginFragment.kt`
  - `app/src/main/java/com/example/housefinder/ui/auth/RegisterFragment.kt`
  - XML: `fragment_login.xml`, `fragment_register.xml`
- Listings flow:
  - `ListingListFragment`, `ListingDetailFragment`, `ListingAdapter`
  - XML: `fragment_listing_list.xml`, `fragment_listing_detail.xml`, `item_listing.xml`
- Preferences + smart alert check:
  - `PreferencesFragment`
  - XML: `fragment_preferences.xml`
- Simulated deposit + reservation:
  - `PaymentFragment`, `ReservationSuccessFragment`
  - XML: `fragment_payment.xml`, `fragment_reservation_success.xml`
- Session helper:
  - `app/src/main/java/com/example/housefinder/ui/common/SessionManager.kt`

## Notes for refinement

- Login currently validates with email + bcrypt against `UserDao.getByEmail`.
- Listing filters support `price`, `location`, `type`, and `availabilityDate`.
- Payment is simulated and writes to `Reservation` + `Receipt`.
- You can now open each XML and redesign with drag-and-drop while keeping IDs unchanged.

## Verified build

- `:app:assembleDebug` completed successfully after scaffolding.

