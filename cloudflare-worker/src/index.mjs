const PROJECT_ID = "gen-lang-client-0963224775";
const DATABASE_ID = "ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e";
const FIREBASE_JWK_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
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

async function getFirebaseSigningKeys() {
  return cachedJson("https://anchor-internal.invalid/firebase-id-token-jwks", async () => {
    const response = await fetch(FIREBASE_JWK_URL);
    if (!response.ok) throw new AppError("unavailable", "Firebase identity verification is temporarily unavailable.", 503);
    const jwks = await response.json();
    const keys = Object.fromEntries((jwks.keys ?? []).map((key) => [key.kid, key]));
    const cacheControl = response.headers.get("cache-control") ?? "";
    const maxAge = Number(cacheControl.match(/max-age=(\d+)/i)?.[1] ?? 3600);
    return { value: keys, ttl: maxAge };
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
  const signingKeys = await getFirebaseSigningKeys();
  const jwk = signingKeys[header.kid];
  if (!jwk) throw new AppError("unauthenticated", "The Firebase sign-in token signing key is no longer valid.", 401);
  const key = await crypto.subtle.importKey(
    "jwk",
    jwk,
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
    const parent = parentPath ? `${encodePath(parentPath)}` : "";
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
    return (Array.isArray(result) ? result : []).nil
  }
}
