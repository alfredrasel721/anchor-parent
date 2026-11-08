# Anchor Family — Shared Firebase Configuration & Anchor Child Integration Package

## 1. Unified Firebase Project & Firestore Database Configuration

Both **Anchor Parent (`com.anchor.parent`)** and **Anchor Child (`com.anchor.child`)** must connect to the same Firebase project and the same named Firestore database:

- **Firebase Project ID**: `gen-lang-client-0963224775`
- **Project Number**: `534194226401`
- **Unified Firestore Database ID**: `ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e`

### Required Fix in Repository 2 (`https://github.com/alfredrasel721/Anchor-Child`)

Replace `app/src/main/res/values/firebase_applet_config.xml` in **Anchor-Child** (which previously pointed to `ai-studio-android-anchorch-710012f1-419e-46fb-87fd-4e0fce481e8e`) with:

```xml
<?xml version="1.0" encoding="utf-8" standalone="no"?>
<resources>
    <string name="firestore_database_id" translatable="false">ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e</string>
</resources>
```

And ensure `FirebaseFirestore` is always initialized with the named database ID:

```kotlin
val databaseId = context.getString(R.string.firestore_database_id)
val firestore = FirebaseFirestore.getInstance(databaseId)
```

---

## 2. Shared Collection Paths (Identical Across Parent & Child)

| Entity | Firestore Path | Parent Access | Paired Child Access |
| :--- | :--- | :--- | :--- |
| User Profile | `/users/{userId}` | Read/Write own UID | Read/Write own UID |
| Family Household | `/families/{familyId}` | Read/Write (`ownerUid`) | Read-only (`get`/`list`) |
| Child Profile & Telemetry | `/families/{familyId}/children/{childId}` | Read/Write | Read + Update telemetry (`usedMinutesToday`, `batteryLevel`, `latitude`, `longitude`, `locationLabel`, `lastSyncLabel`) |
| Pairing Codes | `/families/{familyId}/pairingCodes/{codeId}` | Create, Read, Revoke | Read + Atomic single-use `CONSUMED` transition |
| Verified Paired Devices | `/families/{familyId}/pairedDevices/{childUid}` | Read, Revoke/Delete | Create only via verified active `PairingCode` or `redeemPairingCode` Cloud Function |
| App Usage Rules | `/families/{familyId}/appRules/{ruleId}` | Read/Write | Read + Update `usedMinutesToday` only |
| Downtime Schedules | `/families/{familyId}/schedules/{scheduleId}` | Read/Write | Read-only |
| Safe Geofences | `/families/{familyId}/geofences/{geofenceId}` | Read/Write | Read-only |
| Alerts & SOS | `/families/{familyId}/alerts/{alertId}` | Read/Write/Acknowledge | Read + Create (`SOS`, `GEOFENCE` check-in) |
| Extra Time & App Requests | `/families/{familyId}/requests/{requestId}` | Read/Approve/Decline | Read + Create (`status == "PENDING"`) |
| Family Chat Messages | `/families/{familyId}/messages/{messageId}` | Read/Create (`PARENT`, `SYSTEM`) | Read + Create (`senderRole == "CHILD"`) |
| Shared Family Tasks | `/families/{familyId}/tasks/{taskId}` | Read/Create/Approve (`COMPLETED`) | Read + Update (`completedByChild = true`, `status = "READY_FOR_REVIEW"`) |
| Guardians & Contacts | `/families/{familyId}/guardians/{guardianId}` | Read/Write | Read-only |
| Premium Filter Configs | `/families/{familyId}/premiumConfigs/{configId}` | Read/Write | Read-only |
| Audited Live Sessions | `/families/{familyId}/liveSessions/{sessionId}` | Create request / End | Read + Grant/Decline explicit consent (`consentGranted`) |

---

## 3. Deploying the Firebase Cloud Functions (`functions/`)

The `functions/` directory in this repository contains the production Firebase Admin SDK callable functions:
- `createPairingCode`
- `redeemPairingCode`
- `revokePairingCode`

To deploy to the shared Firebase project:

```bash
cd functions
npm install
firebase deploy --only functions --project gen-lang-client-0963224775
```
