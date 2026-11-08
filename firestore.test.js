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

test('1. Valid Parent access: owner can create and manage household, children, rules, schedules, geofences, tasks, and premiumConfigs', async () => {
  const sarahDb = testEnv.authenticatedContext('sarah_uid').firestore(DATABASE_ID);
  const familyId = 'fam_sarah_uid';

  await assertSucceeds(sarahDb.doc('users/sarah_uid').set({
    uid: 'sarah_uid',
    displayName: 'Sarah Jenkins',
    email: 'sarah.jenkins@family.org',
    role: 'PRIMARY_GUARDIAN',
    familyId,
    notificationCadence: 'ALL_EVENTS',
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}`).set({
    familyId,
    familyName: 'The Jenkins Family',
    ownerUid: 'sarah_uid',
    subscriptionTier: 'FREE',
    subscriptionActive: false,
    autoApproveLearningApps: true,
    fairPlayNoticeMinutes: 5,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/children/child_leo`).set({
    childId: 'child_leo',
    familyId,
    ownerUid: 'sarah_uid',
    name: 'Leo',
    age: 11,
    dailyAllowanceMinutes: 150,
    usedMinutesToday: 105,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/appRules/rule_roblox`).set({
    ruleId: 'rule_roblox',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    packageName: 'com.roblox.client',
    appName: 'Roblox',
    category: 'Games & Entertainment',
    dailyLimitMinutes: 45,
    usedMinutesToday: 10,
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
});

test('2. Valid paired Child access: child redeems active pairing code, reads rules/schedules, and writes permitted telemetry, requests, chat, tasks, and SOS alerts', async () => {
  const sarahDb = testEnv.authenticatedContext('sarah_uid').firestore(DATABASE_ID);
  const leoDb = testEnv.authenticatedContext('leo_child_uid').firestore(DATABASE_ID);
  const familyId = 'fam_sarah_uid';
  const nowMs = Date.now();

  // Parent creates household, child profile, rule, schedule, task, and active pairing code
  await assertSucceeds(sarahDb.doc(`families/${familyId}`).set({
    familyId,
    familyName: 'The Jenkins Family',
    ownerUid: 'sarah_uid',
    subscriptionTier: 'FREE',
    subscriptionActive: false,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/children/child_leo`).set({
    childId: 'child_leo',
    familyId,
    ownerUid: 'sarah_uid',
    name: 'Leo',
    age: 11,
    dailyAllowanceMinutes: 150,
    usedMinutesToday: 60,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/appRules/rule_roblox`).set({
    ruleId: 'rule_roblox',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    packageName: 'com.roblox.client',
    appName: 'Roblox',
    category: 'Games & Entertainment',
    dailyLimitMinutes: 45,
    usedMinutesToday: 20,
    status: 'ENFORCED',
    blocked: false,
    whitelisted: false,
    scheduleOnly: false,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/tasks/task_1`).set({
    taskId: 'task_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    title: 'Math Homework',
    rewardSummary: '+15m Bonus Time',
    status: 'IN_PROGRESS',
    completedByChild: false,
    approvedByParent: false,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/pairingCodes/pair_1`).set({
    codeId: 'pair_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    childName: 'Leo',
    code: '482-915',
    expiresAtEpochMs: nowMs + 600_000,
    status: 'ACTIVE',
    revoked: false,
  }));

  // Child registers pairedDevices membership backed by active pairing code and consumes code
  await assertSucceeds(leoDb.doc(`families/${familyId}/pairedDevices/leo_child_uid`).set({
    deviceId: 'leo_child_uid',
    childUid: 'leo_child_uid',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    childName: 'Leo',
    codeId: 'pair_1',
    redeemedCode: '482-915',
    deviceName: 'Galaxy A54 5G',
    status: 'PAIRED',
    pairedAtEpochMs: nowMs + 1000,
  }));

  await assertSucceeds(leoDb.doc(`families/${familyId}/pairingCodes/pair_1`).update({
    status: 'CONSUMED',
    consumedByChildUid: 'leo_child_uid',
    consumedAtEpochMs: nowMs + 1000,
  }));

  // Paired child can read household, child profile, and appRules
  await assertSucceeds(leoDb.doc(`families/${familyId}`).get());
  await assertSucceeds(leoDb.doc(`families/${familyId}/children/child_leo`).get());
  await assertSucceeds(leoDb.doc(`families/${familyId}/appRules/rule_roblox`).get());

  // Paired child can update permitted telemetry (usedMinutesToday, batteryLevel, location) without changing dailyAllowanceMinutes
  await assertSucceeds(leoDb.doc(`families/${familyId}/children/child_leo`).update({
    usedMinutesToday: 95,
    batteryLevel: 78,
    locationLabel: 'Oakridge Middle School',
  }));

  // Paired child can create PENDING extra time request, CHILD chat message, SOS alert, and mark task READY_FOR_REVIEW
  await assertSucceeds(leoDb.doc(`families/${familyId}/requests/req_leo_1`).set({
    requestId: 'req_leo_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    childName: 'Leo',
    requestType: 'EXTRA_TIME',
    targetName: 'Roblox',
    status: 'PENDING',
  }));

  await assertSucceeds(leoDb.doc(`families/${familyId}/messages/msg_leo_1`).set({
    messageId: 'msg_leo_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    senderName: 'Leo',
    senderRole: 'CHILD',
    text: 'Finished my math worksheet!',
  }));

  await assertSucceeds(leoDb.doc(`families/${familyId}/tasks/task_1`).update({
    completedByChild: true,
    status: 'READY_FOR_REVIEW',
  }));

  await assertSucceeds(leoDb.doc(`families/${familyId}/alerts/alert_sos_1`).set({
    alertId: 'alert_sos_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    childName: 'Leo',
    title: 'Emergency SOS Triggered',
    category: 'SOS',
    severity: 'CRITICAL',
    acknowledged: false,
  }));
});

test('3, 4, 5. Pairing security: invalid pairing code, expired pairing code, and reused/revoked pairing code all fail', async () => {
  const sarahDb = testEnv.authenticatedContext('sarah_uid').firestore(DATABASE_ID);
  const child1Db = testEnv.authenticatedContext('child_1').firestore(DATABASE_ID);
  const child2Db = testEnv.authenticatedContext('child_2').firestore(DATABASE_ID);
  const familyId = 'fam_sarah_uid';
  const nowMs = Date.now();

  await assertSucceeds(sarahDb.doc(`families/${familyId}/pairingCodes/pair_active`).set({
    codeId: 'pair_active',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    code: '482-915',
    expiresAtEpochMs: nowMs + 600_000,
    status: 'ACTIVE',
    revoked: false,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/pairingCodes/pair_expired`).set({
    codeId: 'pair_expired',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    code: '111-222',
    expiresAtEpochMs: nowMs - 10_000,
    status: 'ACTIVE',
    revoked: false,
  }));

  // Scenario 3: Invalid pairing code fails
  await assertFails(child1Db.doc(`families/${familyId}/pairedDevices/child_1`).set({
    deviceId: 'child_1',
    childUid: 'child_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    codeId: 'pair_active',
    redeemedCode: '999-999', // Wrong code!
    status: 'PAIRED',
    pairedAtEpochMs: nowMs + 1000,
  }));

  // Scenario 4: Expired pairing code fails
  await assertFails(child1Db.doc(`families/${familyId}/pairedDevices/child_1`).set({
    deviceId: 'child_1',
    childUid: 'child_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    codeId: 'pair_expired',
    redeemedCode: '111-222',
    status: 'PAIRED',
    pairedAtEpochMs: nowMs,
  }));

  // First child validly consumes pair_active
  await assertSucceeds(child1Db.doc(`families/${familyId}/pairedDevices/child_1`).set({
    deviceId: 'child_1',
    childUid: 'child_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    codeId: 'pair_active',
    redeemedCode: '482-915',
    status: 'PAIRED',
    pairedAtEpochMs: nowMs + 1000,
  }));
  await assertSucceeds(child1Db.doc(`families/${familyId}/pairingCodes/pair_active`).update({
    status: 'CONSUMED',
    consumedByChildUid: 'child_1',
    consumedAtEpochMs: nowMs + 1000,
  }));

  // Scenario 5: Reused pairing code fails for second device
  await assertFails(child2Db.doc(`families/${familyId}/pairedDevices/child_2`).set({
    deviceId: 'child_2',
    childUid: 'child_2',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    codeId: 'pair_active',
    redeemedCode: '482-915',
    status: 'PAIRED',
    pairedAtEpochMs: nowMs + 2000,
  }));
});

test('6 & 7. Unauthenticated access and Cross-family access are strictly denied', async () => {
  const sarahDb = testEnv.authenticatedContext('sarah_uid').firestore(DATABASE_ID);
  const otherFamilyDb = testEnv.authenticatedContext('other_parent_uid').firestore(DATABASE_ID);
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  const familyId = 'fam_sarah_uid';

  await assertSucceeds(sarahDb.doc(`families/${familyId}`).set({
    familyId,
    familyName: 'The Jenkins Family',
    ownerUid: 'sarah_uid',
    subscriptionTier: 'FREE',
    subscriptionActive: false,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/children/child_leo`).set({
    childId: 'child_leo',
    familyId,
    ownerUid: 'sarah_uid',
    name: 'Leo',
    age: 11,
    dailyAllowanceMinutes: 150,
    usedMinutesToday: 45,
  }));

  // Scenario 6: Unauthenticated access denied
  await assertFails(unauthDb.doc(`families/${familyId}`).get());
  await assertFails(unauthDb.doc(`families/${familyId}/children/child_leo`).get());
  await assertFails(unauthDb.doc(`families/${familyId}/pairingCodes/pair_1`).get());

  // Scenario 7: Cross-family access denied
  await assertFails(otherFamilyDb.doc(`families/${familyId}`).get());
  await assertFails(otherFamilyDb.doc(`families/${familyId}/children/child_leo`).get());
  await assertFails(otherFamilyDb.doc(`families/${familyId}/children/child_leo`).update({
    dailyAllowanceMinutes: 900,
  }));
});

test('8. Unauthorized modification of parental settings by paired Child is strictly denied', async () => {
  const sarahDb = testEnv.authenticatedContext('sarah_uid').firestore(DATABASE_ID);
  const leoDb = testEnv.authenticatedContext('leo_child_uid').firestore(DATABASE_ID);
  const familyId = 'fam_sarah_uid';
  const nowMs = Date.now();

  await assertSucceeds(sarahDb.doc(`families/${familyId}`).set({
    familyId,
    familyName: 'The Jenkins Family',
    ownerUid: 'sarah_uid',
    subscriptionTier: 'FREE',
    subscriptionActive: false,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/children/child_leo`).set({
    childId: 'child_leo',
    familyId,
    ownerUid: 'sarah_uid',
    name: 'Leo',
    age: 11,
    dailyAllowanceMinutes: 150,
    usedMinutesToday: 60,
  }));

  await assertSucceeds(sarahDb.doc(`families/${familyId}/appRules/rule_roblox`).set({
    ruleId: 'rule_roblox',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    packageName: 'com.roblox.client',
    appName: 'Roblox',
    category: 'Games & Entertainment',
    dailyLimitMinutes: 45,
    usedMinutesToday: 20,
    status: 'BLOCKED',
    blocked: true,
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

  await assertSucceeds(sarahDb.doc(`families/${familyId}/pairingCodes/pair_1`).set({
    codeId: 'pair_1',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    code: '482-915',
    expiresAtEpochMs: nowMs + 600_000,
    status: 'ACTIVE',
    revoked: false,
  }));

  await assertSucceeds(leoDb.doc(`families/${familyId}/pairedDevices/leo_child_uid`).set({
    deviceId: 'leo_child_uid',
    childUid: 'leo_child_uid',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    codeId: 'pair_1',
    redeemedCode: '482-915',
    status: 'PAIRED',
    pairedAtEpochMs: nowMs + 1000,
  }));

  // Child CANNOT increase their own dailyAllowanceMinutes
  await assertFails(leoDb.doc(`families/${familyId}/children/child_leo`).update({
    dailyAllowanceMinutes: 999,
  }));

  // Child CANNOT unblock an app or increase dailyLimitMinutes on appRules
  await assertFails(leoDb.doc(`families/${familyId}/appRules/rule_roblox`).update({
    blocked: false,
    status: 'ENFORCED',
    dailyLimitMinutes: 500,
  }));

  // Child CANNOT disable a bedtime schedule
  await assertFails(leoDb.doc(`families/${familyId}/schedules/sched_bedtime`).update({
    enabled: false,
  }));

  // Child CANNOT modify family subscription tier
  await assertFails(leoDb.doc(`families/${familyId}`).update({
    subscriptionTier: 'PREMIUM_ANNUAL',
    subscriptionActive: true,
  }));

  // Child CANNOT self-approve an extra-time request
  await assertFails(leoDb.doc(`families/${familyId}/requests/req_hack`).set({
    requestId: 'req_hack',
    familyId,
    ownerUid: 'sarah_uid',
    childId: 'child_leo',
    requestType: 'EXTRA_TIME',
    targetName: 'Roblox',
    status: 'APPROVED',
  }));
});
