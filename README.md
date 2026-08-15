# CampusLAF — Campus Lost & Found

A mobile Android application that helps university students recover items lost on campus.

**Advanced Topics in App Innovation — Final Project · Reichman University**

Team: Natalie Goldberg, Talia Lewinger, Ghila Coen, Omer Golani

---

## What it does

When someone finds a lost item, they report it through the app — adding a title, description, and category, and tagging where they found it using the phone's GPS. The item appears in a live feed that other students can browse.

If a student thinks an item is theirs, they submit a claim describing something only the owner would know. The finder reviews the claim and approves or rejects it. When a claim is approved, the item is marked as claimed and leaves the feed, so it can only be returned to one person.

## Features

- **Authentication** — Google Sign-In and email/password login (two methods)
- **Feed** — live list of found items from the database, newest first
- **Report an item** — with category selection and GPS location capture
- **Claim & approval flow** — claim an item, and the finder approves or rejects
- **Profile** — view the items you've found and the items you've claimed
- **Firebase** — Cloud Firestore database, Analytics, and Crashlytics

## Built with

- Android Studio (native Android, Java, XML layouts)
- Cloud Firestore (database)
- Firebase Authentication (Google + email/password)
- Firebase Analytics & Crashlytics
- Google Play Services Location (GPS)

## How it's organized

The code separates the user interface from the data layer:

- **Activities** (`MainActivity`, `FeedActivity`, `ReportActivity`, `ItemDetailActivity`, `ProfileActivity`) — the screens
- **Repositories** (`UserRepository`, `ItemRepository`, `ClaimRepository`) — all database reads and writes
- **Models** (`User`, `Item`, `Claim`) — the data stored in Firestore
- **`ItemAdapter`** — displays items in scrolling lists (reused across screens)

Data is stored in three Firestore collections: `users`, `items`, and `claims`.

## Running the project

1. Clone the repository:
   ```
   git clone https://github.com/natalie-bit/CampusLAF.git
   ```
2. Open the project in Android Studio.
3. Add your own `google-services.json` from the Firebase console into the `app/` folder (it is not included in the repository).
4. Register your machine's debug SHA-1 fingerprint in the Firebase console so Google Sign-In works.
5. Run the app on an emulator with the Google Play Store image, or on a physical device.

## Notes

- All data is stored in Cloud Firestore — there is no static/hardcoded data.
- The app uses the phone's GPS to tag where each item was found.
- To test the claim-and-approval flow, sign in with two different accounts: one to report an item, and one to claim it.