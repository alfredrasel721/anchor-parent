import { test, before, after, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';

const require = createRequire('/usr/lib/node_modules/');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');

const DATABASE_ID = 'ai-studio-android-anchorpa-a481c5e6-6428-45e8-9bf4-3e58b56cbd1e';

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-test',
    firestore: {
      host: '127.0.0.1',
      port: 8085,
      rules: readFileSync('firestore.rules', 'utf8'),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

test('users collection: owner can create and read own profile; non-owner and unauthenticated denied', async () => {
  const sarahDb = testEnv.authenticatedContext('sarah_uid').firestore(DATABASE_ID);
  const attackerDb = testEnv.authenticatedContext('attacker_uid').firestore(DATABASE_ID);
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);

  const validProfile = {
    uid: 'sarah_uid',
    displayName: 'Sarah Jenkins',
    email: 'sarah.jenkins@family.org',
    role: 'PRIMARY_GUARDIAN',
    familyId: 'fam_sarah_uid',
    notificationCadence: 'ALL_EVENTS',
  };

  await assertSucceeds(sarahDb.doc('users/sarah_uid').set(validProfile));
  await assertSucceeds(sarahDb.doc('users/sarah_uid').get());
  await assertFails(attackerDb.doc('users/sarah_uid').get());
  await assertFails(unauthDb.doc('users/sarah_uid').get());
  await assertFails(attackerDb.doc('users/sarah_uid').set(validProfile));
});

test('families collection: owner can create, update, read household; non-owner denied; invalid schema denied', async () => {
  const sarahDb = testEnv.authenticatedContext('sarah_uid').firestore(DATABASE_ID);
  const attackerDb = testEnv.authenticatedContext('attacker_uid').firestore(DATABASE_ID);

  const validFamily = {
    familyId: 'fam_sarah_uid',
    familyName: 'The Jenkins Family',
    ownerUid: 'sarah_uid',
    subscriptionTier: 'FREE',
    subscriptionActive: false,
    autoApproveLearningApps: true,
    fairPlayNoticeMinutes: 5,
  };

  await assertSucceeds(sarahDb.doc('families/fam_sarah_uid').set(validFamily));
  await assertSucceeds(sarahDb.doc('families/fam_sarah_uid').get());
  await assertSucceeds(sarahDb.doc('families/fam_sarah_uid').update({
    subscriptionTier: 'PREMIUM_ANNUAL',
    subscriptionActive: true,
  }));

  // Non-owner cannot read or update
  await assertFails(attackerDb.doc('families/fam_sarah_uid').get());
  await assertFails(attackerDb.doc('families/fam_sarah_uid').update({ familyName: 'Hacked' }));

  // Owner cannot mutate ownerUid
  await assertFails(sarahDb.doc('families/fam_sarah_uid').update({ ownerUid: 'attacker_uid' }));

  // Invalid tier enum rejected
  await assertFails(sarahDb.doc('families/fam_bad').set({
    ...validFamily,
    familyId: 'fam_bad',
    subscriptionTier: 'INVALID_TIER',
  }));
});

test('children, appRules, schedules, geofences, requests, messages, tasks, guardians, premiumConfigs subcollections', async () => {
  const sarahDb = testEnv.authenticatedContext('sarah_uid').firestore(DATABASE_ID);
  const attackerDb = testEnv.authenticatedContext('attacker_uid').firestore(DATABASE_ID);
  const familyId = 'fam_sarah_uid';

  await assertSucceeds(sarahDb.doc(`families/${familyId}/children/child_leo`).set({
    childId: 'child_leo',
    familyId,
    ownerUid: 'sarah_uid',
    name: 'Leo',
    age: 11,
    dailyAllowanceMinutes: 150,
    usedMinutesToday: 105,
  }));
  await assertFails(attackerDb.doc(`families/${familyId}/children/child_leo`).get());

  await assertSucceeds(sarahDb.doc(`families/${familyId}/appRules/rule_roblox`).set({
    ruleId: 'rule_roblox',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    packageName: 'com.roblox.client',
    appName: 'Roblox',
    category: 'Games & Entertainment',
    dailyLimitMinutes: 45,
    usedMinutesToday: 45,
    status: 'ENFORCED',
    blocked: false,
    whitelisted: false,
    scheduleOnly: false,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/schedules/sched_bedtime`).set({
    scheduleId: 'sched_bedtime',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    title: 'Bedtime & Night Sleep',
    windowDescription: '8:30 PM — 7:00 AM',
    enabled: true,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/geofences/geo_school`).set({
    geofenceId: 'geo_school',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    name: 'Oakridge Middle School',
    radiusMeters: 250,
    alertsEnabled: true,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/alerts/alert_1`).set({
    alertId: 'alert_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    title: 'Arrived at Oakridge Middle School',
    category: 'GEOFENCE',
    severity: 'INFO',
    acknowledged: true,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/requests/req_1`).set({
    requestId: 'req_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    requestType: 'EXTRA_TIME',
    targetName: 'Minecraft',
    status: 'PENDING',
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/messages/msg_1`).set({
    messageId: 'msg_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    senderName: 'Sarah Jenkins',
    senderRole: 'PARENT',
    text: 'Great job finishing homework!',
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/tasks/task_1`).set({
    taskId: 'task_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    title: 'Math & French Homework',
    rewardSummary: '+15m Bonus Time',
    status: 'READY_FOR_REVIEW',
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/guardians/guard_1`).set({
    guardianId: 'guard_1',
    familyId,
    ownerUid: 'sarah_uid',
    name: 'Mark Jenkins',
    relationRole: 'Father • Co-Guardian',
    isPrimaryGuardian: true,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/premiumConfigs/prem_leo`).set({
    configId: 'prem_leo',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    webFilterProfile: 'BALANCED_GUARDIAN',
    socialShieldProfile: 'SUPERVISED',
  }));
});
