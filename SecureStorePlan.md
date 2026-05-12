# Secure Store Plan

## Goal

Store only malicious notification history in Convex without leaking sensitive notification content. Convex should act as a durable malicious-event ledger, while full notification details stay on devices that received the malicious-event payload through FCM.

The phone should not wait for the analysis result. The analysis pipeline is asynchronous, so the app should submit notifications and return immediately.

## Core Design

Sensitive notification fields must not be stored in Convex:

- Notification title
- Notification body
- Full URL list
- Raw message content

Convex should store only metadata needed for history, synchronization, and local validation:

- `eventId`
- `circleId`
- `userId`
- `timestamp`
- `action`
- `contentHash`
- Optional `packageName`

The plaintext notification data is stored locally on each device only if the notification is classified as malicious and delivered through FCM.

## Async Flow

The source phone captures a notification and generates a stable event ID immediately:

```text
Source phone captures notification
  -> Generate eventId = UUID.randomUUID()
  -> Compute contentHash from canonical payload
  -> Store optional local pending record
  -> Send eventId + plaintext payload to backend/analyzer
  -> Backend returns immediately
```

The phone should not keep a socket open waiting for a verdict.

If the notification is not malicious:

```text
Analysis pipeline does nothing
No Convex event is created
No FCM is sent to circle devices
Local pending record expires silently
```

If the notification is malicious:

```text
Backend sends FCM to all devices in the circle
FCM includes eventId + plaintext payload + contentHash + source userId
Each device stores the plaintext locally by eventId
Convex stores only malicious metadata
```

This means the app does not need a real "benign" callback. In this design, benign effectively means no malicious callback arrived before the local pending TTL expired.

## Event ID

Generate `eventId` once, on the source phone, when the notification is first captured.

`eventId` is not a security feature. It is a coordination key:

```text
Which local event should this device fetch?
```

The same `eventId` must be forwarded by the backend and FCM to every circle device. Receiving devices must not generate a new ID for the same malicious event.

## Source Identity

Use the existing Convex/Clerk authenticated user ID as the source identity.

```text
sourceUserId = Clerk/Convex authenticated user id
```

Each receiving device can compare the FCM/Convex source user against its current user:

```text
if sourceUserId == myUserId:
    this malicious event came from my account
else:
    this malicious event came from another circle member
```

This is enough for v1 if the app model is:

```text
one account = one protected person
```

Do not add `sourceDeviceId` in v1 unless distinguishing multiple devices signed into the same account becomes important later.

## Content Hash

Use `contentHash` to validate that the local plaintext payload matches the Convex metadata event.

For v1:

```text
contentHash = SHA-256(canonicalNotificationPayload)
```

The canonical payload should be stable across devices and include:

- `eventId`
- `title`
- `body`
- `packageName`
- Sorted `urls`
- Original notification timestamp
- Source user ID

Do not include unstable fields:

- FCM receive time
- Local database insert time
- Device-specific values

The hash answers:

```text
Is the local payload I found really the same event Convex references?
```

Plain SHA-256 is acceptable for v1 validation, but it is not a strong privacy boundary. If someone can guess the notification content, they can calculate the same hash.

## Future HMAC Upgrade

A stronger future version should use HMAC instead of plain SHA-256:

```text
contentHash = HMAC-SHA256(circleSecret, canonicalNotificationPayload)
```

HMAC needs a shared `circleSecret` that exists only on trusted circle devices. Convex would store the HMAC result, but Convex would not know the secret.

Current recommendation:

```text
v1: SHA-256 for validation
future: HMAC-SHA256 when secure circle key sharing exists
```

## Local Storage

Each device should store malicious notification details locally using `eventId`.

Suggested local fields:

- `eventId`
- `sourceUserId`
- `title`
- `body`
- `packageName`
- `urls`
- `notificationTimestamp`
- `contentHash`
- `receivedAt`

The source phone may also keep a short-lived pending record while waiting for a possible malicious FCM callback.

Suggested pending behavior:

```text
PENDING -> MALICIOUS, if matching FCM callback arrives
PENDING -> EXPIRED, if no malicious callback arrives within TTL
```

Do not treat expiration as a confirmed benign verdict. It only means no malicious result arrived in time.

## Convex Event Shape

Replace the current plaintext `moreDetails` usage with metadata only.

Suggested event table fields:

```ts
event: {
  userId: string,
  circleId: string,
  timestamp: number,
  action: string,
  eventId: string,
  contentHash: string,
  packageName?: string,
}
```

Use `userId` as the source user ID from Convex auth. Use `action` for the event type:

```text
malicious_notification
```

Do not add fields like `risk` or `verdict` unless the real analysis pipeline starts returning them and the product needs to show them.

Add a circle history index:

```ts
.index("byCircleAndDate", ["circleId", "timestamp"])
```

The history query should load malicious events by `circleId`, not only by the current `userId`, so circle members can see shared malicious history.

## History Behavior

The history screen queries Convex for malicious events by circle.

Rows can show non-sensitive metadata:

- Time
- Source member/user
- Action
- Optional app/package name

When opening an event:

```text
Load Convex event by eventId
Load local details by eventId
Recompute contentHash from local details
Compare with Convex contentHash
Display details only if hashes match
```

If local data is missing:

```text
Show: Details unavailable on this device
```

If hash validation fails:

```text
Do not display plaintext
Show unavailable or integrity error state
```

## FCM Payload

For malicious notifications, FCM should include enough data for every circle device to store the event locally:

```json
{
  "eventId": "...",
  "sourceUserId": "...",
  "timestamp": 123456789,
  "contentHash": "...",
  "title": "...",
  "body": "...",
  "packageName": "com.example.app",
  "urls": []
}
```

The receiving app should:

```text
1. Validate required fields exist.
2. Store plaintext locally by eventId.
3. Compare sourceUserId with current userId.
4. Mark the event as from self or from another circle member.
5. Use Convex metadata for durable history.
```

## Important Limitations

FCM is useful for live distribution, but it is not durable storage.

Expected limitations:

- Offline devices may miss plaintext details.
- New devices joining later will see Convex metadata but not old details.
- Reinstalled devices may lose local detail history.
- A source phone may submit a notification and then never receive the malicious callback if FCM delivery fails.

This is acceptable for the privacy-first version because Convex intentionally does not store plaintext.

## Test Scenarios

- Benign or unclassified notification creates no Convex event.
- Malicious notification creates one Convex metadata event.
- FCM payload is stored locally on each receiving device.
- Source phone receives its own malicious callback and recognizes it by matching `sourceUserId`.
- Other circle devices recognize the event as coming from another member.
- Opening a history event with matching local data displays details.
- Opening a history event without local data shows a redacted unavailable state.
- Tampered local data fails hash validation.
- Non-circle members cannot query another circle's malicious history.
- Convex contains no notification title, body, or full URL list.
