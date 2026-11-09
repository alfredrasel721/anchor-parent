const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'demo-anchor-local';
const DATABASE_ID = 'ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e';

let testEnv;
let testCounter = 0;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: '127.0.0.1',
      port: 8085,
      rules: fs.readFileSync('firestore.rules', 'utf8'),
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

async function seedVerifiedFamily() {
  testCounter += 1;
  const parentUid = `parent_${testCounter}`;
  const childUid = `child_device_${testCounter}`;
  const familyId = `fam_${parentUid}`;
  const childId = 'child_leo';
  const codeId = `pair_${testCounter}`;
  const lookupHash = `test_lookup_hash_${testCounter}`;

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore(DATABASE_ID);
    await db.doc(`families/${familyId}`).set({
      familyId,
      familyName: 'Test Household',
      ownerUid: parentUid,
      subscriptionTier: 'FREE',
      subscriptionActive: false,
    });
    await db.doc(`families/${familyId}/children/${childId}`).set({
      familyId,
      childId,
      ownerUid: parentUid,
      name: 'Leo',
      age: 11,
      dailyAllowanceMinutes: 120,
      usedMinutesToday: 35,
      batteryLevel: 80,
      locationLabel: 'Home',
    });
    await db.doc(`families/${familyId}/pairingCodes/${codeId}`).set({
      codeId,
      familyId,
      ownerUid: parentUid,
      childId,
      childName: 'Leo',
      code: '',
      expiresAtEpochMs: Date.now() + 600000,
      status: 'ACTIVE',
      revoked: false,
    });
    await db.doc(`families/${familyId}/pairedDevices/${childUid}`).set({
      deviceId: childUid,
      childUid,
      familyId,
      ownerUid: parentUid,
      childId,
      childName: 'Leo',
      codeId,
      redeemedCode: '',
      status: 'PAIRED',
      pairedAtEpochMs: Date.now(),
      lastHeartbeatEpochMs: Date.now(),
    });
    await db.doc(`pairingCodeLookups/${lookupHash}`).set({
      familyId,
      codeId,
      childId,
      status: 'ACTIVE',
    });
    await db.doc(`pairingCodeLookupIndexes/${codeId}`).set({
      familyId,
      codeId,
      lookupId: lookupHash,
      status: 'ACTIVE',
    });
    await db.doc(`pairingRateLimits/${childUid}`).set({
      childUid,
      failedCount: 1,
      windowStartEpochMs: Date.now(),
    });
  });

  return { parentUid, childUid, familyId, childId, codeId, lookupHash };
}

test('Parent can read pairing status but cannot create or modify pairing secrets from the app', async () => {
  const seeded = await seedVerifiedFamily();
  const parentDb = testEnv.authenticatedContext(seeded.parentUid).firestore(DATABASE_ID);
  const codePath = `families/${seeded.familyId}/pairingCodes/${seeded.codeId}`;

  const code = await assertSucceeds(parentDb.doc(codePath).get());
  assert.equal(code.data().status, 'ACTIVE');
  assert.equal(code.data().code, '');
  assert.equal(code.data().codeHash, undefined);
  await assertSucceeds(parentDb.collection(`families/${seeded.familyId}/pairingCodes`).get());

  await assertFails(parentDb.doc(codePath).update({ status: 'REVOKED' }));
  await assertFails(parentDb.doc(`families/${seeded.familyId}/pairingCodes/fake`).set({
    codeId: 'fake',
    familyId: seeded.familyId,
    ownerUid: seeded.parentUid,
    childId: seeded.childId,
    code: '123456',
    status: 'ACTIVE',
  }));
});

test('Verified Child can read family data and update telemetry, but cannot read pairing codes or change policy', async () => {
  const seeded = await seedVerifiedFamily();
  const childDb = testEnv.authenticatedContext(seeded.childUid).firestore(DATABASE_ID);

  await assertSucceeds(childDb.doc(`families/${seeded.familyId}`).get());
  await assertSucceeds(childDb.doc(`families/${seeded.familyId}/children/${seeded.childId}`).get());
  await assertSucceeds(childDb.doc(`families/${seeded.familyId}/pairedDevices/${seeded.childUid}`).get());

  await assertFails(childDb.doc(`families/${seeded.familyId}/pairingCodes/${seeded.codeId}`).get());
  await assertFails(childDb.collection(`families/${seeded.familyId}/pairingCodes`).get());

  const childPath = `families/${seeded.familyId}/children/${seeded.childId}`;
  await assertSucceeds(childDb.doc(childPath).update({
    usedMinutesToday: 48,
    batteryLevel: 72,
    locationLabel: 'School',
  }));
  await assertFails(childDb.doc(childPath).update({ dailyAllowanceMinutes: 999 }));
  await assertFails(childDb.doc(childPath).update({ ownerUid: seeded.childUid }));
});

test('No mobile client can read or write private pairing lookups, rate limits, or device membership', async () => {
  const seeded = await seedVerifiedFamily();
  const parentDb = testEnv.authenticatedContext(seeded.parentUid).firestore(DATABASE_ID);
  const childDb = testEnv.authenticatedContext(seeded.childUid).firestore(DATABASE_ID);
  const lookupPath = `pairingCodeLookups/${seeded.lookupHash}`;
  const ratePath = `pairingRateLimits/${seeded.childUid}`;
  const lookupIndexPath = `pairingCodeLookupIndexes/${seeded.codeId}`;
  const devicePath = `families/${seeded.familyId}/pairedDevices/${seeded.childUid}`;

  await assertFails(parentDb.doc(lookupPath).get());
  await assertFails(childDb.doc(lookupPath).get());
  await assertFails(parentDb.doc(lookupPath).set({ status: 'ACTIVE' }));
  await assertFails(childDb.doc(lookupPath).set({ status: 'ACTIVE' }));
  await assertFails(parentDb.doc(ratePath).get());
  await assertFails(childDb.doc(ratePath).get());
  await assertFails(parentDb.doc(ratePath).set({ failedCount: 0 }));
  await assertFails(childDb.doc(ratePath).set({ failedCount: 0 }));
  await assertFails(parentDb.doc(lookupIndexPath).get());
  await assertFails(childDb.doc(lookupIndexPath).get());
  await assertFails(parentDb.doc(lookupIndexPath).set({ lookupId: seeded.lookupHash }));
  await assertFails(childDb.doc(lookupIndexPath).set({ lookupId: seeded.lookupHash }));
  await assertFails(childDb.doc(devicePath).update({ status: 'PAIRED', childId: 'another_child' }));
  await assertFails(childDb.doc(`families/${seeded.familyId}/pairedDevices/fake_device`).set({
    deviceId: 'fake_device',
    childUid: 'fake_device',
    familyId: seeded.familyId,
    ownerUid: seeded.parentUid,
    childId: seeded.childId,
    codeId: seeded.codeId,
    status: 'PAIRED',
  }));
});

test('Verified Child can still send SOS, requests, and family messages', async () => {
  const seeded = await seedVerifiedFamily();
  const childDb = testEnv.authenticatedContext(seeded.childUid).firestore(DATABASE_ID);
  const familyPath = `families/${seeded.familyId}`;

  await assertSucceeds(childDb.doc(`${familyPath}/alerts/sos_${testCounter}`).set({
    alertId: `sos_${testCounter}`,
    familyId: seeded.familyId,
    ownerUid: seeded.parentUid,
    childId: seeded.childId,
    childName: 'Leo',
    title: 'Emergency SOS',
    category: 'SOS',
    severity: 'CRITICAL',
    acknowledged: false,
  }));
  await assertSucceeds(childDb.doc(`${familyPath}/requests/request_${testCounter}`).set({
    requestId: `request_${testCounter}`,
    familyId: seeded.familyId,
    ownerUid: seeded.parentUid,
    childId: seeded.childId,
    requestType: 'EXTRA_TIME',
    targetName: 'Learning App',
    status: 'PENDING',
  }));
  await assertSucceeds(childDb.doc(`${familyPath}/messages/message_${testCounter}`).set({
    messageId: `message_${testCounter}`,
    familyId: seeded.familyId,
    ownerUid: seeded.parentUid,
    childId: seeded.childId,
    senderName: 'Leo',
    senderRole: 'CHILD',
    text: 'I am safe.',
  }));
});
