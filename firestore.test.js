const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- DIRTY DOZEN & SECURITY BOUNDS TESTS ---

test("1. Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("2. Cross-user profile read (PII leak): Bob cannot read Alice's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@aden.edu.ye",
      username: "alice",
      fullName: "أليس",
      role: "student",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("3. Self-assigned admin role on create: Alice cannot create profile with admin role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@aden.edu.ye",
      username: "alice",
      fullName: "أليس",
      role: "admin",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("4. Privilege escalation on update: Alice cannot update her role to admin", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@aden.edu.ye",
      username: "alice",
      fullName: "أليس",
      role: "student",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(),
    })
  );
});

test("5. Shadow / Ghost field injection on profile update fails", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@aden.edu.ye",
      username: "alice",
      fullName: "أليس",
      role: "student",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "أليس المحدثة",
      isVerified: true,
      updatedAt: new Date(),
    })
  );
});

test("6. Email spoofing attack: unverified admin email cannot read Bob's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@aden.edu.ye",
      username: "bob",
      fullName: "بوب",
      role: "student",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_999", {
      email: "nbylalhryry62@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofedAdminDb.collection("users").doc(BOB_UID).get());
});

test("7. Cross-user exam result read/list: Bob cannot read or list Alice's exam results", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("results")
      .doc("res_1")
      .set({
        userId: ALICE_UID,
        subjectId: "biology",
        subjectName: "الأحياء",
        level: "1",
        score: 20,
        total: 25,
        percentage: 80,
        passed: true,
        timeTakenSeconds: 300,
        createdAt: new Date(),
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("results").doc("res_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("results").get()
  );
});

test("8. Identity spoofing on exam result create fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("results").doc("res_spoof").set({
      userId: BOB_UID,
      subjectId: "biology",
      subjectName: "الأحياء",
      level: "1",
      score: 20,
      total: 25,
      percentage: 80,
      passed: true,
      timeTakenSeconds: 300,
      createdAt: new Date(),
    })
  );
});

test("9. Out-of-bounds exam score / percentage fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("results").doc("res_invalid").set({
      userId: ALICE_UID,
      subjectId: "biology",
      subjectName: "الأحياء",
      level: "1",
      score: 30,
      total: 25,
      percentage: 120,
      passed: true,
      timeTakenSeconds: 300,
      createdAt: new Date(),
    })
  );
});

test("10. Exam result tampering (update) fails because exam results are immutable", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("results")
      .doc("res_1")
      .set({
        userId: ALICE_UID,
        subjectId: "biology",
        subjectName: "الأحياء",
        level: "1",
        score: 10,
        total: 25,
        percentage: 40,
        passed: false,
        timeTakenSeconds: 300,
        createdAt: new Date(),
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("results").doc("res_1").update({
      score: 25,
      percentage: 100,
      passed: true,
    })
  );
});

// --- POSITIVE & CLIENT QUERY ALIGNMENT TESTS ---

test("11. Authenticated user can create, read, and update their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@aden.edu.ye",
      username: "alice",
      fullName: "أليس",
      role: "student",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "أليس عدن",
      username: "alice_aden",
      updatedAt: new Date(),
    })
  );
});

test("12. Authenticated user can create, list, and delete their own exam results", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("results").doc("res_ok").set({
      userId: ALICE_UID,
      subjectId: "chemistry",
      subjectName: "الكيمياء",
      level: "2",
      score: 22,
      total: 25,
      percentage: 88,
      passed: true,
      timeTakenSeconds: 410,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("results").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("results").doc("res_ok").delete()
  );
});
