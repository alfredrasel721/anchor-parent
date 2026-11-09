# Firebase deployment and Android release setup

This repository is the canonical Firebase backend for Anchor Parent and Anchor Child.

## Existing project configuration

- Firebase / Google Cloud project ID: `gen-lang-client-0963224775`
- Shared Firestore database ID: `ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e`
- Parent Android application ID: `com.anchor.parent`
- Child Android application ID: `com.anchor.child`
- Cloud Functions runtime: Node.js 22
- Cloud Functions region: `us-central1`

The Parent and Child apps must be registered as two separate Android apps in the same Firebase project. Both must use the shared Firestore database above.

## Required owner setup

1. Open the Firebase project `gen-lang-client-0963224775` in Firebase Console.
2. Confirm that Authentication is enabled and Google is enabled as a sign-in provider.
3. Confirm that the named Firestore database above exists. Do not create a second database with a different ID.
4. Register Android app `com.anchor.parent` and download its `google-services.json`.
5. Register Android app `com.anchor.child` and download its separate `google-services.json`.
6. Add the matching Google OAuth SHA-1 fingerprints for the debug and release signing certificates in Firebase/Google Cloud. The Web OAuth client generated for the same Firebase project is used by Google sign-in in both apps.
7. Configure a Google Cloud service account for deployment, with only the Firebase/Firestore/Cloud Functions permissions required for deployment. Store its complete JSON key as a GitHub Actions repository secret named `FIREBASE_SERVICE_ACCOUNT` in this Parent repository. Never commit the JSON key to Git.
8. Ensure the project has billing and the required Google Cloud APIs enabled for Cloud Functions (2nd gen), Cloud Build, Artifact Registry, Eventarc, Firestore, Firebase Authentication, and Cloud Messaging.

## Deploying the backend

After the secure-pairing PR is merged to `main` and the secret above is configured:

1. Open the **Actions** tab in this repository.
2. Select **Deploy Anchor Firebase backend**.
3. Choose **Run workflow**.
4. Enter exactly `DEPLOY` in the confirmation field.
5. Run it and wait for the deployment job to finish successfully.

The workflow deploys only the Firestore rules and Cloud Functions configured in `firebase.json`. It does not create Firebase projects, enable billing, configure Google sign-in, or deploy Android apps.

## Android app configuration and APK signing

- Place the Parent app's Firebase config at `app/google-services.json` in the Parent repository.
- Place the Child app's Firebase config at `app/google-services.json` in the Child repository.
- The current CI debug APKs are test builds. They use a CI-only debug signing key and a placeholder Google OAuth resource; they are not production-ready Firebase-connected releases.
- A production/release APK requires the real Firebase app config, a release signing keystore, and its passwords. Preserve the existing upload/signing key if one already exists; do not generate a replacement casually because Android updates must keep the same signing identity.
- Do not share private keys, keystore passwords, OAuth client secrets, or service-account JSON in chat or commit them to Git.

## Final acceptance test

After deployment and app configuration, install both apps on separate Android devices and verify:

- Parent Google sign-in and household creation.
- Parent issues a six-digit code; Child redeems it once.
- Expired, reused, revoked, and throttled codes are rejected.
- Parent sees the paired Child device.
- Parent unpairs the Child; Child loses household access and must pair again.
- Firestore denies unpaired Child access and client-side writes to pairing secrets/membership.
- SOS notification delivery and other enabled family features work on physical devices.
