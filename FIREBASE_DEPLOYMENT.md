# Anchor Parent + Child — browser-only Firebase setup

This repository contains the trusted Cloud Functions backend. The Parent and Child Android apps are separate Firebase Android apps that must share one verified Firebase project and one Firestore database.

No Firebase ChatGPT plugin, Windows app, Android Studio, or local command line is required for the setup below. Use Firebase Console and GitHub in a browser.

## What the code currently expects

- Parent Android package: `com.anchor.parent`
- Child Android package: `com.anchor.child`
- Shared Firestore database ID currently configured in both apps and the backend:
  `ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e`
- Cloud Functions runtime: Node.js 22
- Firestore database location: `asia-south1` (owner-confirmed existing database)
- Cloud Functions and Android callable-client region: `asia-south1`
- Firebase project ID: `gen-lang-client-0963224775` (provided by the owner in this conversation and now configured as the default target in both repositories).

The owner has confirmed that this project already contains the shared Firestore Native database in `asia-south1`. Both Android apps and the backend are now configured to use that database ID, and the callable Functions plus Firestore trigger are configured for `asia-south1`. **Do not create another database.**

## 1. Confirm the correct Firebase project

1. Open [Firebase Console](https://console.firebase.google.com/) in your browser and sign in with the Google account that owns the project.
2. Select the project whose ID is `gen-lang-client-0963224775`.
3. Open **Project settings** (gear icon) > **General** and confirm the Project ID matches exactly. Do not use the display name.
4. Both repositories are now pinned to this confirmed project; you do not need to create a GitHub `FIREBASE_PROJECT_ID` variable.

If you do not already have a project, create one in Firebase Console. Do not create a second project if your existing Anchor data is already in another project.

## 2. Register the two Android apps

In the confirmed Firebase project, add two Android apps separately:

| App | Android package name |
|---|---|
| Anchor Parent | `com.anchor.parent` |
| Anchor Child | `com.anchor.child` |

The package names are case-sensitive. The uploaded configuration contained both Android clients in each file; I separated them and added the correct app-specific configuration to each repository at `app/google-services.json`. Parent's repository contains only `com.anchor.parent`; Child's contains only `com.anchor.child`. These are client-side Firebase configuration files, not service-account credentials.

## 3. Enable Google sign-in

1. In Firebase Console, open **Authentication** > **Sign-in method** (or **Sign-in providers**).
2. Enable **Google**.
3. Choose a project support email and save.
4. The uploaded configuration already includes a Web OAuth client for Google sign-in. The Android OAuth certificate hashes currently in the files may not match the release signing key that will be used for the final APK. After the release keystore is configured, add its SHA-1 fingerprint to each Firebase Android app in **Project settings** > **Your apps** (see section 7).

Firebase's official setup guide: https://firebase.google.com/docs/auth/android/google-signin

## 4. Check the shared Firestore database

Open **Build** > **Firestore Database** in Firebase Console.

The owner confirmed that the database ID `ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e` already exists in this project as a Firestore Native database in `asia-south1`. **No database creation or location change is needed.** Both apps, Firestore rules deployment, and the Admin SDK backend are configured to use this exact database ID. The callable Functions and SOS Firestore trigger now use `asia-south1` as well.

The CI suite tests the checked-in Firestore rules against the emulator. The guarded deployment workflow will publish those rules to this existing database.

Official database guide: https://firebase.google.com/docs/firestore/manage-databases

## 5. Enable billing and required services

Cloud Functions deployment requires the Firebase project to be on the **Blaze** plan. In Firebase Console, open **Usage and billing** and link a billing account. Set a budget alert in Google Cloud Billing so unexpected usage is visible. A budget alert is not a hard spending cap.

The deployment uses Cloud Functions for Firebase (2nd gen), Cloud Build, Artifact Registry, Eventarc, Cloud Run, Firestore, and Firebase Authentication. Enable any API that the console or deployment workflow reports as missing.

Official Functions guide: https://firebase.google.com/docs/functions/get-started

## 6. Configure GitHub Actions from the browser

The workflows are already pinned to the owner-provided Project ID `gen-lang-client-0963224775`.

### Anchor Parent repository

Open: https://github.com/alfredrasel721/anchor-parent/settings/secrets/actions

Under **Secrets**, add:

- `FIREBASE_SERVICE_ACCOUNT` = the complete private JSON key for the deployment service account (see the next subsection).
- `ANDROID_UPLOAD_KEYSTORE_BASE64` = the Parent release keystore encoded as Base64.
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD` = the keystore password.
- `ANDROID_UPLOAD_KEY_PASSWORD` = the key password.

### Anchor Child repository

Open: https://github.com/alfredrasel721/Anchor-Child/settings/secrets/actions

Under **Secrets**, add:

- `ANDROID_UPLOAD_KEYSTORE_BASE64` = the Child release keystore encoded as Base64.
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD` = the keystore password.
- `ANDROID_UPLOAD_KEY_PASSWORD` = the key password.

The release Gradle configuration currently expects the keystore alias `upload`. If the existing keystore uses a different alias, update the Gradle configuration before building.

If no release keystore exists and neither app has ever been published, you can create a new key in **Google Cloud Shell** (browser-based; no local Android Studio required). Do not do this if an app already has a release key that must be preserved.

```bash
keytool -genkeypair -v -keystore anchor-parent-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 anchor-parent-upload.jks
```

Use a strong password when prompted. Copy the resulting Base64 text directly into the matching GitHub Actions secret. Repeat with a different filename for the Child app if it needs its own key. Keep the original `.jks` file and passwords in a safe offline backup. Never paste the Base64 string or passwords into ChatGPT, a public issue, or a repository file.

**Never paste a service-account JSON key, keystore, or keystore password into ChatGPT or commit them to Git.** Store those private credentials only as GitHub Actions secrets. The app-specific `google-services.json` files are already committed to their respective app repositories; they contain client configuration and are packaged into the APK. They are not substitutes for the private service-account JSON key.

### What is FIREBASE_SERVICE_ACCOUNT?

This is **not** either Android `google-services.json` file. It is a separate private JSON key that lets GitHub Actions authenticate as a Google Cloud service account and deploy the backend. Do not upload this file to ChatGPT, do not put it in either public repository, and do not share it in a screenshot.

Create it from the browser:

1. Open [Google Cloud Service Accounts](https://console.cloud.google.com/iam-admin/serviceaccounts) and select project `gen-lang-client-0963224775`.
2. Click **Create service account**. Name it `anchor-github-deploy`.
3. Grant the new service account these project roles:
   - **Cloud Functions Admin** (`roles/cloudfunctions.admin`)
   - **Service Account User** (`roles/iam.serviceAccountUser`)
   - **Firebase Rules Admin** (`roles/firebaserules.admin`)
   - **Service Usage Consumer** (`roles/serviceusage.serviceUsageConsumer`)
4. Open the new service account, choose **Keys** > **Add key** > **Create new key** > **JSON**. Google downloads a private JSON file to your computer.
5. Open the Parent repository's [Actions secrets page](https://github.com/alfredrasel721/anchor-parent/settings/secrets/actions). Click **New repository secret**.
6. For the name enter exactly `FIREBASE_SERVICE_ACCOUNT`. Open the downloaded JSON file in Notepad, copy all its contents including the opening and closing braces, paste them into the secret's value, and save.
7. Keep the downloaded file in a safe place, then delete the extra Downloads copy after confirming the GitHub secret is saved. Never paste the key into ChatGPT or commit it to Git.

The deployment workflow checks that the service account's `project_id` equals `gen-lang-client-0963224775`. Firebase's documented minimum for function deployment includes Cloud Functions Admin and Service Account User; Firebase Rules Admin is needed for publishing the checked-in Firestore rules. If Google Cloud reports a missing Cloud Build, Eventarc, Artifact Registry, or API permission, stop and share the error text (not the key) so the exact missing role/API can be identified. Do not grant project Owner just to make an error disappear.

Google's role guidance: https://firebase.google.com/docs/projects/iam/permissions

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

After both pull requests are merged to `main`, the Parent repository's `FIREBASE_SERVICE_ACCOUNT` secret is configured, billing is enabled, and the existing Firestore database has been confirmed:

1. Open the Parent repository's **Actions** tab.
2. Select **Deploy Anchor Firebase backend**.
3. Click **Run workflow** on branch `main`.
4. Enter exactly `DEPLOY` in the confirmation field.
5. Run it and wait for the deployment job to succeed.

The workflow checks that the service-account project's `project_id` matches the pinned project ID `gen-lang-client-0963224775`. It deploys only the Firestore rules and Cloud Functions. It does not create projects/databases, enable billing, or configure Google sign-in.

## 9. Build signed release files

After both app-specific `google-services.json` files (already committed in the PRs) and the Android signing secrets are in place:

1. In each repository, open **Actions**.
2. Select **Build signed Android release**.
3. Run on branch `main` and enter exactly `BUILD_RELEASE`.
4. Download the resulting signed APK and AAB artifacts.

The release workflow checks the committed Firebase config against the project ID, correct Android package, and Web OAuth client. It also prints the signing certificate fingerprints. Keep the same release key for all future updates to that app.

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
- Both repositories are pinned to `gen-lang-client-0963224775`.
- Each repository contains its own app-specific `app/google-services.json`.
- Parent and Child callable clients, Cloud Functions, and the SOS Firestore trigger are configured for `asia-south1`.
