# Anchor Family API (Cloudflare Worker)

This Worker replaces the Firebase Cloud Functions used by Anchor Parent and Anchor Child while keeping Firebase Authentication and Firestore.

## Routes

All routes except `GET /health` require a Firebase ID token in `Authorization: Bearer <id-token>`.

- `POST /v1/pairing/create` — Parent creates a short-lived pairing code.
- `POST /v1/pairing/redeem` — Child redeems a one-time code.
- `POST /v1/pairing/revoke` — Parent revokes a code or unpairs a device.
- `POST /v1/pairing/verify` — Child verifies its active paired session.
- `POST /v1/notifications/topic` — Parent obtains its private SOS notification topic.
- `POST /v1/sos` — Paired Child creates an SOS alert and requests an FCM notification.

## Worker configuration

The deployment uses these non-secret variables:

- `FIREBASE_PROJECT_ID=gen-lang-client-0963224775`
- `FIRESTORE_DATABASE_ID=ai-studio-anchorfamilysafe-1f622f51-243e-4d62-9cb1-9fdaf9b5dc9d`

The production Worker `anchor-family-api` has been deployed, and the `workers.dev` subdomain is enabled. Add the JSON service-account key in the Cloudflare dashboard as a Worker **Secret** named `FIREBASE_SERVICE_ACCOUNT`; this secret has not been added yet, so authenticated data operations are not live. Never commit the key or send it in chat.

The service account needs:
- Cloud Datastore User (`roles/datastore.user`) for Firestore document operations.
- Firebase Cloud Messaging Admin (`roles/firebasecloudmessaging.admin`) for SOS push delivery.
- Enable the Cloud Firestore API and Firebase Cloud Messaging API in the Google Cloud project.

The service-account key must belong to project `gen-lang-client-0963224775`.

## Firestore security

The Worker uses the Firestore REST API with a service-account OAuth token. Mobile clients continue using Firebase Authentication and Firestore client SDKs. The `familyNotificationTopics` collection is server-only; Firestore rules must deny all mobile-client reads and writes to it.

## Local checks

Run `node --check src/index.mjs` and `node --test test/worker.test.mjs` from this directory.

## Important deployment gate

Before production deployment, verify that the configured Firestore database is eligible for the Firebase Spark free quota. Cloudflare Workers does not remove Firestore's own billing requirements. Also complete real Parent/Child device tests before enabling SOS notifications for production use.
