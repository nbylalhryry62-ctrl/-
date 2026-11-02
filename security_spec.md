# Security Specification — Aden Medical Test

## 1. Data Invariants
1. **Authentication Mandatory**: No unauthenticated user may read or write any document in `/users/{userId}`, `/users/{userId}/results/{resultId}`, or `/admins/{adminId}`.
2. **User Isolation & Ownership**:
   - A user document at `/users/{userId}` can only be read, created, or updated by `request.auth.uid == userId` (or an authenticated admin).
   - An exam result at `/users/{userId}/results/{resultId}` can only be read, listed, created, or deleted by `request.auth.uid == userId` (or an authenticated admin).
3. **Privilege Escalation Prevention**:
   - Regular users creating `/users/{userId}` must set `role == 'student'` (unless `isAdmin()`).
   - Regular users updating `/users/{userId}` can only modify `['username', 'fullName', 'updatedAt']`; they cannot modify `role`, `userId`, `email`, or `createdAt`.
4. **Immutable Exam Results**:
   - Once an exam result is created at `/users/{userId}/results/{resultId}`, it cannot be updated (`allow update: if false;`), preserving exam integrity.
5. **Temporal Integrity**:
   - All `createdAt` and `updatedAt` fields must be valid Firestore `timestamp` objects with `<= request.time`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Profile Read**: `unauthDb.collection("users").doc("alice").get()` -> Reject
2. **Cross-User Profile Read (PII Leak)**: Bob reads `/users/alice` -> Reject
3. **Self-Assigned Admin Role on Create**: Alice creates `/users/alice` with `role: "admin"` -> Reject
4. **Privilege Escalation on Update**: Alice updates `/users/alice` with `role: "admin"` -> Reject
5. **Shadow / Ghost Field Injection on Profile**: Alice updates `/users/alice` adding `isVerified: true` -> Reject
6. **Immutable Field Mutation**: Alice updates `/users/alice` changing `createdAt` or `userId` -> Reject
7. **Email Spoofing Attack**: User with `email: "nbylalhryry62@gmail.com"` but `email_verified: false` attempts admin read on `/users/bob` -> Reject
8. **Cross-User Exam Result Read/List**: Bob lists `/users/alice/results` -> Reject
9. **Identity Spoofing on Exam Result Create**: Alice creates `/users/alice/results/r1` with `userId: "bob"` -> Reject
10. **Out-of-Bounds Exam Score / Percentage**: Alice creates `/users/alice/results/r1` with `percentage: 150` or `score: -5` or `total: 0` -> Reject
11. **Exam Result Tampering (Update)**: Alice updates `/users/alice/results/r1` to change `score` from 10 to 25 -> Reject
12. **Resource / String Size Poisoning**: Alice creates `/users/alice` with a 5000-character `fullName` -> Reject
