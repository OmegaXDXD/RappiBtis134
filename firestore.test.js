const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_cbtis134";
const BOB_UID = "bob_cbtis134";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function safePastDate() {
  return new Date(Date.now() - 2000);
}

function validProfile(uid, ts = safePastDate()) {
  return {
    userId: uid,
    numeroControl: "23320051340123",
    fullName: "Ana Sofia Martinez",
    specialty: "Programación",
    semester: 4,
    shift: "Matutino",
    email: "ana@cbtis134.edu.mx",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validOrder(uid, ts = safePastDate()) {
  return {
    userId: uid,
    folio: "CBT134-1024",
    numeroControl: "23320051340123",
    studentName: "Ana Sofia Martinez",
    itemsSummary: "1x Chilaquiles Verdes, 1x Horchata",
    items: [
      { itemId: "chilaquiles", name: "Chilaquiles Verdes", quantity: 1, unitPriceMxn: 45 },
      { itemId: "horchata", name: "Agua de Horchata", quantity: 1, unitPriceMxn: 18 },
    ],
    totalMxn: 63,
    pickupWindow: "Receso Matutino (10:00)",
    paymentMethod: "Efectivo",
    notes: "Sin cebolla",
    qrPayload: "CBT134|FOLIO:CBT134-1024|NC:23320051340123|TOTAL:63.00",
    status: "recibido",
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated user cannot read or write student profile or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
});

test("Authenticated student can create and update own profile with valid Numero de Control", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(ref.set(validProfile(ALICE_UID)));
  await assertSucceeds(
    ref.update({
      semester: 5,
      updatedAt: safePastDate(),
    })
  );
});

test("Authenticated student cannot spoof another userId or add ghost fields to profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Cross-user write rejected
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set(validProfile(BOB_UID))
  );

  // Ghost field rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set({ ...validProfile(ALICE_UID), isAdmin: true })
  );
});

test("Authenticated student cannot read or write another student's orders", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb
      .collection("users")
      .doc(BOB_UID)
      .collection("orders")
      .doc("order_bob_1")
      .set(validOrder(BOB_UID))
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("orders").doc("order_bob_1").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("orders").get()
  );
});

test("Order lifecycle enforces valid transitions and locks terminal states", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("orders")
    .doc("order_1");

  // Cannot create directly as 'listo'
  await assertFails(
    orderRef.set({ ...validOrder(ALICE_UID), status: "listo" })
  );

  // Valid create as 'recibido'
  await assertSucceeds(orderRef.set(validOrder(ALICE_UID)));

  // Cannot skip directly from 'recibido' to 'entregado'
  await assertFails(
    orderRef.update({ status: "entregado", updatedAt: safePastDate() })
  );

  // Valid step-by-step progression: recibido -> preparando -> listo -> entregado
  await assertSucceeds(
    orderRef.update({ status: "preparando", updatedAt: safePastDate() })
  );
  await assertSucceeds(
    orderRef.update({ status: "listo", updatedAt: safePastDate() })
  );
  await assertSucceeds(
    orderRef.update({ status: "entregado", updatedAt: safePastDate() })
  );

  // Terminal state locked: cannot modify after 'entregado'
  await assertFails(
    orderRef.update({ status: "cancelado", updatedAt: safePastDate() })
  );

  // Cannot delete order
  await assertFails(orderRef.delete());
});
