# Anchor Parent + Child — browser-only Firebase setup

This repository contains the trusted Cloud Functions backend. The Parent and Child Android apps are separate Firebase Android apps that must share one verified Firebase project and one Firestore database.

No Firebase ChatGPT plugin, Windows app, Android Studio, or local command line is required for the setup below. Use Firebase Console and GitHub in a browser.

## What the code currently expects

- Parent Android package: `com.anchor.parent`
- Child Android package: `com.anchor.child`
- Shared Firestore database ID currently configured in both apps and the backend:
  `ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e`
- Cloud Functions runtime: Node.js 22
- Cloud Functions and Android callable-client region: `us-central1`
- Firebase project ID: **not hardcoded on purpose**. The earlier value `gen-lang-client-0963224775` came from a previous Child-repository emulator configuration and has not been verified as the real production Firebase project.

**Do not deploy until the owner confirms the Firebase project and the Firestore database.** The Firestore database location is permanent. If the database does not already exist, confirm its desired location before creating it; the Functions currently use `us-central1`, and the app/backend region can be aligned to the chosen database location before production deployment.

## 1. Confirm the correct Firebase project

1. Open [Firebase Console](https://console.firebase.google.com/) in your browser and sign in with the Google account that owns the project.
2. Select the existing project that you intend to use for Anchor.
3. Open **Project settings** (gear icon) > **General**.
4. Copy the **Project ID** exactly. Do not use the display name.
5. Keep that Project ID for the GitHub setup in section 6.

If you do not already have a project, create one in Firebase Console. Do not create a second project if your existing Anchor data is already in another project.

## 2. Register the two Android apps

In the confirmed Firebase project, add two Android apps separately:

| App | Android package name |
|---|---|
| Anchor Parent | `com.anchor.parent` |
| Anchor Child | `com.anchor.child` |

The package names are case-sensitive and cannot be changed for a registered Firebase Android app. Download the corresponding `google-services.json` file for each app. Do not use the Parent file for Child or vice versa.

## 3. Enable Google sign-in

1. In Firebase Console, open **Authentication** > **Sign-in method** (or **Sign-in providers**).
2. Enable **Google**.
3. Choose a project support email and save.
4. Before downloading the final config files, add the release signing certificate's SHA-1 fingerprint to each Android app in **Project settings** > **Your apps**. Section 7 explains how to obtain the fingerprint through GitHub Actions without installing software on the laptop.
5. Download a fresh `google-services.json` for each Android app after the SHA-1 and Google provider are configured. The file should contain the Web OAuth client used by Google sign-in.

Firebase's official setup guide: https://firebase.google.com/docs/auth/android/google-signin

## 4. Check the shared Firestore database

Open **Build** > **Firestore Database** in Firebase Console.

- First check whether the database ID below already exists:
  `ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e`
- If it exists in the confirmed project, keep it. Both apps and the backend are already aligned to this ID.
- If it does not exist, **pause before creating anything and send the project ID plus a screenshot of the Firestore page**. The database location cannot be changed later, and we should align the Functions region before creation. The current Functions region is `us-central1`; for users in Bangladesh, an Asia region may be preferable if no database exists yet.
- If Firebase rejects the current long database ID, do not invent a different ID in the console. Send the error; the same new ID must be changed consistently in both Android apps, `firebase.json`, Firestore rules deployment, and the Admin SDK backend.

If we confirm a new database is needed, use **Add database** > **Standard edition** > **Native mode**, enter the agreed database ID and location, and choose **Production mode**. Do not use Test mode for production. The CI suite already tests the checked-in Firestore rules against the emulator; the deployment workflow publishes those rules to the selected production database.

Official database guide: https://firebase.google.com/docs/firestore/manage-databases

## 5. Enable billing and required services

Cloud Functions deployment requires the Firebase project to be on the **Blaze** plan. In Firebase Console, open **Usage and billing** and link a billing account. Set a budget alert in Google Cloud Billing so unexpected usage is visible. A budget alert is not a hard spending cap.

The deployment uses Cloud Functions for Firebase (2nd gen), Cloud Build, Artifact Registry, Eventarc, Cloud Run, Firestore, and Firebase Authentication. Enable any API that the console or deployment workflow reports as missing.

Official Functions guide: https://firebase.google.com/docs/functions/get-started

## 6. Configure GitHub Actions from the browser

Do this only after section 1 confirms the real Firebase Project ID.

### Anchor Parent repository

Open: https://github.com/alfredrasel721/anchor-parent/settings/secrets/actions

Under **Variables**, add:

- `FIREBASE_PROJECT_ID` = the exact Project ID copied from Firebase Console.

Under **Secrets**, add:

- `FIREBASE_SERVICE_ACCOUNT` = the complete JSON key for the deployment service account.
- `FIREBASE_GOOGLE_SERVICES_JSON` = the complete Parent app's `google-services.json`.
- `ANDROID_UPLOAD_KEYSTORE_BASE64` = the release keystore encoded as Base64.
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD` = the keystore password.
- `ANDROID_UPLOAD_KEY_PASSWORD` = the key password.

### Anchor Child repository

Open: https://github.com/alfredrasel721/Anchor-Child/settings/secrets/actions

Under **Variables**, add:

- `FIREBASE_PROJECT_ID` = the same exact Firebase Project ID.

Under **Secrets**, add:

- `FIREBASE_GOOGLE_SERVICES_JSON` = the complete Child app's `google-services.json`.
- `ANDROID_UPLOAD_KEYSTORE_BASE64` = the Child release keystore encoded as Base64.
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD` = the keystore password.
- `ANDROID_UPLOAD_KEY_PASSWORD` = the key password.

The release Gradle configuration currently expects the keystore alias `upload`. If the existing keystore uses a different alias, update the Gradle configuration before building.

**Never paste a service-account JSON key, keystore, or keystore password into ChatGPT or commit them to Git.** Store them only as GitHub Actions secrets. The Firebase `google-services.json` file contains app/project identifiers and OAuth client configuration; this repository's release workflow reads it from a GitHub secret rather than committing it.

### Create the deployment service account

In [Google Cloud Console — Service Accounts](https://console.cloud.google.com/iam-admin/serviceaccounts):

1. Select the same verified Firebase project.
2. Create a service account for GitHub Actions deployment.
3. Grant the deployment identity the required roles. Firebase documents **Cloud Functions Admin** (`roles/cloudfunctions.admin`) and **Service Account User** (`roles/iam.serviceAccountUser`) for deploying Functions. Also grant the Firebase Rules permissions needed to publish Firestore rules (for example, Firebase Rules Admin) and only the additional permissions the deployment reports as missing.
4. Create a JSON key and save the downloaded file securely.
5. Put the full JSON contents into the Parent repository's `FIREBASE_SERVICE_ACCOUNT` Actions secret.

Do not grant project Owner to a long-lived service account unless absolutely necessary. If a deployment permission is missing, add the specific documented role rather than broadening access blindly.

## 7. Get signing fingerprints without installing anything

After the existing release keystore secrets have been added to each repository:

1. Open the repository's **Actions** tab.
2. Select **Android signing certificate fingerprints**.
3. Click **Run workflow**, choose branch `main`, and enter exactly `PRINT`.
4. Copy the displayed **SHA-1** and **SHA-256** fingerprints.

Use the SHA-1 fingerprint for Google sign-in in Firebase Project settings > Your apps. Use the SHA-256 fingerprint when registering the Android apps in **App Check** with **Play Integrity**. If Google Play App Signing is enabled, also add the Play App Signing certificate fingerprint shown in Play Console.

The apps install the debug App Check provider in debug builds and Play Integrity in release builds. Register both apps in App Check before considering enforcement. Do not enable enforcement until real release builds have been tested successfully.

App Check guide: https://firebase.google.com/docs/app-check/android/play-integrity-provider

## 8. Deploy the trusted backend

After both pull requests are merged to `main`, the project variable and service-account secret are configured, billing is enabled, and the Firestore database has been verified:

1. Open the Parent repository's **Actions** tab.
2. Select **Deploy Anchor Firebase backend**.
3. Click **Run workflow** on branch `main`.
4. Enter exactly `DEPLOY` in the confirmation field.
5. Run it and wait for the deployment job to succeed.

The workflow checks that the service-account project's `project_id` matches the `FIREBASE_PROJECT_ID` variable. It deploys only the Firestore rules and Cloud Functions. It does not create projects/databases, enable billing, or configure Google sign-in.

## 9. Build signed release files

After the correct config files and signing secrets are in place:

1. In each repository, open **Actions**.
2. Select **Build signed Android release**.
3. Run on branch `main` and enter exactly `BUILD_RELEASE`.
4. Download the resulting signed APK and AAB artifacts.

The release workflow checks that the Firebase config matches the verified project ID, the correct Android package, and a Web OAuth client. It also prints the signing certificate fingerprints. Keep the same release key for all future updates to that app.

## 10. Final physical-device acceptance test

Install Parent and Child on separate Android devices and verify:

- Parent Google sign-in and household creation.
- Parent issues a six-digit pairing code; Child redeems it once.
- Expired, reused, revoked, and throttled codes are rejected.
- Parent sees the paired Child device.
- Parent unpairs the Child; Child loses household access and must pair again.
- Firestore denies unpaired Child access and client-side writes to pairing secrets/membership.
- SOS notification delivery and other enabled family features work on physical devices.

## What is already prepared in GitHub

- Parent and Child CI workflows build debug APKs and run Android unit tests.
- Firestore emulator security tests run in CI using the local-only project ID `demo-anchor-local`.
- Parent CI checks Cloud Functions syntax and dependencies.
- Debug APKs are uploaded as workflow artifacts.
- A guarded Firebase deployment workflow is prepared in the Parent repository.
- Browser-run keystore fingerprint and signed-release workflows are prepared in both repositories.
- The default Firebase project alias was removed so an unverified project cannot be selected accidentally.
