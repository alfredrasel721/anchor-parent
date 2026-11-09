/**
 * Anchor Family — shared trusted backend for Anchor Parent and Anchor Child.
 * Firebase project: gen-lang-client-0963224775
 * Firestore database: ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e
 *
 * Pairing secrets are never readable or writable by mobile clients. All pairing
 * state transitions are performed with the Admin SDK in these callable functions.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const crypto = require("node:crypto");

if (!admin.apps.length) {
  admin.initializeApp();
}

const DATABASE_ID = "ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e";
const PAIRING_TTL_MS = 10 * 60 * 1000;
const MAX_REDEEM_ATTEMPTS = 5;
const RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000;

function getDb() {
  return getFirestore(admin.app(), DATABASE_ID);
}

function normalizePairingCode(rawCode) {
  const digits = String(rawCode || "").replace(/[^0-9]/g, "");
  return digits.length === 6 ? digits : null;
}

function hashPairingCode(code) {
  return crypto.createHash("sha256").update(code).digest("hex");
}

function requireAuth(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Sign in before using Anchor pairing.");
  }
  return request.auth.uid;
}

function safeDeviceValue(value, fallback, maxLength) {
  const normalized = String(value || fallback).trim();
  return normalized.slice(0, maxLength) || fallback;
}

/**
 * Parent-only: creates a six-digit, ten-minute, single-use code.
 * Only a hash is stored; the raw code is returned once to the authenticated Parent.
 */
exports.createPairingCode = onCall(async (request) => {
  const parentUid = requireAuth(request);
  const familyId = String(request.data && request.data.familyId || "").trim();
  const childId = String(request.data && request.data.childId || "").trim();
  const childName = safeDeviceValue(request.data && request.data.childName, "Child", 100);

  if (!familyId || familyId !== "fam_" + parentUid || !childId) {
    throw new HttpsError("invalid-argument", "A valid owned familyId and childId are required.");
  }

  const db = getDb();
  const familyRef = db.doc("families/" + familyId);
  const childRef = db.doc("families/" + familyId + "/children/" + childId);
  const familySnap = await familyRef.get();

  if (!familySnap.exists || familySnap.data().ownerUid !== parentUid) {
    throw new HttpsError("permission-denied", "Only the verified family owner can create pairing codes.");
  }

  const childSnap = await childRef.get();
  if (!childSnap.exists || childSnap.data().ownerUid !== parentUid ||
      childSnap.data().familyId !== familyId || childSnap.data().childId !== childId) {
    throw new HttpsError("not-found", "The selected child profile does not belong to this family.");
  }

  for (let attempt = 0; attempt < 5; attempt += 1) {
    const code = String(crypto.randomInt(100000, 1000000));
    const codeHash = hashPairingCode(code);
    const codeId = "pair_" + childId + "_" + crypto.randomUUID();
    const nowMs = Date.now();
    const expiresAtEpochMs = nowMs + PAIRING_TTL_MS;
    const codeRef = db.doc("families/" + familyId + "/pairingCodes/" + codeId);
    const lookupRef = db.doc("pairingCodeLookups/" + codeHash);

    const transactionResult = await db.runTransaction(async (tx) => {
      const [currentChildSnap, lookupSnap] = await Promise.all([
        tx.get(childRef),
        tx.get(lookupRef),
      ]);

      if (!currentChildSnap.exists ||
          currentChildSnap.data().ownerUid !== parentUid) {
        throw new HttpsError("permission-denied", "Child profile ownership changed.");
      }

      if (lookupSnap.exists) {
        return { collision: true };
      }

      const oldCodeId = String(currentChildSnap.data().activePairingCodeId || "");
      let oldCodeRef = null;
      let oldCodeSnap = null;
      let oldLookupRef = null;

      if (oldCodeId) {
        oldCodeRef = db.doc("families/" + familyId + "/pairingCodes/" + oldCodeId);
        oldCodeSnap = await tx.get(oldCodeRef);
        if (oldCodeSnap.exists && oldCodeSnap.data().codeHash) {
          oldLookupRef = db.doc("pairingCodeLookups/" + oldCodeSnap.data().codeHash);
          await tx.get(oldLookupRef);
        }
      }

      if (oldCodeSnap && oldCodeSnap.exists && oldCodeSnap.data().status === "ACTIVE") {
        tx.update(oldCodeRef, {
          status: "REVOKED",
          revoked: true,
          revokedAtEpochMs: nowMs,
        });
      }
      if (oldLookupRef) {
        tx.delete(oldLookupRef);
      }

      tx.set(codeRef, {
        codeId: codeId,
        familyId: familyId,
        ownerUid: parentUid,
        childId: childId,
        childName: childName,
        code: "",
        codeHash: codeHash,
        expiresAtEpochMs: expiresAtEpochMs,
        status: "ACTIVE",
        revoked: false,
        attemptCount: 0,
        maxAttempts: MAX_REDEEM_ATTEMPTS,
        consumedByChildUid: "",
        consumedAtEpochMs: 0,
        createdAtEpochMs: nowMs,
        createdAt: FieldValue.serverTimestamp(),
      });

      tx.set(lookupRef, {
        familyId: familyId,
        codeId: codeId,
        status: "ACTIVE",
        expiresAtEpochMs: expiresAtEpochMs,
        createdAtEpochMs: nowMs,
      });

      tx.update(childRef, {
        activePairingCodeId: codeId,
        updatedAt: FieldValue.serverTimestamp(),
      });

      return { collision: false };
    });

    if (!transactionResult.collision) {
      return {
        verified: true,
        codeId: codeId,
        familyId: familyId,
        ownerUid: parentUid,
        childId: childId,
        childName: childName,
        code: code.slice(0, 3) + "-" + code.slice(3),
        expiresAtEpochMs: expiresAtEpochMs,
        status: "ACTIVE",
      };
    }
  }

  throw new HttpsError("resource-exhausted", "Could not allocate a unique pairing code. Please retry.");
});

/**
 * Child-only: redeems a code without requiring the Child to know the Parent's familyId.
 * Invalid attempts are committed to the rate-limit document before the callable returns an error.
 */
exports.redeemPairingCode = onCall(async (request) => {
  const childUid = requireAuth(request);
  const code = normalizePairingCode(request.data && request.data.code);

  if (!code) {
    throw new HttpsError("invalid-argument", "Enter the six-digit code shown in Anchor Parent.");
  }

  const deviceName = safeDeviceValue(request.data && request.data.deviceName, "Android Child Device", 100);
  const deviceModel = safeDeviceValue(request.data && request.data.deviceModel, "Android", 100);
  const osVersion = safeDeviceValue(request.data && request.data.osVersion, "Android", 50);
  const db = getDb();
  const nowMs = Date.now();
  const codeHash = hashPairingCode(code);
  const rateLimitRef = db.doc("pairingRateLimits/" + childUid);
  const lookupRef = db.doc("pairingCodeLookups/" + codeHash);

  const outcome = await db.runTransaction(async (tx) => {
    const [rateSnap, lookupSnap] = await Promise.all([
      tx.get(rateLimitRef),
      tx.get(lookupRef),
    ]);

    const rateData = rateSnap.exists ? rateSnap.data() : {};
    const oldWindowStart = Number(rateData.windowStartEpochMs || 0);
    const sameWindow = oldWindowStart > 0 && nowMs - oldWindowStart < RATE_LIMIT_WINDOW_MS;
    const windowStartEpochMs = sameWindow ? oldWindowStart : nowMs;
    const failedCount = sameWindow ? Number(rateData.failedCount || 0) : 0;

    if (failedCount >= MAX_REDEEM_ATTEMPTS) {
      return {
        errorCode: "resource-exhausted",
        errorMessage: "Too many attempts. Wait 15 minutes before trying again.",
      };
    }

    if (!lookupSnap.exists || lookupSnap.data().status !== "ACTIVE") {
      tx.set(rateLimitRef, {
        childUid: childUid,
        failedCount: failedCount + 1,
        windowStartEpochMs: windowStartEpochMs,
        lastAttemptEpochMs: nowMs,
      });
      return {
        errorCode: "not-found",
        errorMessage: "Invalid or expired pairing code. Check the code in Anchor Parent.",
      };
    }

    const lookup = lookupSnap.data();
    const familyId = String(lookup.familyId || "");
    const codeId = String(lookup.codeId || "");
    if (!familyId || !codeId) {
      tx.delete(lookupRef);
      return {
        errorCode: "failed-precondition",
        errorMessage: "Pairing code is no longer valid. Ask the Parent to generate a new one.",
      };
    }

    const codeRef = db.doc("families/" + familyId + "/pairingCodes/" + codeId);
    const familyRef = db.doc("families/" + familyId);
    const childRef = db.doc("families/" + familyId + "/children/" + String(lookup.childId || ""));
    const deviceRef = db.doc("families/" + familyId + "/pairedDevices/" + childUid);

    const [codeSnap, familySnap, childSnap, deviceSnap] = await Promise.all([
      tx.get(codeRef),
      tx.get(familyRef),
      tx.get(childRef),
      tx.get(deviceRef),
    ]);

    if (!codeSnap.exists || !familySnap.exists || !childSnap.exists) {
      tx.delete(lookupRef);
      return {
        errorCode: "failed-precondition",
        errorMessage: "The Parent profile is no longer available. Ask the Parent to generate a new code.",
      };
    }

    const codeData = codeSnap.data();
    const familyData = familySnap.data();
    const childData = childSnap.data();

    if (codeData.status !== "ACTIVE" || codeData.codeHash !== codeHash ||
        lookup.expiresAtEpochMs !== codeData.expiresAtEpochMs) {
      tx.delete(lookupRef);
      return {
        errorCode: "failed-precondition",
        errorMessage: "This pairing code has already been used or revoked.",
      };
    }

    if (Number(codeData.expiresAtEpochMs || 0) <= nowMs) {
      tx.update(codeRef, { status: "EXPIRED" });
      tx.delete(lookupRef);
      return {
        errorCode: "deadline-exceeded",
        errorMessage: "This pairing code expired. Ask the Parent for a new code.",
      };
    }

    if (familyData.ownerUid !== codeData.ownerUid ||
        childData.ownerUid !== codeData.ownerUid ||
        childData.familyId !== familyId ||
        childData.childId !== codeData.childId) {
      tx.delete(lookupRef);
      return {
        errorCode: "permission-denied",
        errorMessage: "The pairing code does not match a valid family profile.",
      };
    }

    if (childData.pairedChildUid && childData.pairedChildUid !== childUid) {
      return {
        errorCode: "already-exists",
        errorMessage: "This child profile is already paired to another device. Ask the Parent to unpair it first.",
      };
    }

    if (deviceSnap.exists && deviceSnap.data().status === "PAIRED" &&
        deviceSnap.data().childId !== codeData.childId) {
      return {
        errorCode: "already-exists",
        errorMessage: "This account is already paired to another child profile.",
      };
    }

    tx.update(codeRef, {
      status: "CONSUMED",
      consumedByChildUid: childUid,
      consumedAtEpochMs: nowMs,
    });
    tx.delete(lookupRef);

    tx.set(deviceRef, {
      deviceId: childUid,
      childUid: childUid,
      familyId: familyId,
      ownerUid: codeData.ownerUid,
      childId: codeData.childId,
      childName: codeData.childName || "Child",
      codeId: codeId,
      redeemedCode: "",
      deviceName: deviceName,
      deviceModel: deviceModel,
      osVersion: osVersion,
      status: "PAIRED",
      pairedAtEpochMs: nowMs,
      lastHeartbeatEpochMs: nowMs,
      updatedAt: FieldValue.serverTimestamp(),
    });

    tx.set(childRef, {
      pairedChildUid: childUid,
      activePairingCodeId: FieldValue.delete(),
      deviceName: deviceName,
      deviceModel: deviceModel,
      osVersion: osVersion,
      syncStatus: "ENFORCED",
      lastSyncLabel: "Paired & verified",
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    tx.delete(rateLimitRef);

    return {
      verified: true,
      familyId: familyId,
      ownerUid: codeData.ownerUid,
      childId: codeData.childId,
      childName: codeData.childName || "Child",
      codeId: codeId,
      deviceId: childUid,
      pairedAtEpochMs: nowMs,
    };
  });

  if (outcome.errorCode) {
    throw new HttpsError(outcome.errorCode, outcome.errorMessage);
  }
  return outcome;
});

/** Parent-only: revoke a pending code or unpair a registered Child device. */
exports.revokePairingCode = onCall(async (request) => {
  const parentUid = requireAuth(request);
  const familyId = String(request.data && request.data.familyId || "").trim();
  const codeId = String(request.data && request.data.codeId || "").trim();
  const deviceUid = String(request.data && request.data.deviceUid || "").trim();

  if (!familyId || familyId !== "fam_" + parentUid || (!codeId && !deviceUid)) {
    throw new HttpsError("invalid-argument", "A valid familyId and codeId or deviceUid are required.");
  }

  const db = getDb();
  const familyRef = db.doc("families/" + familyId);
  const familySnap = await familyRef.get();
  if (!familySnap.exists || familySnap.data().ownerUid !== parentUid) {
    throw new HttpsError("permission-denied", "Only the family owner can revoke pairing.");
  }

  const nowMs = Date.now();
  await db.runTransaction(async (tx) => {
    let codeRef = null;
    let codeSnap = null;
    let lookupRef = null;
    let deviceRef = null;
    let deviceSnap = null;
    let childRef = null;
    let childSnap = null;

    if (codeId) {
      codeRef = db.doc("families/" + familyId + "/pairingCodes/" + codeId);
      codeSnap = await tx.get(codeRef);
      if (codeSnap.exists && codeSnap.data().codeHash) {
        lookupRef = db.doc("pairingCodeLookups/" + codeSnap.data().codeHash);
        await tx.get(lookupRef);
      }
    }

    if (deviceUid) {
      deviceRef = db.doc("families/" + familyId + "/pairedDevices/" + deviceUid);
      deviceSnap = await tx.get(deviceRef);
      if (deviceSnap.exists) {
        childRef = db.doc("families/" + familyId + "/children/" + deviceSnap.data().childId);
        childSnap = await tx.get(childRef);
      }
    }

    if (codeId && (!codeSnap || !codeSnap.exists ||
        codeSnap.data().ownerUid !== parentUid)) {
      throw new HttpsError("not-found", "Pairing code not found for this family.");
    }

    if (codeSnap && codeSnap.exists) {
      tx.update(codeRef, {
        status: "REVOKED",
        revoked: true,
        revokedAtEpochMs: nowMs,
      });
      if (lookupRef) tx.delete(lookupRef);
    }

    if (deviceUid && deviceSnap && deviceSnap.exists) {
      if (deviceSnap.data().ownerUid !== parentUid) {
        throw new HttpsError("permission-denied", "Device does not belong to this family.");
      }
      tx.delete(deviceRef);
      if (childSnap && childSnap.exists && childSnap.data().pairedChildUid === deviceUid) {
        tx.update(childRef, {
          pairedChildUid: FieldValue.delete(),
          syncStatus: "PENDING",
          lastSyncLabel: "Device unpaired by Parent",
          updatedAt: FieldValue.serverTimestamp(),
        });
      }
    }
  });

  return { revoked: true, familyId: familyId, codeId: codeId || null, deviceUid: deviceUid || null };
});

/** Child-only: validates a locally persisted pairing session against server membership. */
exports.verifyPairingSession = onCall(async (request) => {
  const childUid = requireAuth(request);
  const familyId = String(request.data && request.data.familyId || "").trim();
  const childId = String(request.data && request.data.childId || "").trim();

  if (!familyId || !childId) {
    throw new HttpsError("invalid-argument", "familyId and childId are required.");
  }

  const db = getDb();
  const [familySnap, childSnap, deviceSnap] = await Promise.all([
    db.doc("families/" + familyId).get(),
    db.doc("families/" + familyId + "/children/" + childId).get(),
    db.doc("families/" + familyId + "/pairedDevices/" + childUid).get(),
  ]);

  if (!familySnap.exists || !childSnap.exists || !deviceSnap.exists) {
    return { verified: false };
  }

  const family = familySnap.data();
  const child = childSnap.data();
  const device = deviceSnap.data();
  const verified = family.ownerUid === device.ownerUid &&
    device.childUid === childUid &&
    device.familyId === familyId &&
    device.childId === childId &&
    device.status === "PAIRED" &&
    child.ownerUid === family.ownerUid &&
    child.pairedChildUid === childUid;

  return {
    verified: verified,
    familyId: verified ? familyId : "",
    ownerUid: verified ? family.ownerUid : "",
    childId: verified ? childId : "",
    childName: verified ? (child.name || device.childName || "Child") : "",
    codeId: verified ? (device.codeId || "") : "",
  };
});

/**
 * SOS push trigger. Parent clients must subscribe to family_sos_<familyId> after
 * verifying their family membership; never publish a topic token from an untrusted client.
 */
exports.onSafetyAlertCreated = onDocumentCreated(
  {
    document: "families/{familyId}/alerts/{alertId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const alert = snap.data();
    if (!alert || alert.category !== "SOS") return;

    const familyId = String(event.params.familyId || "");
    const message = {
      topic: "family_sos_" + familyId,
      notification: {
        title: alert.title || "Emergency SOS from " + (alert.childName || "Child"),
        body: alert.description || "Open Anchor Parent to view the emergency alert.",
      },
      android: { priority: "high" },
      data: {
        familyId: familyId,
        alertId: String(event.params.alertId || ""),
        category: "SOS",
        severity: String(alert.severity || "CRITICAL"),
      },
    };

    try {
      await admin.messaging().send(message);
    } catch (error) {
      console.error("SOS FCM dispatch failed:", error);
    }
  }
);
