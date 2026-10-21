# API reference

Base path `/api`. JSON in, JSON out. Every request except login and register needs
`Authorization: Bearer <token>`.

Roles are written as **S** (student), **T** (teacher), **A** (admin). "Owner" means the user who
created the row.

- [Envelopes](#envelopes)
- [Auth](#auth)
- [Venues](#venues)
- [Bookings](#bookings)
- [Equipment](#equipment)
- [Notifications](#notifications)
- [Reports](#reports)
- [Classes and clubs](#classes-and-clubs)
- [Admin](#admin)
- [Enumerations](#enumerations)

## Envelopes

Success:

```json
{ "success": true, "data": { }, "message": "ok", "timestamp": "2026-10-21T08:30:00Z" }
```

Paged success — `data` becomes:

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0, "last": true }
```

Failure:

```json
{ "success": false, "error": { "code": "CONFLICT", "message": "That slot is already booked", "details": [] } }
```

| Code | HTTP | Meaning |
| --- | --- | --- |
| `BAD_REQUEST` | 400 | a rule was broken |
| `VALIDATION_ERROR` | 400 | a field is missing or malformed, `details` lists each one |
| `UNAUTHORIZED` | 401 | missing or expired token |
| `FORBIDDEN` | 403 | wrong role, or not the owner |
| `NOT_FOUND` | 404 | no such row |
| `CONFLICT` | 409 | the slot is already taken |
| `INTERNAL_ERROR` | 500 | unplanned; the message stays generic |

---

## Auth

### `POST /api/auth/register` — public

```json
{ "username": "kelly", "password": "secret123", "realName": "Kelly Wu",
  "role": "STUDENT", "gender": "FEMALE", "age": 16,
  "phone": "0400000000", "studentNo": "2026-0412", "classId": 1 }
```

`200` → `{ token, user }`. Only the fields above are read; `role` defaults to `STUDENT`.

### `POST /api/auth/login` — public

```json
{ "username": "student", "password": "student123" }
```

`200`:

```json
{ "success": true,
  "data": { "token": "eyJhbGciOi...", "user": { "id": 3, "username": "student", "role": "STUDENT" } } }
```

Wrong password or unknown user → `401 UNAUTHORIZED`, same message for both.

### `GET /api/auth/me` — any role

Returns the `UserDto` of the caller. Useful after a page reload.

### `POST /api/auth/logout` — any role

`200` with `data: null`. Tokens are stateless, so this exists for the client to clear its store; the
token stays valid until it expires.

---

## Venues

### `GET /api/venues` — any role

| Query | Type | Notes |
| --- | --- | --- |
| `status` | `OPEN` \| `MAINTENANCE` \| `CLOSED` | optional filter |
| `keyword` | string | matches name or building, case-insensitive |
| `page`, `size` | int | default `0`, `20` |

Returns a page of `VenueDto`.

### `GET /api/venues/{id}` — any role

`200` → `VenueDto`, `404` if missing.

```json
{ "id": 1, "name": "Badminton Court 2", "type": "BADMINTON", "indoor": true,
  "building": "Sports Hall", "location": "Court 2", "capacity": 4,
  "openTime": "08:00:00", "closeTime": "21:00:00", "status": "OPEN",
  "rules": "Non-marking shoes only", "imageUrls": null }
```

### `GET /api/venues/{id}/availability` — any role

| Query | Type | Notes |
| --- | --- | --- |
| `from` | `YYYY-MM-DD` | defaults to today |
| `to` | `YYYY-MM-DD` | defaults to `from + 7 days` |

`200` → list of slots, generated from the venue's weekly windows:

```json
[ { "date": "2026-10-22", "startTime": "08:00:00", "endTime": "09:00:00",
    "available": true, "reason": null },
  { "date": "2026-10-22", "startTime": "09:00:00", "endTime": "10:00:00",
    "available": false, "reason": "Already booked" } ]
```

`reason` is `Already booked`, `Venue under maintenance` or `Venue closed`.

### `POST /api/venues` — A

```json
{ "name": "Court 3", "type": "BASKETBALL", "indoor": true, "building": "Sports Hall",
  "location": "Court 3", "capacity": 10, "openTime": "08:00", "closeTime": "21:00",
  "status": "OPEN", "rules": "Indoor shoes only",
  "schedules": [ { "weekday": 1, "startTime": "08:00", "endTime": "21:00" },
                 { "weekday": 6, "startTime": "09:00", "endTime": "12:00" } ] }
```

`200` → the created `VenueDto`. Weekday is `1` (Monday) to `7` (Sunday).

### `PUT /api/venues/{id}` — A

Same body. Schedules are replaced wholesale when `schedules` is present.

### `DELETE /api/venues/{id}` — A

Sets `status = CLOSED`; it does not delete the row, so booking history stays readable. Reopening is
a `PUT` with `"status": "OPEN"`.

---

## Bookings

### `GET /api/bookings` — any role

| Query | Type | Notes |
| --- | --- | --- |
| `venueId` | long | optional |
| `status` | `PENDING` \| `APPROVED` \| `REJECTED` \| `CANCELLED` \| `COMPLETED` \| `NO_SHOW` | optional |
| `from`, `to` | `YYYY-MM-DD` | booking date range |
| `mine` | boolean | ignored for students, who always get only their own |
| `page`, `size` | int | default `0`, `20` |

`200` → page of `BookingDto`.

### `POST /api/bookings` — any role

```json
{ "venueId": 1, "bookingDate": "2026-10-22",
  "startTime": "10:00", "endTime": "11:00",
  "purpose": "FREE_PLAY", "attendeesCount": 2,
  "classId": null, "clubId": null }
```

`200` → `BookingDto` with `status` either `APPROVED` (no approval needed) or `PENDING`.

| Failure | Status |
| --- | --- |
| venue not found | `404` |
| venue not `OPEN` | `400` |
| `endTime <= startTime` | `400` |
| outside opening hours | `400` |
| overlapping active booking | `409` |
| `attendeesCount` above venue capacity | `400` |

Approval is required when `classId` or `clubId` is set, when `attendeesCount >= 10`, or when
`purpose` is `PE_CLASS`, `CLUB_TRAINING`, `EVENT` or `EXAM`.

### `GET /api/bookings/{id}` — owner, T, A

`200` → `BookingDto`. A student asking for someone else's booking gets `403`.

### `PATCH /api/bookings/{id}` — owner, while `PENDING`

Same body as create. Anything already decided cannot be edited; cancel and rebook instead.

### `POST /api/bookings/{id}/cancel` — owner

`200` → `BookingDto` with `status = CANCELLED`. Allowed from `PENDING` or `APPROVED`. Equipment
borrowed against the booking is returned and the stock counter goes back up.

### `POST /api/bookings/{id}/approve` — T, A

`PENDING` → `APPROVED`. `400` if the booking is not pending.

### `POST /api/bookings/{id}/reject` — T, A

```json
{ "reason": "Pitch is needed for the inter-school match" }
```

`PENDING` → `REJECTED`. The reason is optional but is shown to the student, so it is worth writing.

### `POST /api/bookings/{id}/check-in` — T, A

`APPROVED` → `COMPLETED`, and `checkInAt` is stamped. This is the only way a booking becomes
`COMPLETED`; the sweep never does.

### `POST /api/bookings/{id}/no-show` — T, A

`APPROVED` → `NO_SHOW`. The scheduler does the same thing every minute for bookings whose end time
has passed, so this endpoint is for doing it early.

### `BookingDto`

```json
{ "id": 12, "userId": 3, "venueId": 1, "venueName": "Badminton Court 2",
  "classId": null, "clubId": null,
  "bookingDate": "2026-10-22", "startTime": "10:00:00", "endTime": "11:00:00",
  "purpose": "FREE_PLAY", "attendeesCount": 2, "status": "APPROVED",
  "rejectReason": null, "approvedBy": 3, "approvedAt": "2026-10-21T09:12:00Z",
  "checkInAt": null, "cancelledAt": null, "createdAt": "2026-10-21T09:12:00Z",
  "canCancel": true, "canApprove": false }
```

`canCancel` and `canApprove` are computed per caller. Render buttons from them rather than
re-deriving the state machine in the client.

---

## Equipment

### `GET /api/equipment` — any role

`200` → list of `{ id, name, venueId, total, available }`.

### `POST /api/equipment/borrow` — any role

```json
{ "bookingId": 12, "equipmentId": 1, "qty": 2 }
```

`200` → the updated `EquipmentDto`. `400` when `qty` exceeds `available`.

### `POST /api/equipment/{id}/return` — any role

`200` → the updated `EquipmentDto`. Returning twice is harmless: the borrow row is already marked
returned and the counter is capped at `total`.

---

## Notifications

### `GET /api/notifications` — any role

`200` → the caller's notifications, newest first:
`{ id, type, title, content, read, relatedBookingId, createdAt }`.

`type` is one of `BOOKING_CREATED`, `APPROVAL_REQUIRED`, `APPROVED`, `REJECTED`, `CANCELLED`,
`NO_SHOW`.

### `POST /api/notifications/{id}/read` — owner

`200` with `data: null`.

### `POST /api/notifications/read-all` — any role

`200` with `data: null`.

---

## Reports

Both endpoints accept `from` and `to` (`YYYY-MM-DD`; defaults are the last 30 days) and an optional
`venueId`. Role: **T** or **A**.

### `GET /api/reports/utilization`

```json
[ { "venueId": 1, "venueName": "Badminton Court 2", "type": "BADMINTON",
    "total": 20, "completed": 15, "cancelled": 3, "noShow": 2, "utilizationRate": 75.0 } ]
```

`utilizationRate` is `completed / total * 100`, rounded to one decimal.

### `GET /api/reports/bookings-summary`

```json
{ "total": 42,
  "byStatus": { "APPROVED": 20, "PENDING": 6, "COMPLETED": 12, "CANCELLED": 4 },
  "topVenues": { "Football Pitch": 14, "Badminton Court 2": 9 } }
```

---

## Classes and clubs

### `GET /api/classes` — any role

`200` → `[{ "id": 1, "name": "Grade 10A", "meta": "Grade 10" }]`.

### `GET /api/clubs` — any role

`200` → `[{ "id": 2, "name": "Badminton Club", "meta": "Sport" }]`.

Both feed the optional class/club pickers in the booking form; selecting either forces approval.

---

## Admin

### `GET /api/admin/users` — A

| Query | Type | Notes |
| --- | --- | --- |
| `role` | `STUDENT` \| `TEACHER` \| `ADMIN` | optional |
| `page`, `size` | int | default `0`, `20` |

`200` → page of `UserDto` (no password field, ever).

### `PATCH /api/admin/users/{id}` — A

| Query | Type | Notes |
| --- | --- | --- |
| `status` | `ACTIVE` \| `DISABLED` | required |

`200` → the updated `UserDto`. A disabled account cannot log in: `POST /api/auth/login` answers
`403 Forbidden` with `Account is disabled`. Enable it again with `status=ACTIVE`.

---

## Enumerations

| Enum | Values |
| --- | --- |
| `Role` | `STUDENT`, `TEACHER`, `ADMIN` |
| `VenueType` | `VOLLEYBALL`, `BADMINTON`, `FOOTBALL`, `TENNIS_INDOOR`, `TENNIS_OUTDOOR`, `SQUASH`, `BASKETBALL`, `TABLE_TENNIS`, `OTHER` |
| `VenueStatus` | `OPEN`, `MAINTENANCE`, `CLOSED` |
| `BookingPurpose` | `FREE_PLAY`, `PE_CLASS`, `CLUB_TRAINING`, `EVENT`, `EXAM`, `OTHER` |
| `BookingStatus` | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED`, `NO_SHOW` |
| `NotificationType` | `BOOKING_CREATED`, `APPROVAL_REQUIRED`, `APPROVED`, `REJECTED`, `CANCELLED`, `NO_SHOW` |
| `Status` (user) | `ACTIVE`, `DISABLED` |
| `Gender` | `MALE`, `FEMALE`, `OTHER` |
