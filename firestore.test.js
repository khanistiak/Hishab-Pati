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

test("unauthenticated user cannot read or write accounts or backups", async () => {
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
  await assertFails(unauthDb.doc("users/user_alice/backups/latest").get());
  await assertFails(unauthDb.doc("users/user_alice/backups/latest").set({
    userId: "user_alice",
    docType: "manifest",
    payloadEncoding: "gzip_base64",
    backupJsonCompressed: "H4sIAAAAAAAA/6tWKkktLlGyUlAqS8wpTtVRKi1OLUpVslIyNDQyBQA1C0F1HQAAAA==",
    chunkCount: 0,
    accountsCount: 1,
    transactionsCount: 2,
    categoriesCount: 3,
    debtsCount: 0,
    fileSizeBytes: 512,
    uploadedAt: new Date(),
    uploadedAtMillis: Date.now()
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
    includeInTotal: true,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("authenticated user cannot access another user's accounts or backups", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  await assertSucceeds(aliceDb.doc("users/user_alice/backups/latest").set({
    userId: "user_alice",
    docType: "manifest",
    schemaVersion: 2,
    payloadEncoding: "gzip_base64",
    backupJsonCompressed: "H4sIAAAAAAAA/6tWKkktLlGyUlAqS8wpTtVRKi1OLUpVslIyNDQyBQA1C0F1HQAAAA==",
    chunkCount: 0,
    backupDate: "2026-11-23T10:00:00Z",
    accountsCount: 2,
    transactionsCount: 15,
    categoriesCount: 8,
    subcategoriesCount: 12,
    budgetsCount: 3,
    debtsCount: 1,
    savingsGoalsCount: 2,
    fileSizeBytes: 4096,
    compressedSizeBytes: 128,
    uploadedAt: new Date(),
    uploadedAtMillis: Date.now()
  }));

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
  await assertFails(bobDb.doc("users/user_alice/backups/latest").get());
  await assertFails(bobDb.doc("users/user_alice/backups/latest").delete());
  await assertFails(bobDb.doc("users/user_alice/backups/latest_chunk_0").get());
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

test("authenticated user can write, read, and delete inline GZIP cloud backup manifest", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  const manifestRef = aliceDb.doc("users/user_alice/backups/latest");
  await assertSucceeds(manifestRef.set({
    userId: "user_alice",
    docType: "manifest",
    schemaVersion: 2,
    payloadEncoding: "gzip_base64",
    backupJsonCompressed: "H4sIAAAAAAAA/6tWKkktLlGyUlAqS8wpTtVRKi1OLUpVslIyNDQyBQA1C0F1HQAAAA==",
    chunkCount: 0,
    backupDate: "2026-11-23T10:00:00Z",
    accountsCount: 4,
    transactionsCount: 120,
    categoriesCount: 10,
    subcategoriesCount: 18,
    budgetsCount: 5,
    debtsCount: 2,
    savingsGoalsCount: 3,
    fileSizeBytes: 48200,
    compressedSizeBytes: 6420,
    uploadedAt: new Date(),
    uploadedAtMillis: Date.now()
  }));
  await assertSucceeds(manifestRef.get());
  await assertSucceeds(manifestRef.delete());
});

test("authenticated user can write, read, and delete chunked cloud backup across sibling docs", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  const revisionId = "1763900000000_2";

  await assertSucceeds(aliceDb.doc("users/user_alice/backups/latest_chunk_0").set({
    userId: "user_alice",
    docType: "chunk",
    revisionId,
    chunkIndex: 0,
    totalChunks: 2,
    data: "CHUNK_0_BASE64_PAYLOAD",
    uploadedAt: new Date()
  }));
  await assertSucceeds(aliceDb.doc("users/user_alice/backups/latest_chunk_1").set({
    userId: "user_alice",
    docType: "chunk",
    revisionId,
    chunkIndex: 1,
    totalChunks: 2,
    data: "CHUNK_1_BASE64_PAYLOAD",
    uploadedAt: new Date()
  }));
  await assertSucceeds(aliceDb.doc("users/user_alice/backups/latest").set({
    userId: "user_alice",
    docType: "manifest",
    schemaVersion: 2,
    payloadEncoding: "gzip_base64_chunked",
    revisionId,
    chunkCount: 2,
    backupDate: "2026-11-23T10:05:00Z",
    accountsCount: 6,
    transactionsCount: 2400,
    categoriesCount: 14,
    subcategoriesCount: 32,
    budgetsCount: 12,
    debtsCount: 4,
    savingsGoalsCount: 5,
    fileSizeBytes: 1850000,
    compressedSizeBytes: 910000,
    uploadedAt: new Date(),
    uploadedAtMillis: Date.now()
  }));

  await assertSucceeds(aliceDb.doc("users/user_alice/backups/latest").get());
  await assertSucceeds(aliceDb.doc("users/user_alice/backups/latest_chunk_0").get());
  await assertSucceeds(aliceDb.doc("users/user_alice/backups/latest_chunk_1").get());
  await assertSucceeds(aliceDb.doc("users/user_alice/backups/latest_chunk_1").delete());
});

test("invalid backup manifests and chunks are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();

  // Mismatched userId
  await assertFails(aliceDb.doc("users/user_alice/backups/latest").set({
    userId: "user_bob",
    docType: "manifest",
    payloadEncoding: "gzip_base64",
    backupJsonCompressed: "abc",
    chunkCount: 0,
    accountsCount: 1,
    transactionsCount: 1,
    categoriesCount: 1,
    debtsCount: 0,
    fileSizeBytes: 100,
    uploadedAt: new Date(),
    uploadedAtMillis: Date.now()
  }));

  // Missing payload fields
  await assertFails(aliceDb.doc("users/user_alice/backups/latest").set({
    userId: "user_alice",
    docType: "manifest",
    payloadEncoding: "gzip_base64",
    accountsCount: 1,
    transactionsCount: 1,
    categoriesCount: 1,
    debtsCount: 0,
    fileSizeBytes: 100,
    uploadedAt: new Date(),
    uploadedAtMillis: Date.now()
  }));

  // Invalid chunk index >= totalChunks
  await assertFails(aliceDb.doc("users/user_alice/backups/latest_chunk_2").set({
    userId: "user_alice",
    docType: "chunk",
    revisionId: "rev1",
    chunkIndex: 2,
    totalChunks: 2,
    data: "abc",
    uploadedAt: new Date()
  }));

  // Arbitrary backup document ID is rejected
  await assertFails(aliceDb.doc("users/user_alice/backups/arbitrary_doc").set({
    userId: "user_alice",
    docType: "chunk",
    revisionId: "rev1",
    chunkIndex: 0,
    totalChunks: 1,
    data: "abc",
    uploadedAt: new Date()
  }));
});
