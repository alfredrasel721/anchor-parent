import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import worker, {
  normalizePairingCode,
  safePathSegment,
  encodeFirestoreValue,
  encodeFirestoreFields,
  decodeFirestoreValue,
  decodeFirestoreFields,
  verifyFirebaseIdToken
} from "../src/index.mjs";

test("normalizes six-digit pairing codes", () => {
  assert.equal(normalizePairingCode("123-456"), "123456");
  assert.equal(normalizePairingCode(" 123 456 "), "123456");
  assert.equal(normalizePairingCode("12345"), null);
  assert.equal(normalizePairingCode("1234567"), null);
});

test("rejects unsafe Firestore path segments", () => {
  assert.equal(safePathSegment("child_01", "childId"), "child_01");
  assert.throws(() => safePathSegment("../private", "childId"));
  assert.throws(() => safePathSegment("", "childId"));
});

test("encodes and decodes Firestore REST values", () => {
  const fields = encodeFirestoreFields({
    name: "Leo",
    active: true,
    age: 11,
    ratio: 1.5,
    createdAt: new Date("2026-11-10T00:00:00.000Z"),
    tags: ["safe", "home"],
    profile: { role: "child" },
    ignored: undefined
  });
  assert.deepEqual(decodeFirestoreFields(fields), {
    name: "Leo",
    active: true,
    age: 11,
    ratio: 1.5,
    createdAt: "2026-11-10T00:00:00.000Z",
    tags: ["safe", "home"],
    profile: { role: "child" }
  });
  assert.deepEqual(encodeFirestoreValue(null), { nullValue: null });
  assert.equal(decodeFirestoreValue({ integerValue: "42" }), 42);
});

test("health route works without Firebase credentials", async () => {
  const response = await worker.fetch(new Request("https://example.workers.dev/health"), {});
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { ok: true, service: "anchor-family-api", version: "1.0.0" });
});

test("pairing routes reject requests without Firebase bearer tokens", async () => {
  const response = await worker.fetch(new Request("https://example.workers.dev/v1/pairing/create", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ familyId: "fam_parent", childId: "child_1" })
  }), {});
  assert.equal(response.status, 401);
  const body = await response.json();
  assert.equal(body.error.code, "unauthenticated");
});

test("unknown routes do not disclose backend configuration", async () => {
  const response = await worker.fetch(new Request("https://example.workers.dev/v1/admin", { method: "POST" }), {});
  assert.equal(response.status, 404);
});

test("validates Firebase certificate structure and rejects an invalid token signature", async () => {
  const originalFetch = globalThis.fetch;
  const certificate = await readFile(new URL("./fixtures/firebase-test-cert.pem", import.meta.url), "utf8");
  const kid = "test-firebase-key";
  const now = Math.floor(Date.now() / 1000);
  const encode = (value) => Buffer.from(JSON.stringify(value)).toString("base64url");
  const unsigned = [
    encode({ alg: "RS256", typ: "JWT", kid }),
    encode({
      aud: "test-anchor-project",
      iss: "https://securetoken.google.com/test-anchor-project",
      sub: "child-user-123",
      iat: now, exp: now + 3600, auth_time: now - 10
    })
  ].join(".");
  const invalidSignature = Buffer.alloc(256).toString("base64url");
  const token = unsigned + "." + invalidSignature;
  globalThis.fetch = async () => new Response(JSON.stringify({ [kid]: certificate }), {
    status: 200, headers: { "content-type": "application/json", "cache-control": "public, max-age=300" }
  });
  try {
    await assert.rejects(() => verifyFirebaseIdToken(token, "test-anchor-project"), { code: "unauthenticated" });
    await assert.rejects(() => verifyFirebaseIdToken(token, "wrong-project"), { code: "unauthenticated" });
  } finally {
    globalThis.fetch = originalFetch;
  }
});
