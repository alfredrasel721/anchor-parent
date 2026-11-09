# Anchor backend setup and deployment

Anchor uses Firebase Authentication and Firestore for identity and app data, with a Cloudflare Worker for trusted server-side operations.

- Firebase project: `gen-lang-client-0963224775`
- Firestore database currently configured in the apps: `ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e`
- Firestore location: `asia-south1`
- Worker URL: `https://anchor-family-api.anchorfamily.workers.dev`
- Parent package: `com.anchor.parent`
- Child package: `com.anchor.child`

## Important: Spark/free-tier check is a deployment gate

The configured Firestore database has a custom database ID, not `(default)`. The Parent CI workflow checks the Firestore database metadata field `freeTier` using the repository secret `FIREBASE_SERVICE_ACCOUNT`. A custom database ID is not enough to infer eligibility; use the returned metadata rather than guessing.

**Do not deploy the Worker against Firestore while the project is on Spark unless the metadata check confirms `freeTier: true`.** If the result is false or cannot be read, pause and resolve the database plan first. The alternatives are to migrate the app to a Spark-eligible `(default)` database (including data and rules) or use a billing-enabled Firestore plan. Cloudflare Workers does not remove Firestore's own billing requirements.

## Worker API

The Worker replaces the previous Firebase Cloud Functions callable endpoints and SOS trigger:

- `POST /v1/pairing/create`
- `POST /v1/pairing/redeem`
- `POST /v1/pairing/revoke`
- `POST /v1/pairing/verify`
- `POST /v1/notifications/topic` (Parent only)
- `POST /v1/sos` (verified paired Child only)
- `GET /health`

Mobile apps send their Firebase ID token. The Worker verifies the token, authorizes the user against the family/device documents, and then uses a service-account credential to access Firestore through its REST API. The service-account JSON is never included in either APK.

## Required Google Cloud service-account setup

Use the existing service account JSON key only in secret stores. Do not send it in chat or commit it to Git.

Grant the service account these roles, separated by purpose:

**Worker runtime**
- **Cloud Datastore User** (`roles/datastore.user`) — read/write Firestore documents.
- **Firebase Cloud Messaging Admin** (`roles/firebasecloudmessaging.admin`) — send SOS push notifications.

**GitHub deployment and eligibility checks**
- **Cloud Datastore Viewer** (`roles/datastore.viewer`) — read the database's `freeTier` metadata.
- **Firebase Rules Admin** (`roles/firebaserules.admin`) — deploy the checked-in Firestore rules.
- **Service Usage Consumer** (`roles/serviceusage.serviceUsageConsumer`) — consume the enabled Google APIs.

The Cloud Firestore API and Firebase Cloud Messaging API must be enabled in the Google Cloud project.

The Parent repository's GitHub secret `FIREBASE_SERVICE_ACCOUNT` is used for the metadata check and the Firestore-rules deployment workflow. The Worker needs its own copy of the same JSON value as a Cloudflare Worker secret.

## Add the Worker secret in Cloudflare

The production Worker is not deployed yet. The staging Worker `anchor-family-api-staging` was deployed only to validate the JavaScript bundle. Add the secret to the production Worker after the Firestore free-tier gate passes and the production Worker is created.

1. Open the [Cloudflare dashboard](https://dash.cloudflare.com/).
2. Select **Workers & Pages** > **anchor-family-api**.
3. Open **Settings** > **Variables and Secrets**.
4. Add a **Secret** named exactly `FIREBASE_SERVICE_ACCOUNT`.
5. Paste the full JSON contents of the Google service-account key and save.
6. Keep the value secret. Do not paste it into this repository, an issue, or ChatGPT.

The Worker also uses these non-secret values:

- `FIREBASE_PROJECT_ID=gen-lang-client-0963224775`
- `FIRESTORE_DATABASE_ID=ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e`

## Firestore security rules

The collection `familyNotificationTopics/{familyId}` is server-only. The rules explicitly deny all mobile-client reads and writes to it. Parent clients obtain the unguessable FCM topic from the Worker after authenticating; Child clients cannot read it.

After reviewing the rules, deploy them from the Parent repository:

1. Open **Actions** > **Deploy Anchor Firestore rules**.
2. Run on `main`.
3. Enter `DEPLOY_RULES` to confirm.
4. Verify the workflow completes successfully.

This workflow deploys **Firestore rules only**. It does not deploy Firebase Cloud Functions.

## Local Worker checks

From `cloudflare-worker/`:

```bash
node --check src/index.mjs
node --test test/worker.test.mjs
```

The Worker tests cover code normalization, safe document paths, Firestore REST value conversion, Firebase X.509 token verification failure paths, unauthenticated requests, and the health route.

## Parent/Child device acceptance test

Before any public release, test with two physical Android devices:

1. Parent signs in and registers for SOS push notifications.
2. Parent creates a six-digit pairing code.
3. Child redeems the code once; replay, expiry, and brute-force attempts are rejected.
4. Child verifies its paired session.
5. Parent revokes the code or unpairs the device; Child verification fails afterward.
6. Child sends an SOS; the Worker writes the alert and sends an FCM notification to the private Parent topic.
7. Parent receives the notification with the app foregrounded, backgrounded, and after restart.
8. Confirm an unpaired Child cannot create an SOS for another family.

Do not mark SOS as production-ready until notification permission behavior, Android background delivery, and real-device delivery have all been verified.
