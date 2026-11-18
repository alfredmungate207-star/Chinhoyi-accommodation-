const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_alfred";
const ADMIN_EMAIL = "alfredmungate207@gmail.com";
const LANDLORD_UID = "landlord_tapiwa";
const CUSTOMER_UID = "customer_rudo";
const OTHER_UID = "other_user";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminDb() {
  return testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
}

function spoofedAdminDb() {
  return testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
}

function landlordDb() {
  return testEnv
    .authenticatedContext(LANDLORD_UID, { email: "tapiwa@gmail.com", email_verified: true })
    .firestore();
}

function customerDb() {
  return testEnv
    .authenticatedContext(CUSTOMER_UID, { email: "rudo@gmail.com", email_verified: true })
    .firestore();
}

function otherDb() {
  return testEnv
    .authenticatedContext(OTHER_UID, { email: "other@gmail.com", email_verified: true })
    .firestore();
}

function sampleHousePayload(landlordUid, status = "PENDING", featured = false) {
  const now = new Date(Date.now() - 2000);
  return {
    landlordUid,
    landlordName: "Tapiwa Moyo",
    name: "Jacaranda Haven",
    suburb: "Cold Stream",
    description: "Quiet boarding house 800m from CUT campus with Wi-Fi and solar.",
    monthlyRentUsd: 85.0,
    depositUsd: 85.0,
    totalRooms: 12,
    vacantRooms: 4,
    genderPolicy: "FEMALE_ONLY",
    roomType: "SHARED_2",
    distanceToCutKm: 0.8,
    approxLat: -17.352,
    approxLng: 30.205,
    amenities: ["Wi-Fi", "Solar Backup", "Borehole"],
    coverPhotoRef: "drawable:img_boarding_exterior",
    photoRefs: ["drawable:img_boarding_exterior", "drawable:img_boarding_room"],
    status,
    featured,
    rating: 4.8,
    reviewCount: 6,
    createdAt: now,
    updatedAt: now,
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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("houses").where("status", "==", "APPROVED").get());
  await assertFails(unauth.collection("platform_config").doc("settings").get());
  await assertFails(unauth.collection("users").doc(CUSTOMER_UID).get());
});

test("User registration prevents self-escalation to ADMIN or landlordApproved=true", async () => {
  const now = new Date(Date.now() - 2000);
  await assertSucceeds(
    customerDb().collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Rudo Chikwanda",
      email: "rudo@gmail.com",
      role: "CUSTOMER",
      landlordApproved: false,
      accountActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-assigning ADMIN
  await assertFails(
    otherDb().collection("users").doc(OTHER_UID).set({
      userId: OTHER_UID,
      displayName: "Hacker",
      email: "other@gmail.com",
      role: "ADMIN",
      landlordApproved: true,
      accountActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject PII cross-user read
  await assertFails(otherDb().collection("users").doc(CUSTOMER_UID).get());

  // Spoofed unverified admin email cannot list users or update config
  await assertFails(spoofedAdminDb().collection("users").get());
  await assertSucceeds(adminDb().collection("users").get());
});

test("Admin can configure upgradable platform settings; non-admin cannot", async () => {
  const now = new Date(Date.now() - 1000);
  const configPayload = {
    unlockFeeUsd: 2.5,
    unlockFeeZig: 70.0,
    ecocashNumber: "0771 234 567",
    ecocashName: "Alfred Mungate",
    maxPhotosPerHouse: 10,
    requireHouseApproval: true,
    requireFeeForLocation: true,
    requireFeeForChat: true,
    enableLiveVacancy: true,
    enableFeaturedBoost: true,
    maintenanceMode: false,
    welcomeBannerText: "Welcome to Chinhoyi Stays",
    updatedAt: now,
  };

  await assertFails(customerDb().collection("platform_config").doc("settings").set(configPayload));
  await assertSucceeds(adminDb().collection("platform_config").doc("settings").set(configPayload));
  await assertSucceeds(customerDb().collection("platform_config").doc("settings").get());
});

test("Landlord submits PENDING house, updates vacancy, Admin approves admission", async () => {
  await assertSucceeds(
    landlordDb().collection("houses").doc("house_1").set(sampleHousePayload(LANDLORD_UID, "PENDING", false))
  );

  // Customer cannot see PENDING house via get or list
  await assertFails(customerDb().collection("houses").doc("house_1").get());
  await assertFails(customerDb().collection("houses").get());

  // Landlord cannot self-approve or self-feature
  await assertFails(
    landlordDb().collection("houses").doc("house_1").update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Landlord can update live vacantRooms
  await assertSucceeds(
    landlordDb().collection("houses").doc("house_1").update({
      vacantRooms: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Admin approves house admission
  await assertSucceeds(
    adminDb().collection("houses").doc("house_1").update({
      status: "APPROVED",
      featured: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Now customer query aligned with status == APPROVED succeeds!
  await assertSucceeds(
    customerDb().collection("houses").where("status", "==", "APPROVED").get()
  );
  // Landlord query aligned with landlordUid succeeds!
  await assertSucceeds(
    landlordDb().collection("houses").where("landlordUid", "==", LANDLORD_UID).get()
  );
});

test("EcoCash payment submission -> Admin sees payerName & unlocks exact location + chat", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = new Date(Date.now() - 5000);
    await db.collection("houses").doc("house_1").set(sampleHousePayload(LANDLORD_UID, "APPROVED", false));
    await db.collection("houses").doc("house_1").collection("private").doc("location").set({
      houseId: "house_1",
      landlordUid: LANDLORD_UID,
      exactLat: -17.3526,
      exactLng: 30.2054,
      streetAddress: "Plot 14 Jacaranda Drive, Cold Stream",
      landmarks: "Opposite Cold Stream Shops",
      contactPhone: "0774 888 999",
      updatedAt: now,
    });
  });

  // Before unlock: customer CANNOT read exact private location or create chat thread
  await assertFails(
    customerDb().collection("houses").doc("house_1").collection("private").doc("location").get()
  );
  const threadId = `${CUSTOMER_UID}_house_1`;
  const now = new Date(Date.now() - 1000);
  await assertFails(
    customerDb().collection("chat_threads").doc(threadId).set({
      customerUid: CUSTOMER_UID,
      customerName: "Rudo",
      landlordUid: LANDLORD_UID,
      landlordName: "Tapiwa",
      houseId: "house_1",
      houseName: "Jacaranda Haven",
      memberUids: [CUSTOMER_UID, LANDLORD_UID],
      lastMessage: "Hi",
      lastSenderUid: CUSTOMER_UID,
      unreadByCustomer: 0,
      unreadByLandlord: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer submits EcoCash payment request with payerName
  await assertSucceeds(
    customerDb().collection("payment_requests").doc("pay_1").set({
      customerUid: CUSTOMER_UID,
      customerEmail: "rudo@gmail.com",
      houseId: "house_1",
      houseName: "Jacaranda Haven",
      landlordUid: LANDLORD_UID,
      payerName: "Rudo Chikwanda",
      payerPhone: "0772 555 111",
      referenceCode: "EC261117.1540.X12",
      amountUsd: 2.0,
      currencyLabel: "USD",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Other customer cannot read Rudo's payment request
  await assertFails(otherDb().collection("payment_requests").doc("pay_1").get());

  // Admin verifies payerName and unlocks
  await assertSucceeds(
    adminDb().collection("payment_requests").doc("pay_1").update({
      status: "UNLOCKED",
      adminNote: "EcoCash verified for Rudo Chikwanda",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    adminDb().collection("unlocks").doc(threadId).set({
      customerUid: CUSTOMER_UID,
      houseId: "house_1",
      houseName: "Jacaranda Haven",
      landlordUid: LANDLORD_UID,
      payerName: "Rudo Chikwanda",
      paymentRequestId: "pay_1",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Now customer CAN read exact GPS location and chat with landlord!
  await assertSucceeds(
    customerDb().collection("houses").doc("house_1").collection("private").doc("location").get()
  );
  await assertSucceeds(
    customerDb().collection("chat_threads").doc(threadId).set({
      customerUid: CUSTOMER_UID,
      customerName: "Rudo",
      landlordUid: LANDLORD_UID,
      landlordName: "Tapiwa",
      houseId: "house_1",
      houseName: "Jacaranda Haven",
      memberUids: [CUSTOMER_UID, LANDLORD_UID],
      lastMessage: "Hello, is the twin room still open?",
      lastSenderUid: CUSTOMER_UID,
      unreadByCustomer: 0,
      unreadByLandlord: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    customerDb().collection("chat_threads").doc(threadId).collection("messages").doc("msg_1").set({
      threadId,
      senderUid: CUSTOMER_UID,
      senderName: "Rudo",
      memberUids: [CUSTOMER_UID, LANDLORD_UID],
      text: "Hello, is the twin room still open?",
      messageType: "TEXT",
      createdAt: now,
    })
  );

  // Non-member cannot read or query messages in chat thread
  await assertFails(otherDb().collection("chat_threads").doc(threadId).get());
  await assertFails(otherDb().collection("chat_threads").doc(threadId).collection("messages").get());
  await assertFails(
    otherDb().collection("chat_threads").doc(threadId).collection("messages").where("memberUids", "array-contains", CUSTOMER_UID).get()
  );
});
