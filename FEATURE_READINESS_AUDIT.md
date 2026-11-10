# Anchor — Full 111-Requirement Release-Coverage Audit

**Date:** 2026-11-10  
**Status:** DEVELOPMENT / NOT READY FOR MARKET RELEASE  
**Authoritative source:** [Original canonical feature catalog](https://github.com/alfredrasel721/nppc-app/blob/HEAD/01-product-requirements/FEATURE_CATALOG.md)  
**Coverage:** exactly 111 identifiers, preserved without deleting any requested feature.

> **Important:** “UNVERIFIED” does **not** mean the design is missing, and a passing Android compile is not proof that a feature operates. “PARTIAL” means some relevant code/UI exists. No requirement is marked “COMPLETE” without an independent end-to-end acceptance result and applicable OS/store-policy checks.

## Product contract (must not be reduced)

All requested features remain in scope. The **only** Premium capability groups are (1) Screen Mirroring, (2) Live Camera, (3) Live Audio, (4) Notification Filtering, (5) Web Filtering, and (6) Social-Media App/Content Filtering. Other specified features stay **Free**, including child-facing live-session permission, visibility, and immediate stop. Conditional integrations (wearables/school/call-SMS) remain tracked; unsupported OS access must not be misrepresented as supported.

## Verified engineering infrastructure, **not** feature acceptance

- Parent/Child Android unit-test + debug APK workflows previously passed.
- Cloudflare Worker is deployed with a secret binding; `GET /health` passed. No complete authenticated Parent–Child pairing/SOS device scenario has been demonstrated.
- Google API reports the configured Anchor Family Safe Firestore database `freeTier: true`.
- Firestore rules explicitly deny client writes to subscription entitlement fields (2026-11-10 change, tested by emulator).
- Parent's unverified “instant Premium unlock” was removed; actual Google Play Billing server verification is not implemented.
- Child consent no longer masquerades as a connected live camera/mic/screen stream; real WebRTC transport is still required.
- Child AccessibilityService now has a deterministic parent-rule/usage-limit path with an emergency-system allowlist, but real-device reliability and Play accessibility declaration require verification.

## Acceptance status by requirement

| Requirement ID | Tier | Requested feature | Status | Remaining verification/implementation |
|---|---|---|---|---|
| `FR-PARENT-001` | FREE | Create family account and child profiles | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-002` | FREE | Pair/enroll child phone or tablet | **PARTIAL** | Worker/Auth/Firestore pairing code exists; end-to-end on physical phones not verified |
| `FR-PARENT-003` | FREE | View child/device dashboard and online status | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-004` | FREE | View daily/weekly screen-time and app-usage reports | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-005` | FREE | Set total daily screen-time limit | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-006` | FREE | Set per-app limits; block/unblock apps | **PARTIAL** | Parent policy UI/Firestore and Child cached Accessibility rule handling implemented; physical Android/OS validation pending |
| `FR-PARENT-007` | FREE | Configure bedtime, school, homework and downtime schedules | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-008` | FREE | Keep essential/emergency apps always allowed | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-009` | FREE | Approve/deny child requests for extra time or access | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-010` | PREMIUM | Filter websites and web content categories; manage web allowlist/blocklist | **PARTIAL** | Premium UI/settings exist; no verified purchase or full on-device filter/media engine |
| `FR-PARENT-011` | FREE | Manage app-installation and purchase approvals where supported | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-012` | FREE | View child location and last update time | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-013` | FREE | Configure safe places/geofences and arrival/departure alerts | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-014` | FREE | Receive SOS, low-battery, offline and permission alerts | **PARTIAL** | Worker SOS + FCM paths exist; real delivery/permissions/fallback tests pending |
| `FR-PARENT-015` | FREE | Review device health, active rules and permission status | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-016` | FREE | Manage alerts, child devices and account settings | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-017` | FREE | View browser/search/YouTube activity where supported | **NOT IMPLEMENTED** | No reliable OS-supported end-to-end implementation shown |
| `FR-PARENT-018` | FREE | Detect risky content, messages, keywords or images where supported | **NOT IMPLEMENTED** | No reliable OS-supported end-to-end implementation shown |
| `FR-PARENT-019` | FREE | Call/SMS activity and contact controls where lawful/supported | **NOT IMPLEMENTED** | No reliable OS-supported end-to-end implementation shown |
| `FR-PARENT-020` | FREE | Location history, route replay and driving reports | **NOT IMPLEMENTED** | No reliable OS-supported end-to-end implementation shown |
| `FR-PARENT-021` | FREE | Remote ring / find device | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-022` | FREE | Create chores, learning tasks and screen-time rewards | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-023` | FREE | Family chat / voice messaging | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-PARENT-024` | PREMIUM | Request visible child-approved screen mirroring, live camera, or live audio sessions (three separately gated Premium capabilities) | **PARTIAL** | Premium UI/settings exist; no verified purchase or full on-device filter/media engine |
| `FR-PARENT-025` | FREE | Export activity reports and configure family goals | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-001` | FREE | Guided child-device enrollment / pairing | **PARTIAL** | Worker/Auth/Firestore pairing code exists; end-to-end on physical phones not verified |
| `FR-CHILD-002` | FREE | Explain supervision and requested permissions in child-friendly terms | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-003` | FREE | Display today's screen time used and remaining | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-004` | FREE | Show allowed/blocked apps and current schedule | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-005` | FREE | Warn before limits or bedtime begin | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-006` | FREE | Display clear restriction reason and next available time | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-007` | FREE | Apply parent-approved app and device/time restrictions (web content filtering is separately Premium) | **PARTIAL** | Parent policy UI/Firestore and Child cached Accessibility rule handling implemented; physical Android/OS validation pending |
| `FR-CHILD-008` | FREE | Allow child to request extra time or access where supported | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-009` | FREE | Receive parent decisions / granted exceptions | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-010` | FREE | Preserve permitted emergency calling during lock periods | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-011` | FREE | Provide SOS button and alert delivery status where supported | **PARTIAL** | Worker SOS + FCM paths exist; real delivery/permissions/fallback tests pending |
| `FR-CHILD-012` | FREE | Show device connection, sync and permission status | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-013` | FREE | Require authorized parent action for protected settings/unpairing | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-014` | FREE | Show personal usage history and wellbeing tips | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-015` | FREE | Child check-in: 'I'm safe', 'Arrived', or 'Need pickup' | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-016` | FREE | Family text/voice chat | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-017` | FREE | View tasks, submit completion and earn reward minutes | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-018` | FREE | Puzzles, exercises, eye-care breaks and healthy-habit goals | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-019` | FREE | Show upcoming schedules and request a schedule exception | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-020` | FREE | Child-controlled pause/resume of allotted free-time budget | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-021` | FREE | Privacy dashboard: what's shared, with whom and when | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-022` | FREE | Visible consent/indicator for any screen, camera or audio session | **PARTIAL** | Child consent and visible stop UI present; actual OS media session transport not implemented |
| `FR-CHILD-023` | FREE | Offline rule enforcement with safe recovery after reconnecting | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CO_GUARDIAN-001` | FREE | Invite/remove a second guardian | **PARTIAL** | Guardian/contact screens exist; role authorization/real invitation workflows not validated |
| `FR-CO_GUARDIAN-002` | FREE | Assign guardian permissions for specific children | **PARTIAL** | Guardian/contact screens exist; role authorization/real invitation workflows not validated |
| `FR-CO_GUARDIAN-003` | FREE | View permitted reports, location and alerts | **PARTIAL** | Guardian/contact screens exist; role authorization/real invitation workflows not validated |
| `FR-CO_GUARDIAN-004` | FREE | Receive SOS and respond to child requests if authorized | **PARTIAL** | Guardian/contact screens exist; role authorization/real invitation workflows not validated |
| `FR-CO_GUARDIAN-005` | FREE | Read-only, temporary-caregiver or location-only access | **PARTIAL** | Guardian/contact screens exist; role authorization/real invitation workflows not validated |
| `FR-CO_GUARDIAN-006` | FREE | Track who changed a rule or approved a request | **PARTIAL** | Guardian/contact screens exist; role authorization/real invitation workflows not validated |
| `FR-CO_GUARDIAN-007` | FREE | Resolve conflicting guardian decisions and revoke access | **PARTIAL** | Guardian/contact screens exist; role authorization/real invitation workflows not validated |
| `FR-EMERGENCY-001` | FREE | Add and verify emergency contacts | **PARTIAL** | Contact/SOS UI and Android dial/SMS intents exist; verified-contact and escalation delivery not validated |
| `FR-EMERGENCY-002` | FREE | Receive SOS alert and available location | **PARTIAL** | Contact/SOS UI and Android dial/SMS intents exist; verified-contact and escalation delivery not validated |
| `FR-EMERGENCY-003` | FREE | Allow child to call approved emergency contacts | **PARTIAL** | Contact/SOS UI and Android dial/SMS intents exist; verified-contact and escalation delivery not validated |
| `FR-EMERGENCY-004` | FREE | Acknowledge SOS; trigger fallback/escalation contacts | **PARTIAL** | Contact/SOS UI and Android dial/SMS intents exist; verified-contact and escalation delivery not validated |
| `FR-EMERGENCY-005` | FREE | Time-limited emergency location link (not routine tracking) | **PARTIAL** | Contact/SOS UI and Android dial/SMS intents exist; verified-contact and escalation delivery not validated |
| `FR-FAMILY_WEB_MIGRATED-001` | FREE | View family, children, paired devices and rules | **UNVERIFIED** | Family web capability is not present as a standalone validated web product |
| `FR-FAMILY_WEB_MIGRATED-002` | FREE | Manage account security and subscription | **UNVERIFIED** | Family web capability is not present as a standalone validated web product |
| `FR-FAMILY_WEB_MIGRATED-003` | FREE | Review/export reports and configure rule templates | **UNVERIFIED** | Family web capability is not present as a standalone validated web product |
| `FR-FAMILY_WEB_MIGRATED-004` | FREE | Review consent history and guardian action audit log | **UNVERIFIED** | Family web capability is not present as a standalone validated web product |
| `FR-FAMILY_WEB_MIGRATED-005` | FREE | Request data download or deletion | **UNVERIFIED** | Family web capability is not present as a standalone validated web product |
| `FR-WEARABLE_OPTIONAL-001` | FREE | Pair compatible child GPS watch | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-WEARABLE_OPTIONAL-002` | FREE | Watch location, safe-zone alerts and battery status | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-WEARABLE_OPTIONAL-003` | FREE | SOS button and approved-contact calls | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-WEARABLE_OPTIONAL-004` | FREE | Watch-to-family voice messages or chat where supported | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-WEARABLE_OPTIONAL-005` | FREE | Show last update, offline state and SIM/connectivity status | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-BACKEND-001` | FREE | Authenticate devices and securely sync parental rules | **PARTIAL** | Worker/Auth/Firestore pairing code exists; end-to-end on physical phones not verified |
| `FR-BACKEND-002` | FREE | Track feature permissions, device status and last sync | **PARTIAL** | Worker or Firestore/local support exists for some workflows; full resilience and feature enforcement unverified |
| `FR-BACKEND-003` | FREE | Deliver alerts, approvals and SOS reliably | **PARTIAL** | Worker SOS + FCM paths exist; real delivery/permissions/fallback tests pending |
| `FR-BACKEND-004` | FREE | Apply OS-compliant app/time/web restrictions | **PARTIAL** | Worker or Firestore/local support exists for some workflows; full resilience and feature enforcement unverified |
| `FR-BACKEND-005` | FREE | Encrypt sensitive data and restrict access by role | **PARTIAL** | Worker or Firestore/local support exists for some workflows; full resilience and feature enforcement unverified |
| `FR-BACKEND-006` | FREE | Provide secure recovery and device replacement flow | **PARTIAL** | Worker or Firestore/local support exists for some workflows; full resilience and feature enforcement unverified |
| `FR-BACKEND-007` | FREE | Queue events offline; retry safely and avoid duplicate alerts | **PARTIAL** | Worker or Firestore/local support exists for some workflows; full resilience and feature enforcement unverified |
| `FR-BACKEND-008` | FREE | Geofence processing and map/location integration | **PARTIAL** | Worker or Firestore/local support exists for some workflows; full resilience and feature enforcement unverified |
| `FR-BACKEND-009` | FREE | Support OS-specific policy capabilities and fallbacks | **PARTIAL** | Worker or Firestore/local support exists for some workflows; full resilience and feature enforcement unverified |
| `FR-BACKEND-010` | FREE | Feature flags, service-health metrics and incident monitoring | **PARTIAL** | Worker or Firestore/local support exists for some workflows; full resilience and feature enforcement unverified |
| `FR-ADMIN-001` | FREE | Resolve pairing, sync, permission and billing issues | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-ADMIN-002` | FREE | Access minimal diagnostic data, not private child content by default | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-ADMIN-003` | FREE | Role-based staff permissions and audit logs | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-ADMIN-004` | FREE | Handle abuse reports and account/data-deletion requests | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-ADMIN-005` | FREE | Time-limited, explicitly authorized support access | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-ADMIN-006` | FREE | Manage plans, entitlements, app versions and incidents | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-ADMIN-007` | FREE | Manage retention, consent, regional policy and security reviews | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-SCHOOL_OPTIONAL-001` | FREE | School-hours focus rules with appropriate authorization | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-SCHOOL_OPTIONAL-002` | FREE | Limited school-device usage summaries | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-SCHOOL_OPTIONAL-003` | FREE | Separate school and household data/permissions | **NOT IMPLEMENTED** | No integrated operational service/device support shown in current Parent/Child Android repos |
| `FR-PRIVACY-001` | FREE | Age-appropriate notice, consent and transparency | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
| `FR-PRIVACY-002` | FREE | Clear indication of sensitive monitoring; no covert surveillance | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
| `FR-PRIVACY-003` | FREE | Data minimization, encryption and retention/deletion controls | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
| `FR-PRIVACY-004` | FREE | Guardian identity verification and secure account recovery | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
| `FR-PRIVACY-005` | FREE | Emergency access and fail-safe behavior when a device is locked | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
| `FR-PRIVACY-006` | FREE | Test Android/iOS differences before promising a feature | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
| `FR-PRIVACY-007` | FREE | Explain permissions and handle permission revocation gracefully | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
| `FR-PRIVACY-008` | FREE | Accessibility, localization and low-bandwidth support | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
| `FR-PRIVACY-009` | FREE | Periodic access reviews and privacy/security audit reports | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
| `FR-PARENT-026` | PREMIUM | Premium screen mirroring request and live session viewer | **PARTIAL** | Premium UI/settings exist; no verified purchase or full on-device filter/media engine |
| `FR-PARENT-027` | PREMIUM | Premium live camera request and live session viewer | **PARTIAL** | Premium UI/settings exist; no verified purchase or full on-device filter/media engine |
| `FR-PARENT-028` | PREMIUM | Premium live microphone audio request and live session listener | **PARTIAL** | Premium UI/settings exist; no verified purchase or full on-device filter/media engine |
| `FR-PARENT-029` | PREMIUM | Premium notification filter: app/category-based allow or suppress where OS permits | **PARTIAL** | Premium UI/settings exist; no verified purchase or full on-device filter/media engine |
| `FR-PARENT-030` | PREMIUM | Premium social-media app/content filtering rules where technically available | **PARTIAL** | Premium UI/settings exist; no verified purchase or full on-device filter/media engine |
| `FR-CHILD-024` | FREE | Show Premium notification-filter status and clearly disclose its scope | **UNVERIFIED** | Screen/data-model or local/cloud scaffolding may exist; complete feature has not passed end-to-end acceptance |
| `FR-CHILD-025` | FREE | Child-initiated approval or denial for each live screen/camera/microphone session | **PARTIAL** | Child consent and visible stop UI present; actual OS media session transport not implemented |
| `FR-CHILD-026` | FREE | Persistent live-session indicator, stop button, and audit visibility | **PARTIAL** | Child consent and visible stop UI present; actual OS media session transport not implemented |
| `FR-CHILD-027` | PREMIUM | Apply Premium web/social/notification rules only with authorized permissions and entitlement | **PARTIAL** | Premium UI/settings exist; no verified purchase or full on-device filter/media engine |
| `FR-BACKEND-011` | FREE | Verify Play Billing purchases server-side; entitlements and refunds | **NOT IMPLEMENTED** | No Play Billing server-side purchase-token verification/refund webhook |
| `FR-BACKEND-012` | FREE | Reject Premium actions on server and child device when entitlement absent | **PARTIAL** | Firestore denies client entitlement forgery; authenticated backend + Child Premium enforcement still incomplete |
| `FR-PRIVACY-010` | FREE | Never capture sensitive content secretly or bypass OS consent dialogs | **PARTIAL** | Privacy screens and rules exist; retention/consent/release audit not completed |
## Non-negotiable release conditions

1. No hard-coded demo family/child location or simulated telemetry is shown as if it came from a real device after sign-in; no fake Premium entitlement or streaming/notification count.
2. Child device permissions, pairing, revocation, screen-time restrictions, SOS, offline recovery, and emergency bypass work on physical Android phones (including background/locked conditions).
3. A supported browser/web/social/notification filtering engine exists, is disclosed, and is *server-entitlement gated*. Unsupported content interception is not promised.
4. Screen/camera/mic Premium features have **real** end-to-end media transport and separate per-session Child consent; Android MediaProjection/runtime permissions, foreground indicator, and instant stop work.
5. Play Billing purchases/refunds are verified by a trusted server; Android clients cannot grant Premium to themselves.
6. Firestore rules are deployed to the *confirmed Spark-eligible database*, Google Sign-In works on the production-signing SHA fingerprint, and relevant permissions/notifications pass smoke tests.
7. All 111 FR items have an acceptance decision: end-to-end verified / explicitly OS-limited and disclosed / integration not yet available. The latter two must not be marketed as supported completed features.
8. Privacy, children's data, accessibility declarations, billing, Google Play review, and production signing complete before marketing.

## Current release decision

**BLOCK PUBLIC RELEASE.** Continue implementing the full requested catalog and verifying real-device behavior. A debug APK artifact is a test build, **not** an approved final app. Do not merge Draft PRs or publish production APK/AAB based only on compilation.

**Canonical source:** `nppc-app/01-product-requirements/FEATURE_CATALOG.md` (111 rows); `SRS.md` defines acceptance conditions. This audit tracks every row; it does **not** reclassify pricing tiers or discard any features.
