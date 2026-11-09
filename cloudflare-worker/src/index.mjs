const PROJECT_ID = "gen-lang-client-0963224775";
const DATABASE_ID = "ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e";
const FIREBASE_CERT_URL = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const MAX_REDEEM_ATTEMPTS = 5;
const RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000;
const PAIRING_TTL_MS = 10 * 60 * 1000;
const DELETE_FIELD = Symbol("delete-field");

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
  "Access-Control-Max-Age": "86400",
  "Vary": "Origin"
};

export class AppError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.name = "AppError";
    this.code = code;
    this.status = status;
  }
}

export function normalizePairingCode(rawCode) {
  const digits = String(rawCode ?? "").replace(/[^0-9]/g, "");
  return digits.length === 6 ? digits : null;
}

export function safePathSegment(value, label, maxLength = 120) {
  const text = String(value ?? "").trim();
  if (!text || text.length > maxLength || !/^[A-Za-z0-9_-]+$/.test(text)) {
    throw new AppError("invalid-argument", `${label} is invalid.`, 400);
  }
  return text;
}

export function encodeFirestoreValue(value) {
  if (value === null) return { nullValue: null };
  if (value instanceof Date) return { timestampValue: value.toISOString() };
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") {
    return Number.isSafeInteger(value)
      ? { integerValue: String(value) }
      : { doubleValue: value };
  }
  if (Array.isArray(value)) {
    return { arrayValue: { values: value.map(encodeFirestoreValue) } };
  }
  if (typeof value === "object") {
    return { mapValue: { fields: encodeFirestoreFields(value) } };
  }
  throw new TypeError(`Unsupported Firestore value type: ${typeof value}`);
}

export function encodeFirestoreFields(data) {
  const fields = {};
  for (const [key, value] of Object.entries(data ?? {})) {
    if (value === undefined || value === DELETE_FIELD) continue;
    fields[key] = encodeFirestoreValue(value);
  }
  return fields;
}

export function decodeFirestoreValue(value) {
  if (!value || typeof value !== "object") return null;
  if ("stringValue" in value) return value.stringValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return value.doubleValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("nullValue" in value) return null;
  if ("timestampValue" in value) return value.timestampValue;
  if ("referenceValue" in value) return value.referenceValue;
  if ("arrayValue" in value) return (value.arrayValue.values ?? []).map(decodeFirestoreValue);
  if ("mapValue" in value) return decodeFirestoreFields(value.mapValue.fields ?? {});
  return null;
}

export function decodeFirestoreFields(fields) {
  return Object.fromEntries(Object.entries(fields ?? {}).map(([key, value]) => [key, decodeFirestoreValue(value)]));
}

function jsonResponse(body, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...CORS_HEADERS,
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      ...extraHeaders
    }
  });
}

function statusForCode(code) {
  return ({
    "invalid-argument": 400,
    unauthenticated: 401,
    "permission-denied": 403,
    "not-found": 404,
    "already-exists": 409,
    "failed-precondition": 412,
    "deadline-exceeded": 410,
    "resource-exhausted": 429,
    unavailable: 503,
    internal: 500
  })[code] ?? 400;
}

function errorResponse(error) {
  const appError = error instanceof AppError
    ? error
    : new AppError("internal", "The Anchor service could not complete this request.", 500);
  if (!(error instanceof AppError)) {
    console.error(JSON.stringify({
      message: "Anchor API request failed",
      error: error instanceof Error ? error.message : String(error)
    }));
  }
  return jsonResponse({ error: { code: appError.code, message: appError.message } }, appError.status || statusForCode(appError.code));
}

function base64UrlDecode(value) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
  const binary = atob(padded);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function base64UrlEncodeBytes(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function decodeJwtPart(part) {
  return JSON.parse(new TextDecoder().decode(base64UrlDecode(part)));
}

function pemToDer(pem) {
  const body = pem.replace(/-----BEGIN CERTIFICATE-----|-----END CERTIFICATE-----|\s/g, "");
  const binary = atob(body);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function readDerElement(bytes, offset) {
  if (offset >= bytes.length) throw new Error("Invalid X.509 certificate encoding.");
  const tag = bytes[offset];
  let length = bytes[offset + 1];
  let headerLength = 2;
  if (length & 0x80) {
    const lengthBytes = length & 0x7f;
    if (lengthBytes < 1 || lengthBytes > 4 || offset + 2 + lengthBytes > bytes.length) {
      throw new Error("Invalid X.509 certificate length.");
    }
    length = 0;
    for (let index = 0; index < lengthBytes; index += 1) {
      length = (length * 256) + bytes[offset + 2 + index];
    }
    headerLength += lengthBytes;
  }
  const start = offset + headerLength;
  const end = start + length;
  if (end > bytes.length) throw new Error("Truncated X.509 certificate.");
  return { tag, offset, start, end, next: end };
}

function extractSubjectPublicKeyInfo(certificateDer) {
  const certificate = readDerElement(certificateDer, 0);
  if (certificate.tag !== 0x30) throw new Error("Invalid X.509 certificate sequence.");
  const tbs = readDerElement(certificateDer, certificate.start);
  if (tbs.tag !== 0x30) throw new Error("Invalid X.509 TBS certificate.");
  let cursor = tbs.start;
  let field = readDerElement(certificateDer, cursor);
  if (field.tag === 0xa0) {
    cursor = field.next;
    field = readDerElement(certificateDer, cursor);
  }
  // serialNumber, signature, issuer, validity, and subject precede SPKI.
  for (let index = 0; index < 5; index += 1) {
    cursor = field.next;
    field = readDerElement(certificateDer, cursor);
  }
  if (field.tag !== 0x30) throw new Error("X.509 certificate does not contain a valid public key.");
  return certificateDer.slice(field.offset, field.next);
}

async function cachedJson(cacheKey, loader, fallbackTtlSeconds = 300) {
  const cache = globalThis.caches?.default;
  const request = new Request(cacheKey);
  if (cache) {
    const cached = await cache.match(request);
    if (cached) {
      try { return await cached.json(); } catch { /* refresh invalid cache */ }
    }
  }
  const loaded = await loader();
  if (cache) {
    const response = new Response(JSON.stringify(loaded.value), {
      headers: { "Cache-Control": `public, max-age=${Math.max(30, Math.min(loaded.ttl ?? fallbackTtlSeconds, 3600))}` }
    });
    await cache.put(request, response);
  }
  return loaded.value;
}

async function getFirebaseSigningCerts() {
  return cachedJson("https://anchor-internal.invalid/firebase-id-token-certs", async () => {
    const response = await fetch(FIREBASE_CERT_URL);
    if (!response.ok) throw new AppError("unavailable", "Firebase identity verification is temporarily unavailable.", 503);
    const certs = await response.json();
    const cacheControl = response.headers.get("cache-control") ?? "";
    const maxAge = Number(cacheControl.match(/max-age=(\d+)/i)?.[1] ?? 3600);
    return { value: certs, ttl: maxAge };
  });
}

export async function verifyFirebaseIdToken(idToken, projectId = PROJECT_ID) {
  const parts = String(idToken ?? "").split(".");
  if (parts.length !== 3) throw new AppError("unauthenticated", "A valid Firebase sign-in token is required.", 401);
  let header;
  let claims;
  try {
    header = decodeJwtPart(parts[0]);
    claims = decodeJwtPart(parts[1]);
  } catch {
    throw new AppError("unauthenticated", "The Firebase sign-in token is malformed.", 401);
  }
  if (header.alg !== "RS256" || typeof header.kid !== "string") {
    throw new AppError("unauthenticated", "The Firebase sign-in token uses an unsupported signature.", 401);
  }
  const now = Math.floor(Date.now() / 1000);
  if (claims.aud !== projectId ||
      claims.iss !== `https://securetoken.google.com/${projectId}` ||
      typeof claims.sub !== "string" || claims.sub.length < 1 || claims.sub.length > 128 ||
      !Number.isFinite(claims.exp) || claims.exp <= now ||
      !Number.isFinite(claims.iat) || claims.iat > now + 60 ||
      !Number.isFinite(claims.auth_time) || claims.auth_time > now + 60) {
    throw new AppError("unauthenticated", "The Firebase sign-in token is expired or does not belong to this project.", 401);
  }
  const signingCerts = await getFirebaseSigningCerts();
  const certificate = signingCerts[header.kid];
  if (!certificate) throw new AppError("unauthenticated", "The Firebase sign-in token signing key is no longer valid.", 401);
  const key = await crypto.subtle.importKey(
    "spki",
    extractSubjectPublicKeyInfo(pemToDer(certificate)),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"]
  );
  const verified = await crypto.subtle.verify(
    { name: "RSASSA-PKCS1-v1_5" },
    key,
    base64UrlDecode(parts[2]),
    new TextEncoder().encode(`${parts[0]}.${parts[1]}`)
  );
  if (!verified) throw new AppError("unauthenticated", "The Firebase sign-in token signature is invalid.", 401);
  return { uid: claims.sub, claims };
}

function parseServiceAccount(env) {
  let serviceAccount;
  try {
    serviceAccount = typeof env.FIREBASE_SERVICE_ACCOUNT === "string"
      ? JSON.parse(env.FIREBASE_SERVICE_ACCOUNT)
      : env.FIREBASE_SERVICE_ACCOUNT;
  } catch {
    throw new AppError("unavailable", "The Anchor backend Firebase service credential is not configured correctly.", 503);
  }
  const projectId = env.FIREBASE_PROJECT_ID || PROJECT_ID;
  if (!serviceAccount || serviceAccount.project_id !== projectId ||
      !serviceAccount.client_email || !serviceAccount.private_key) {
    throw new AppError("unavailable", "The Anchor backend Firebase service credential is missing or belongs to another project.", 503);
  }
  return { serviceAccount, projectId };
}

async function getGoogleAccessToken(serviceAccount, scopes) {
  const scope = [...scopes].sort().join(" ");
  const cacheKey = `https://anchor-internal.invalid/google-access-token/${encodeURIComponent(serviceAccount.client_email)}/${encodeURIComponent(scope)}`;
  const cached = await cachedJson(cacheKey, async () => {
    const now = Math.floor(Date.now() / 1000);
    const header = base64UrlEncodeBytes(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
    const payload = base64UrlEncodeBytes(new TextEncoder().encode(JSON.stringify({
      iss: serviceAccount.client_email,
      scope,
      aud: GOOGLE_TOKEN_URL,
      iat: now,
      exp: now + 3600
    })));
    const unsigned = `${header}.${payload}`;
    const privateKey = serviceAccount.private_key.replace(/\\n/g, "\n");
    const pemBody = privateKey.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
    const pkcs8 = Uint8Array.from(atob(pemBody), (character) => character.charCodeAt(0));
    const key = await crypto.subtle.importKey(
      "pkcs8", pkcs8,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["sign"]
    );
    const signature = await crypto.subtle.sign(
      { name: "RSASSA-PKCS1-v1_5" }, key, new TextEncoder().encode(unsigned)
    );
    const assertion = `${unsigned}.${base64UrlEncodeBytes(new Uint8Array(signature))}`;
    const response = await fetch(GOOGLE_TOKEN_URL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion
      })
    });
    if (!response.ok) {
      console.error(JSON.stringify({ message: "Google OAuth token exchange failed", status: response.status }));
      throw new AppError("unavailable", "The Anchor backend could not authenticate with Firebase. Check the service account permissions.", 503);
    }
    const result = await response.json();
    return { value: { accessToken: result.access_token }, ttl: Math.max(30, Number(result.expires_in ?? 3600) - 90) };
  }, 3000);
  return cached.accessToken;
}

function encodePath(path) {
  return path.split("/").map(encodeURIComponent).join("/");
}

class FirestoreRest {
  constructor(projectId, databaseId, accessToken) {
    this.documentsUrl = `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/databases/${encodeURIComponent(databaseId)}/documents`;
    this.accessToken = accessToken;
  }

  async request(method, path, body) {
    const response = await fetch(`${this.documentsUrl}${path}`, {
      method,
      headers: {
        Authorization: `Bearer ${this.accessToken}`,
        "Content-Type": "application/json"
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) })
    });
    const raw = await response.text();
    let data = {};
    try { data = raw ? JSON.parse(raw) : {}; } catch { data = { raw }; }
    if (!response.ok) {
      const status = data.error?.status ?? "INTERNAL";
      const code = status === "ABORTED" ? "aborted"
        : status === "NOT_FOUND" ? "not-found"
        : status === "ALREADY_EXISTS" ? "already-exists"
        : status === "PERMISSION_DENIED" ? "permission-denied"
        : status === "UNAUTHENTICATED" ? "unauthenticated"
        : status === "RESOURCE_EXHAUSTED" ? "resource-exhausted"
        : status === "FAILED_PRECONDITION" ? "failed-precondition"
        : "unavailable";
      const httpStatus = response.status === 409 ? 409
        : response.status === 404 ? 404
        : response.status === 403 ? 403
        : response.status === 401 ? 401
        : response.status === 429 ? 429
        : response.status >= 500 ? 503
        : response.status >= 400 ? 400 : 500;
      const error = new AppError(code, data.error?.message || "Firebase database request failed.", httpStatus);
      error.firestoreStatus = status;
      throw error;
    }
    return data;
  }

  documentName(path) {
    return `${this.documentsUrl}/${encodePath(path)}`;
  }

  async beginTransaction() {
    const result = await this.request("POST", ":beginTransaction", { options: { readWrite: {} } });
    return result.transaction;
  }

  async rollback(transaction) {
    return this.request("POST", ":rollback", { transaction });
  }

  async commit(transaction, writes) {
    return this.request("POST", ":commit", { transaction, writes });
  }

  async get(path, transaction) {
    const query = transaction ? `?transaction=${encodeURIComponent(transaction)}` : "";
    try {
      const document = await this.request("GET", `/${encodePath(path)}${query}`);
      return { id: path.split("/").at(-1), data: decodeFirestoreFields(document.fields), document };
    } catch (error) {
      if (error instanceof AppError && error.status === 404) return null;
      throw error;
    }
  }

  async query(collectionPath, field, value, transaction) {
    const segments = collectionPath.split("/");
    const collectionId = segments.pop();
    const parentPath = segments.join("/");
    const parent = parentPath ? `/${encodePath(parentPath)}` : "";
    const result = await this.request("POST", `${parent}:runQuery`, {
      structuredQuery: {
        from: [{ collectionId }],
        where: {
          fieldFilter: {
            field: { fieldPath: field },
            op: "EQUAL",
            value: encodeFirestoreValue(value)
          }
        },
        limit: 50
      },
      transaction
    });
    return (Array.isArray(result) ? result : []).filter((row) => row.document).map((row) => ({
      id: row.document.name.split("/").at(-1),
      data: decodeFirestoreFields(row.document.fields),
      document: row.document
    }));
  }
}

function makeSetWrite(db, path, data, options = {}) {
  const write = {
    update: { name: db.documentName(path), fields: encodeFirestoreFields(data) }
  };
  if (options.create) write.currentDocument = { exists: false };
  if (options.merge) write.updateMask = { fieldPaths: Object.keys(data) };
  return write;
}

function makeUpdateWrite(db, path, data) {
  return {
    update: { name: db.documentName(path), fields: encodeFirestoreFields(data) },
    updateMask: { fieldPaths: Object.keys(data) }
  };
}

async function runTransaction(db, handler, maxAttempts = 3) {
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const transaction = await db.beginTransaction();
    const writes = [];
    const tx = {
      get: (path) => db.get(path, transaction),
      query: (path, field, value) => db.query(path, field, value, transaction),
      set: (path, data, options = {}) => writes.push(makeSetWrite(db, path, data, options)),
      update: (path, data) => writes.push(makeUpdateWrite(db, path, data)),
      delete: (path) => writes.push({ delete: db.documentName(path) })
    };
    try {
      const result = await handler(tx);
      if (writes.length) await db.commit(transaction, writes);
      else await db.rollback(transaction).catch(() => {});
      return result;
    } catch (error) {
      await db.rollback(transaction).catch(() => {});
      if (error instanceof AppError && error.firestoreStatus === "ABORTED" && attempt < maxAttempts - 1) continue;
      throw error;
    }
  }
  throw new AppError("unavailable", "The database is busy. Please try again.", 503);
}

async function sha256Hex(value) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function randomHex(byteLength = 16) {
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function safeDeviceValue(value, fallback, maxLength) {
  const normalized = String(value ?? fallback).replace(/[\u0000-\u001f\u007f]/g, " ").trim();
  return (normalized || fallback).slice(0, maxLength);
}

function requireUid(user) {
  if (!user?.uid) throw new AppError("unauthenticated", "Sign in before using Anchor.", 401);
  return user.uid;
}

async function createPairingCode(db, uid, body) {
  const familyId = safePathSegment(body.familyId, "familyId");
  const childId = safePathSegment(body.childId, "childId");
  const childName = safeDeviceValue(body.childName, "Child", 100);
  if (familyId !== `fam_${uid}`) throw new AppError("permission-denied", "Only the verified family owner can create pairing codes.", 403);

  for (let attempt = 0; attempt < 5; attempt += 1) {
    const code = String(crypto.getRandomValues(new Uint32Array(1))[0] % 900000 + 100000);
    const codeHash = await sha256Hex(code);
    const codeId = `pair_${childId}_${crypto.randomUUID()}`;
    const nowMs = Date.now();
    const expiresAtEpochMs = nowMs + PAIRING_TTL_MS;
    const outcome = await runTransaction(db, async (tx) => {
      const [familySnap, childSnap, lookupSnap] = await Promise.all([
        tx.get(`families/${familyId}`),
        tx.get(`families/${familyId}/children/${childId}`),
        tx.get(`pairingCodeLookups/${codeHash}`)
      ]);
      if (!familySnap || familySnap.data.ownerUid !== uid) {
        throw new AppError("permission-denied", "Only the verified family owner can create pairing codes.", 403);
      }
      if (!childSnap || childSnap.data.ownerUid !== uid || childSnap.data.familyId !== familyId || childSnap.data.childId !== childId) {
        throw new AppError("not-found", "The selected child profile does not belong to this family.", 404);
      }
      if (lookupSnap) return { collision: true };

      const activeCodes = await tx.query(`families/${familyId}/pairingCodes`, "childId", childId);
      const stale = [];
      for (const activeCodeSnap of activeCodes) {
        const activeCode = activeCodeSnap.data;
        if (activeCode.status !== "ACTIVE") continue;
        const indexPath = `pairingCodeLookupIndexes/${activeCodeSnap.id}`;
        const indexSnap = await tx.get(indexPath);
        const lookupIds = new Set();
        if (indexSnap?.data.lookupId) lookupIds.add(indexSnap.data.lookupId);
        if (activeCode.codeHash) lookupIds.add(activeCode.codeHash);
        stale.push({ codeId: activeCodeSnap.id, lookupIds: [...lookupIds], hasIndex: Boolean(indexSnap) });
      }

      for (const old of stale) {
        tx.update(`families/${familyId}/pairingCodes/${old.codeId}`, {
          status: "REVOKED", revoked: true, revokedAtEpochMs: nowMs
        });
        for (const oldLookupId of old.lookupIds) tx.delete(`pairingCodeLookups/${oldLookupId}`);
        if (old.hasIndex) tx.delete(`pairingCodeLookupIndexes/${old.codeId}`);
      }
      tx.set(`families/${familyId}/pairingCodes/${codeId}`, {
        codeId, familyId, ownerUid: uid, childId, childName, code: "",
        expiresAtEpochMs, status: "ACTIVE", revoked: false,
        attemptCount: 0, maxAttempts: MAX_REDEEM_ATTEMPTS,
        consumedByChildUid: "", consumedAtEpochMs: 0,
        createdAtEpochMs: nowMs, createdAt: new Date(nowMs)
      }, { create: true });
      tx.set(`pairingCodeLookups/${codeHash}`, {
        familyId, codeId, childId, ownerUid: uid, status: "ACTIVE",
        expiresAtEpochMs, createdAtEpochMs: nowMs
      }, { create: true });
      tx.set(`pairingCodeLookupIndexes/${codeId}`, {
        familyId, codeId, lookupId: codeHash, status: "ACTIVE", createdAtEpochMs: nowMs
      }, { create: true });
      tx.update(`families/${familyId}/children/${childId}`, {
        activePairingCodeId: codeId, updatedAt: new Date(nowMs)
      });
      return { collision: false };
    });
    if (!outcome.collision) {
      return {
        verified: true, codeId, familyId, ownerUid: uid, childId, childName,
        code: `${code.slice(0, 3)}-${code.slice(3)}`,
        expiresAtEpochMs, status: "ACTIVE"
      };
    }
  }
  throw new AppError("resource-exhausted", "Could not allocate a unique pairing code. Please retry.", 429);
}

async function redeemPairingCode(db, uid, body) {
  const code = normalizePairingCode(body.code);
  if (!code) throw new AppError("invalid-argument", "Enter the six-digit code shown in Anchor Parent.", 400);
  const deviceName = safeDeviceValue(body.deviceName, "Android Child Device", 100);
  const deviceModel = safeDeviceValue(body.deviceModel, "Android", 100);
  const osVersion = safeDeviceValue(body.osVersion, "Android", 50);
  const nowMs = Date.now();
  const codeHash = await sha256Hex(code);
  const outcome = await runTransaction(db, async (tx) => {
    const [rateSnap, lookupSnap] = await Promise.all([
      tx.get(`pairingRateLimits/${uid}`),
      tx.get(`pairingCodeLookups/${codeHash}`)
    ]);
    const rateData = rateSnap?.data ?? {};
    const oldWindowStart = Number(rateData.windowStartEpochMs ?? 0);
    const sameWindow = oldWindowStart > 0 && nowMs - oldWindowStart < RATE_LIMIT_WINDOW_MS;
    const windowStartEpochMs = sameWindow ? oldWindowStart : nowMs;
    const failedCount = sameWindow ? Number(rateData.failedCount ?? 0) : 0;
    if (failedCount >= MAX_REDEEM_ATTEMPTS) {
      return { errorCode: "resource-exhausted", errorMessage: "Too many attempts. Wait 15 minutes before trying again." };
    }
    if (!lookupSnap || lookupSnap.data.status !== "ACTIVE") {
      tx.set(`pairingRateLimits/${uid}`, {
        childUid: uid, failedCount: failedCount + 1,
        windowStartEpochMs, lastAttemptEpochMs: nowMs
      });
      return { errorCode: "not-found", errorMessage: "Invalid or expired pairing code. Check the code in Anchor Parent." };
    }

    const lookup = lookupSnap.data;
    const familyId = String(lookup.familyId ?? "");
    const codeId = String(lookup.codeId ?? "");
    const childId = String(lookup.childId ?? "");
    if (!familyId || !codeId || !childId || !/^[A-Za-z0-9_-]+$/.test(familyId) || !/^[A-Za-z0-9_-]+$/.test(childId)) {
      tx.delete(`pairingCodeLookups/${codeHash}`);
      return { errorCode: "failed-precondition", errorMessage: "Pairing code is no longer valid. Ask the Parent to generate a new one." };
    }

    const [codeSnap, familySnap, childSnap, deviceSnap, lookupIndexSnap] = await Promise.all([
      tx.get(`families/${familyId}/pairingCodes/${codeId}`),
      tx.get(`families/${familyId}`),
      tx.get(`families/${familyId}/children/${childId}`),
      tx.get(`families/${familyId}/pairedDevices/${uid}`),
      tx.get(`pairingCodeLookupIndexes/${codeId}`)
    ]);
    if (!codeSnap || !familySnap || !childSnap) {
      tx.delete(`pairingCodeLookups/${codeHash}`);
      if (lookupIndexSnap) tx.delete(`pairingCodeLookupIndexes/${codeId}`);
      return { errorCode: "failed-precondition", errorMessage: "The Parent profile is no longer available. Ask the Parent to generate a new code." };
    }
    const codeData = codeSnap.data;
    const familyData = familySnap.data;
    const childData = childSnap.data;
    if (codeData.status !== "ACTIVE" || lookup.expiresAtEpochMs !== codeData.expiresAtEpochMs) {
      tx.delete(`pairingCodeLookups/${codeHash}`);
      if (lookupIndexSnap) tx.delete(`pairingCodeLookupIndexes/${codeId}`);
      return { errorCode: "failed-precondition", errorMessage: "This pairing code has already been used or revoked." };
    }
    if (Number(codeData.expiresAtEpochMs ?? 0) <= nowMs) {
      tx.update(`families/${familyId}/pairingCodes/${codeId}`, { status: "EXPIRED" });
      tx.delete(`pairingCodeLookups/${codeHash}`);
      if (lookupIndexSnap) tx.delete(`pairingCodeLookupIndexes/${codeId}`);
      return { errorCode: "deadline-exceeded", errorMessage: "This pairing code expired. Ask the Parent for a new code." };
    }
    if (familyData.ownerUid !== codeData.ownerUid || childData.ownerUid !== codeData.ownerUid ||
        childData.familyId !== familyId || childData.childId !== codeData.childId) {
      tx.delete(`pairingCodeLookups/${codeHash}`);
      if (lookupIndexSnap) tx.delete(`pairingCodeLookupIndexes/${codeId}`);
      return { errorCode: "permission-denied", errorMessage: "The pairing code does not match a valid family profile." };
    }
    if (childData.pairedChildUid && childData.pairedChildUid !== uid) {
      return { errorCode: "already-exists", errorMessage: "This child profile is already paired to another device. Ask the Parent to unpair it first." };
    }
    if (deviceSnap && deviceSnap.data.status === "PAIRED" && deviceSnap.data.childId !== codeData.childId) {
      return { errorCode: "already-exists", errorMessage: "This account is already paired to another child profile." };
    }

    tx.update(`families/${familyId}/pairingCodes/${codeId}`, {
      status: "CONSUMED", consumedByChildUid: uid, consumedAtEpochMs: nowMs
    });
    tx.delete(`pairingCodeLookups/${codeHash}`);
    if (lookupIndexSnap) tx.delete(`pairingCodeLookupIndexes/${codeId}`);
    tx.set(`families/${familyId}/pairedDevices/${uid}`, {
      deviceId: uid, childUid: uid, familyId, ownerUid: codeData.ownerUid,
      childId: codeData.childId, childName: codeData.childName || "Child", codeId,
      redeemedCode: "", deviceName, deviceModel, osVersion, status: "PAIRED",
      pairedAtEpochMs: nowMs, lastHeartbeatEpochMs: nowMs, updatedAt: new Date(nowMs)
    });
    tx.update(`families/${familyId}/children/${childId}`, {
      pairedChildUid: uid, activePairingCodeId: DELETE_FIELD,
      deviceName, deviceModel, osVersion, syncStatus: "ENFORCED",
      lastSyncLabel: "Paired & verified", updatedAt: new Date(nowMs)
    });
    tx.delete(`pairingRateLimits/${uid}`);
    return {
      verified: true, familyId, ownerUid: codeData.ownerUid,
      childId: codeData.childId, childName: codeData.childName || "Child",
      codeId, deviceId: uid, pairedAtEpochMs: nowMs
    };
  });
  if (outcome.errorCode) throw new AppError(outcome.errorCode, outcome.errorMessage, statusForCode(outcome.errorCode));
  return outcome;
}


async function revokePairingCode(db, uid, body) {
  const familyId = safePathSegment(body.familyId, "familyId");
  const codeId = body.codeId ? safePathSegment(body.codeId, "codeId") : "";
  const deviceUid = body.deviceUid ? safePathSegment(body.deviceUid, "deviceUid") : "";
  if (familyId !== `fam_${uid}` || (!codeId && !deviceUid)) {
    throw new AppError("invalid-argument", "A valid familyId and codeId or deviceUid are required.", 400);
  }
  const nowMs = Date.now();
  await runTransaction(db, async (tx) => {
    const familySnap = await tx.get(`families/${familyId}`);
    if (!familySnap || familySnap.data.ownerUid !== uid) {
      throw new AppError("permission-denied", "Only the family owner can revoke pairing.", 403);
    }
    let codeSnap = null;
    let indexSnap = null;
    let deviceSnap = null;
    let childSnap = null;
    if (codeId) {
      [codeSnap, indexSnap] = await Promise.all([
        tx.get(`families/${familyId}/pairingCodes/${codeId}`),
        tx.get(`pairingCodeLookupIndexes/${codeId}`)
      ]);
      if (!codeSnap || codeSnap.data.ownerUid !== uid) {
        throw new AppError("not-found", "Pairing code not found for this family.", 404);
      }
    }
    if (deviceUid) {
      deviceSnap = await tx.get(`families/${familyId}/pairedDevices/${deviceUid}`);
      if (deviceSnap) {
        childSnap = await tx.get(`families/${familyId}/children/${safePathSegment(deviceSnap.data.childId, "childId")}`);
        if (deviceSnap.data.ownerUid !== uid) {
          throw new AppError("permission-denied", "Device does not belong to this family.", 403);
        }
      }
    }
    if (codeSnap) {
      tx.update(`families/${familyId}/pairingCodes/${codeId}`, {
        status: "REVOKED", revoked: true, revokedAtEpochMs: nowMs
      });
      const lookupId = indexSnap?.data.lookupId || codeSnap.data.codeHash;
      if (lookupId) tx.delete(`pairingCodeLookups/${safePathSegment(lookupId, "lookupId", 128)}`);
      if (indexSnap) tx.delete(`pairingCodeLookupIndexes/${codeId}`);
    }
    if (deviceSnap) {
      tx.delete(`families/${familyId}/pairedDevices/${deviceUid}`);
      if (childSnap && childSnap.data.pairedChildUid === deviceUid) {
        tx.update(`families/${familyId}/children/${deviceSnap.data.childId}`, {
          pairedChildUid: DELETE_FIELD, syncStatus: "PENDING",
          lastSyncLabel: "Device unpaired by Parent", updatedAt: new Date(nowMs)
        });
      }
    }
  });
  return { revoked: true, familyId, codeId: codeId || null, deviceUid: deviceUid || null };
}

async function verifyPairingSession(db, uid, body) {
  const familyId = safePathSegment(body.familyId, "familyId");
  const childId = safePathSegment(body.childId, "childId");
  return runTransaction(db, async (tx) => {
    const [familySnap, childSnap, deviceSnap] = await Promise.all([
      tx.get(`families/${familyId}`),
      tx.get(`families/${familyId}/children/${childId}`),
      tx.get(`families/${familyId}/pairedDevices/${uid}`)
    ]);
    if (!familySnap || !childSnap || !deviceSnap) return { verified: false };
    const family = familySnap.data;
    const child = childSnap.data;
    const device = deviceSnap.data;
    const verified = family.ownerUid === device.ownerUid &&
      device.childUid === uid && device.familyId === familyId &&
      device.childId === childId && device.status === "PAIRED" &&
      child.ownerUid === family.ownerUid && child.pairedChildUid === uid;
    return {
      verified,
      familyId: verified ? familyId : "",
      ownerUid: verified ? family.ownerUid : "",
      childId: verified ? childId : "",
      childName: verified ? (child.name || device.childName || "Child") : "",
      codeId: verified ? (device.codeId || "") : ""
    };
  });
}

async function getOrCreateSosTopic(db, uid, body) {
  const familyId = safePathSegment(body.familyId, "familyId");
  if (familyId !== `fam_${uid}`) throw new AppError("permission-denied", "Only the family owner can register Parent notifications.", 403);
  return runTransaction(db, async (tx) => {
    const [familySnap, topicSnap] = await Promise.all([
      tx.get(`families/${familyId}`),
      tx.get(`familyNotificationTopics/${familyId}`)
    ]);
    if (!familySnap || familySnap.data.ownerUid !== uid) {
      throw new AppError("permission-denied", "Only the verified family owner can register Parent notifications.", 403);
    }
    if (topicSnap?.data.topic) return { topic: topicSnap.data.topic };
    const topic = `anchor_sos_${randomHex(24)}`;
    tx.set(`familyNotificationTopics/${familyId}`, {
      familyId, ownerUid: uid, topic, createdAt: new Date()
    }, { create: true });
    return { topic };
  });
}

async function sendFcmTopic(accessToken, projectId, topic, payload) {
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      message: {
        topic,
        notification: { title: payload.title, body: payload.body },
        android: { priority: payload.priority ?? "HIGH" },
        data: payload.data
      }
    })
  });
  if (!response.ok) {
    const errorText = await response.text();
    console.error(JSON.stringify({ message: "FCM SOS notification failed", status: response.status, detail: errorText.slice(0, 300) }));
    return false;
  }
  return true;
}

async function dispatchSos(db, accessToken, projectId, uid, body) {
  const familyId = safePathSegment(body.familyId, "familyId");
  const childId = safePathSegment(body.childId, "childId");
  const childName = safeDeviceValue(body.childName, "Child", 100);
  const locationLabel = safeDeviceValue(body.locationLabel, "Location unavailable", 240);
  const batteryLevel = Math.max(0, Math.min(100, Number(body.batteryLevel) || 0));
  const isPracticeDrill = body.isPracticeDrill === true;
  const alertId = body.alertId ? safePathSegment(body.alertId, "alertId") : `sos_${crypto.randomUUID()}`;
  const now = new Date();
  const result = await runTransaction(db, async (tx) => {
    const [familySnap, childSnap, deviceSnap, topicSnap] = await Promise.all([
      tx.get(`families/${familyId}`),
      tx.get(`familys/${familyId}/children/${childId}`),
      tx.get(`families/${familyId}/pairedDevices/${uid}`),
      tx.get(`familyNotificationTopics/${familyId}`)
    ]);
    if (!familySnap || !childSnap || !deviceSnap) {
      throw new AppError("permission-denied", "This Child device is not paired to the requested family.", 403);
    }
    const family = familySnap.data;
    const child = childSnap.data;
    const device = deviceSnap.data;
    const verified = family.ownerUid === device.ownerUid &&
      child.ownerUid === family.ownerUid && child.familyId === familyId &&
      child.childId === childId && child.pairedChildUid === uid &&
      device.childUid === uid && device.familyId === familyId &&
      device.childId === childId && device.status === "PAIRED";
    if (!verified) throw new AppError("permission-denied", "This Child device is no longer paired to this family.", 403);
    const title = isPracticeDrill
      ? `SOS Practice Drill Completed (${childName})`
      : `URGENT SOS Emergency Beacon (${childName})`;
    const description = `Location: ${locationLabel} • Battery: ${batteryLevel}%" • Dispatched to Primary & Co-Parent Guardians.`;
    tx.set(`families/${familyId}/alerts/${alertId}`, {
      alertId, familyId, ownerUid: family.ownerUid, childId, childName,
      title, description, category: "SOS",
      severity: isPracticeDrill ? "WARNING" : "CRITICAL",
      timestampLabel: `Today, ${now.toISOString()}`,
      acknowledged: false, createdAt: now
    }, { create: true });
    return { alertId, title, description, topic: topicSnap?.data.topic || null };
  });
  const notificationSent = result.topic
    ? await sendFcmTopic(accessToken, projectId, result.topic, {
        title: result.title,
        body: result.description,
        priority: isPracticeDrill ? "NORMAL" : "HIGH",
        data: {
          familyId, alertId: result.alertId, category: "SOS",
          severity: isPracticeDrill ? "WARNING" : "CRITICAL"
        }
      })
    : false;
  return { alertId: result.alertId, notificationSent };
}

async function readJsonBody(request) {
  const contentLength = Number(request.headers.get("content-length") ?? 0);
  if (contentLength > 16 * 1024) throw new AppError("invalid-argument", "Request body is too large.", 400);
  let text;
  try { text = await request.text(); } catch { throw new AppError("invalid-argument", "Request body could not be read.", 400); }
  if (text.length > 16 * 1024) throw new AppError("invalid-argument", "Request body is too large.", 400);
  try { return text ? JSON.parse(text) : {}; }
  catch { throw new AppError("invalid-argument", "Request body must be valid JSON.", 400); }
}

export async function handleRequest(request, env) {
  const url = new URL(request.url);
  if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS_HEADERS });
  if (url.pathname === "/health" && request.method === "GET") {
    return jsonResponse({ ok: true, service: "anchor-family-api", version: "1.0.0" });
  }
  if (request.method !== "POST") return jsonResponse({ error: { code: "not-found", message: "Route not found." } }, 404);
  const routes = new Set([
    "/v1/pairing/create", "/v1/pairing/redeem", "/v1/pairing/revoke",
    "/v1/pairing/verify", "/v1/notifications/topic", "/v1/sos"
  ]);
  if (!routes.has(url.pathname)) return jsonResponse({ error: { code: "not-found", message: "Route not found." } }, 404);

  const authorization = request.headers.get("authorization") ?? "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  if (!match) throw new AppError("unauthenticated", "Sign in before using Anchor.", 401);
  const projectId = env.FIREBASE_PROJECT_ID || PROJECT_ID;
  const databaseId = env.FIRESTORE_DATABASE_ID || DATABASE_ID;
  const user = await verifyFirebaseIdToken(match[1], projectId);
  const body = await readJsonBody(request);
  const { serviceAccount } = parseServiceAccount({ ...env, FIREBASE_PROJECT_ID: projectId });
  const scopes = url.pathname === "/v1/sos"
    ? [FIRESTORE_SCOPE, FCM_SCOPE]
    : [FIRESTORE_SCOPE];
  const accessToken = await getGoogleAccessToken(serviceAccount, scopes);
  const db = new FirestoreRest(projectId, databaseId, accessToken);
  const uid = requireUid(user);

  switch (url.pathname) {
    case "/v1/pairing/create":
      return jsonResponse(await createPairingCode(db, uid, body));
    case "/v1/pairing/redeem":
      return jsonResponse(await redeemPairingCode(db, uid, body));
    case "/v1/pairing/revoke":
      return jsonResponse(await revokePairingCode(db, uid, body));
    case "/v1/pairing/verify":
      return jsonResponse(await verifyPairingSession(db, uid, body));
    case "/v1/notifications/topic":
      return jsonResponse(await getOrCreateSosTopic(db, uid, body));
    case "/v1/sos":
      return jsonResponse(await dispatchSos(db, accessToken, projectId, uid, body));
    default:
      return jsonResponse({ error: { code: "not-found", message: "Route not found." } }, 404);
  }
}

export default {
  async fetch(request, env, ctx) {
    try {
      return await handleRequest(request, env);
    } catch (error) {
      return errorResponse(error);
    }
  }
};
