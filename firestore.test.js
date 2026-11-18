const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("unauthenticated user cannot read or write accounts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.doc("users/user_alice/accounts/acc1").get());
  await assertFails(unauthDb.doc("users/user_alice/accounts/acc1").set({
    userId: "user_alice",
    name: "Cash Wallet",
    type: "CASH",
    balance: 100,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("authenticated user can create their own account with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  await assertSucceeds(aliceDb.doc("users/user_alice/accounts/acc1").set({
    userId: "user_alice",
    name: "Cash Wallet",
    type: "CASH",
    balance: 100.0,
    institution: "Wallet",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("authenticated user cannot access another user's accounts", async () => {
  const bobDb = testEnv.authenticatedContext("user_bob").firestore();
  await assertFails(bobDb.doc("users/user_alice/accounts/acc1").get());
  await assertFails(bobDb.doc("users/user_alice/accounts/acc1").set({
    userId: "user_alice",
    name: "Hacked",
    type: "CASH",
    balance: 0,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("user cannot create account with invalid type or missing required fields", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  await assertFails(aliceDb.doc("users/user_alice/accounts/acc2").set({
    userId: "user_alice",
    name: "Invalid",
    type: "CRYPTO_NOT_ALLOWED",
    balance: 50,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("authenticated user can create transaction for their own account", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  await assertSucceeds(aliceDb.doc("users/user_alice/transactions/tx1").set({
    userId: "user_alice",
    type: "EXPENSE",
    amount: 25.5,
    title: "Lunch",
    accountId: "acc1",
    category: "Food",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});
