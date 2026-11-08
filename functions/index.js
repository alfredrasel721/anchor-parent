/**
 * Anchor Family — Trusted Firebase Cloud Functions (Admin SDK)
 * Project ID: gen-lang-client-0963224775
 * Shared Firestore Database ID: ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e
 *
 * Implements:
 * 1. createPairingCode  - Parent generates a cryptographically secure, 10-minute one-time pairing code
 * 2. redeemPairingCode  - Child verifies & atomically consumes a pairing code and registers pairedDevices membership
 * 3. revokePairingCode  - Parent immediately revokes an active pairing code
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { getFirestore } = require("firebase-admin/firestore");
const crypto = require("node:crypto");

if (!admin.apps.length) {
  admin.initializeApp();
}

const DATABASE_ID = "ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e";
const PAIRING_TTL_MS = 10 * 60 * 1000; // 10 minutes
const MAX_REDEEM_ATTEMPTS = 5;
const RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000; // 15 minutes

function getSharedDb() {
  return getFirestore(admin.app(), DATABASE_ID);
}

function hashPairingCode(normalizedCode) {
  return crypto.createHash("sha256").update(normalizedCode).digest("hex");
}

function normalizePairingCode(rawCode) {
  const digits = String(rawCode || "").replace(/[^0-9]/g, "");
  if (digits.length !== 6) {
    return null;
  }
  return `${digits.slice(0, 3)}-${digits.slice(3, 6)}`;
}

function generateSecureSixDigitCode() {
  const part1 = crypto.randomInt(100, 1000);
  const part2 = crypto.randomInt(100, 1000);
  return `${part1}-${part2}`;
}

exports.createPairingCode = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required to generate a pairing code.");
  }

  const parentUid = request.auth.uid;
  const familyId = String(request.data?.familyId || `fam_${parentUid}`).trim();
  const childId = String(request.data?.childId || "").trim();
  const childName = String(request.data?.childName || "Child").trim();

  if (!familyId || !childId) {
    throw new HttpsError("invalid-argument", "Both familyId and childId are required.");
  }

  const db = getSharedDb();
  const familyRef = db.doc(`families/${familyId}`);
  const familySnap = await familyRef.get();

  if (familySnap.exists && familySnap.data().ownerUid !== parentUid) {
    throw new HttpsError("permission-denied", "Only the primary household guardian can generate pairing codes.");
  }

  const nowMs = Date.now();
  const expiresAtEpochMs = nowMs + PAIRING_TTL_MS;
  const code = generateSecureSixDigitCode();
  const codeHash = hashPairingCode(code);
  const codeId = `pair_${childId}_${nowMs}`;

  const pairingPayload = {
    codeId,
    familyId,
    ownerUid: parentUid,
    childId,
    childName,
    code,
    codeHash,
    expiresAtEpochMs,
    status: "ACTIVE",
    revoked: false,
    attemptCount: 0,
    maxAttempts: MAX_REDEEM_ATTEMPTS,
    consumedByChildUid: "",
    consumedAtEpochMs: 0,
    createdAtEpochMs: nowMs,
  };

  await db.runTransaction(async (tx) => {
    const activeQuery = db
      .collection(`families/${familyId}/pairingCodes`)
      .where("childId", "==", childId)
      .where("status", "==", "ACTIVE");
    const existingActive = await tx.get(activeQuery);
    existingActive.forEach((docSnap) => {
      tx.update(docSnap.ref, {
        status: "REVOKED",
        revoked: true,
        revokedAtEpochMs: nowMs,
      });
    });

    const codeRef = db.doc(`families/${familyId}/pairingCodes/${codeId}`);
    tx.set(codeRef, pairingPayload);
  });

  return {
    codeId,
    familyId,
    childId,
    childName,
    code,
    expiresAtEpochMs,
    status: "ACTIVE",
  };
});

exports.redeemPairingCode = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Child device must be authenticated before redeeming a pairing code.");
  }

  const childUid = request.auth.uid;
  const normalizedCode = normalizePairingCode(request.data?.code);
  const familyId = String(request.data?.familyId || "").trim();
  const codeId = String(request.data?.codeId || "").trim();
  const deviceName = String(request.data?.deviceName || "Android Child Device").trim();
  const deviceModel = String(request.data?.deviceModel || "Android").trim();
  const osVersion = String(request.data?.osVersion || "Android 14").trim();

  if (!normalizedCode || !familyId || !codeId) {
    throw new HttpsError("invalid-argument", "A valid 6-digit pairing code, familyId, and codeId are required.");
  }

  const db = getSharedDb();
  const nowMs = Date.now();
  const rateLimitRef = db.doc(`pairingRateLimits/${childUid}`);
  const codeRef = db.doc(`families/${familyId}/pairingCodes/${codeId}`);

  return await db.runTransaction(async (tx) => {
    const [rateSnap, codeSnap] = await Promise.all([
      tx.get(rateLimitRef),
      tx.get(codeRef),
    ]);

    if (rateSnap.exists) {
      const rateData = rateSnap.data();
      const windowStart = Number(rateData.windowStartEpochMs || 0);
      const failedCount = Number(rateData.failedCount || 0);
      if (nowMs - windowStart < RATE_LIMIT_WINDOW_MS && failedCount >= MAX_REDEEM_ATTEMPTS) {
        throw new HttpsError("resource-exhausted", "Too many failed pairing attempts. Please wait 15 minutes and generate a new code.");
      }
    }

    if (!codeSnap.exists) {
      throw new HttpsError("not-found", "Pairing code not found.");
    }

    const codeData = codeSnap.data();

    if (codeData.revoked === true || codeData.status === "REVOKED") {
      throw new HttpsError("failed-precondition", "This pairing code was revoked by the parent.");
    }

    if (codeData.status === "CONSUMED") {
      throw new HttpsError("already-exists", "This pairing code has already been used.");
    }

    if (Number(codeData.attemptCount || 0) >= Number(codeData.maxAttempts || MAX_REDEEM_ATTEMPTS)) {
      throw new HttpsError("resource-exhausted", "Maximum verification attempts exceeded for this pairing code.");
    }

    if (nowMs > Number(codeData.expiresAtEpochMs || 0)) {
      tx.update(codeRef, { status: "EXPIRED" });
      throw new HttpsError("deadline-exceeded", "This pairing code has expired (10-minute window elapsed).");
    }

    if (codeData.code !== normalizedCode) {
      const nextAttempts = Number(codeData.attemptCount || 0) + 1;
      tx.update(codeRef, {
        attemptCount: nextAttempts,
        status: nextAttempts >= MAX_REDEEM_ATTEMPTS ? "EXPIRED" : codeData.status,
      });
      tx.set(
        rateLimitRef,
        {
          childUid,
          failedCount: admin.firestore.FieldValue.increment(1),
          windowStartEpochMs: nowMs,
        },
        { merge: true }
      );
      throw new HttpsError("permission-denied", "Invalid pairing code.");
    }

    // Valid code! Atomically consume code and register pairedDevices membership
    tx.update(codeRef, {
      status: "CONSUMED",
      consumedByChildUid: childUid,
      consumedAtEpochMs: nowMs,
    });

    const pairedDeviceRef = db.doc(`families/${familyId}/pairedDevices/${childUid}`);
    const pairedDeviceRecord = {
      deviceId: childUid,
      childUid,
      familyId,
      ownerUid: codeData.ownerUid,
      childId: codeData.childId,
      childName: codeData.childName || "Child",
      codeId,
      redeemedCode: normalizedCode,
      deviceName,
      deviceModel,
      osVersion,
      status: "PAIRED",
      pairedAtEpochMs: nowMs,
      lastHeartbeatEpochMs: nowMs,
    };
    tx.set(pairedDeviceRef, pairedDeviceRecord);

    const childRef = db.doc(`families/${familyId}/children/${codeData.childId}`);
    tx.set(
      childRef,
      {
        pairedChildUid: childUid,
        deviceName,
        deviceModel,
        osVersion,
        syncStatus: "ENFORCED",
        lastSyncLabel: "Paired & Verified just now",
      },
      { merge: true }
    );

    return {
      verified: true,
      familyId,
      ownerUid: codeData.ownerUid,
      childId: codeData.childId,
      childName: codeData.childName || "Child",
      deviceId: childUid,
      pairedAtEpochMs: nowMs,
    };
  });
});

exports.revokePairingCode = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required to revoke a pairing code.");
  }

  const parentUid = request.auth.uid;
  const familyId = String(request.data?.familyId || `fam_${parentUid}`).trim();
  const codeId = String(request.data?.codeId || "").trim();

  if (!familyId || !codeId) {
    throw new HttpsError("invalid-argument", "Both familyId and codeId are required.");
  }

  const db = getSharedDb();
  const codeRef = db.doc(`families/${familyId}/pairingCodes/${codeId}`);
  const codeSnap = await codeRef.get();

  if (!codeSnap.exists) {
    throw new HttpsError("not-found", "Pairing code does not exist.");
  }

  if (codeSnap.data().ownerUid !== parentUid) {
    throw new HttpsError("permission-denied", "Only the family owner can revoke this pairing code.");
  }

  const nowMs = Date.now();
  await codeRef.update({
    status: "REVOKED",
    revoked: true,
    revokedAtEpochMs: nowMs,
  });

  return {
    revoked: true,
    codeId,
    familyId,
    revokedAtEpochMs: nowMs,
  };
});
