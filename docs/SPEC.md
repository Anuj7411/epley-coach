# Epley Coach — product specification and architecture

Shipaton 2026, Next Gen track. Android. Solo build.
Written 2026-09-08. Deadline 2026-09-30.

---

## 1. What this is

**An Android app that measures your head angle while you perform the Epley manoeuvre at home,
and refuses to let you count a hold until you are actually in position.**

The Epley manoeuvre cures BPPV — the most common cause of vertigo — by physically rolling
displaced calcium crystals out of the wrong ear canal. It is free, drug-free, takes five minutes,
and works. It only works if the head angles are right.

### The problem, quantified

| | |
|---|---|
| BPPV lifetime prevalence | 2.4% (up to 10% of over-70s) |
| Of those, receive effective treatment | **8%** |
| Head-angle error, self-administered from a video | **40–51°** |
| Head-angle error, expert-guided | 14–24° |
| Head-angle error, IMU + audio guidance (published) | **7.6°** |
| Self-treatment outcome with IMU guidance vs specialist | 61.1% vs 66.7% — **statistically equivalent** (n=88) |

The manoeuvre is not the bottleneck. Doing it at the right angle is.

### One-line pitch

> For someone diagnosed with BPPV, Epley Coach turns the phone into a head-angle instrument, so
> they can perform the manoeuvre at the angles that actually work instead of guessing from a video.

---

## 2. Scope

### In scope
- Guided **posterior-canal Epley** manoeuvre, four positions
- Real-time head-angle measurement (neck extension, head rotation)
- **Audio-first** guidance: spoken cues + haptics. Screen is secondary
- Dwell-gated hold timers — the timer will not start unless the head is in range *and* still
- Blocking red-flag safety screener before any manoeuvre
- Session history, on device
- One in-app purchase via RevenueCat Test Store

### Explicitly out of scope, and why
| Excluded | Reason |
|---|---|
| Diagnosing **which ear** is affected | Requires observing nystagmus. Phone is against the face, eyes are shut. Physically impossible with one phone |
| Diagnosing BPPV at all | The user must already have been told they have it. We guide treatment, we do not diagnose |
| Other canals (horizontal, anterior) | Different manoeuvres. Scope creep. Posterior canal is 85–95% of BPPV |
| Cloud sync, accounts, analytics on health data | No server means no breach, no consent burden, no data-protection surface |
| Any claim of medical-device status | It is not one, and we will say so in the strongest available wording |

---

## 3. Functional requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-1 | Detect at launch whether the device has a usable orientation sensor; refuse to run with a clear message if not | Must |
| FR-2 | Present a blocking red-flag screener. Any positive answer stops the session and advises urgent care. Cannot be skipped or paywalled | Must |
| FR-3 | Capture a mount calibration from an upright, still, forward-facing pose | Must |
| FR-4 | Report neck extension and head rotation continuously, at >= 25 Hz | Must |
| FR-5 | For each of the four positions, hold a target pose and tolerance; indicate direction of correction | Must |
| FR-6 | Start the hold timer only when the pose is within tolerance **and** angular rate is below the stillness threshold, sustained | Must |
| FR-7 | Speak every instruction aloud and confirm with haptics; the flow must be completable with the screen off-axis and eyes shut | Must |
| FR-8 | Abort the session on a shake gesture or any volume key | Must |
| FR-9 | Mark head rotation unreliable when the twist decomposition is ill-conditioned, rather than reporting a confident wrong number | Must |
| FR-10 | Record each session locally: timestamps, angles achieved, holds completed, aborts | Should |
| FR-11 | Export session history as CSV | Should — **this is the paid feature** |
| FR-12 | Offer left/right side selection, explicitly labelled as the user's own report, not a diagnosis | Should |
| FR-13 | Replay a completed session's angle trace | Could |
| FR-14 | Support three mounts — cheek hold, headband, and in-hand practice — each with its own stated tolerance. No mount requires equipment the user does not already own | Must |
| FR-15 | Refuse a calibration taken with the phone too near flat, with a message saying what to do instead, rather than building the session on an undetermined forward axis | Must |
| FR-16 | Latch a warning when the phone rotates faster than a neck can, and require recalibration before the reading is trusted again | Must |
| FR-17 | Offer a return-to-upright drift check that reports the measured mount error, since upright is a pose whose true reading is known to be zero | Must |
| FR-18 | In practice mode, state on screen that the reading describes the phone and not the user's head | Must |

## 4. Non-functional requirements

| ID | Requirement | Rationale |
|---|---|---|
| NFR-1 | Angle accuracy within 5° of a physical inclinometer, static | The error being corrected is 40–51°; 5° is an order of magnitude inside it |
| NFR-2 | No sample-to-sample discontinuity above 30° | 30° at 50 Hz is 1,500°/s, far beyond human head motion. **Verified: worst observed 5.16°** |
| NFR-3 | Yaw drift under 10° over a 90-second session | The manoeuvre runs ~60 s from calibration. **Measured: −4.2° over 264 s** |
| NFR-4 | Screen stays on for the whole session | Non-wakeup sensors stop delivering when the CPU suspends |
| NFR-5 | Zero network access on the measurement path | Nothing to breach, nothing to consent to |
| NFR-6 | No health data touches the purchase path | Ethical floor for monetising a health tool |
| NFR-7 | Runs on API 24+ | Covers essentially every device in use |
| NFR-8 | Core measurement logic has no Android dependency | So it is unit-testable on the JVM without a device |

---

## 5. System architecture

```
                          ┌──────────────────────────────────┐
                          │  :app  (Android, Compose)        │
                          │                                  │
  Sensor HAL              │  ┌────────────────────────────┐  │
  ┌──────────────┐        │  │ HeadTracker                │  │
  │ gyroscope    │──┐     │  │  registers sensor listener │  │
  │ accelerometer│──┼────►│  │  quaternion per frame      │  │
  └──────────────┘  │     │  │  angular rate              │  │
                    │     │  │  unwrap + calibration      │  │
   Android sensor   │     │  └────────────┬───────────────┘  │
   fusion produces  │     │               │ StateFlow        │
   GAME_ROTATION_   │     │               ▼                  │
   VECTOR ──────────┘     │  ┌────────────────────────────┐  │
                          │  │ ManeuverEngine             │  │
                          │  │  current step, tolerance   │  │
                          │  │  dwell gate, hold timer    │  │
                          │  └───┬──────────────┬─────────┘  │
                          │      │              │            │
                          │      ▼              ▼            │
                          │  ┌────────┐   ┌───────────────┐  │
                          │  │ Voice  │   │ Compose UI    │  │
                          │  │ +haptic│   │ (secondary)   │  │
                          │  └────────┘   └───────────────┘  │
                          │      │                           │
                          │      ▼                           │
                          │  ┌────────────────────────────┐  │
                          │  │ SessionStore (local file)  │  │
                          │  └────────────────────────────┘  │
                          │      │                           │
                          │      ▼ entitlement check         │
                          │  ┌────────────────────────────┐  │
                          │  │ RevenueCat (Test Store)    │  │
                          │  └────────────────────────────┘  │
                          └──────────────────────────────────┘
                                        │
                          ┌─────────────▼────────────────────┐
                          │  :core  (pure Kotlin, no Android)│
                          │                                  │
                          │  Quaternion    swing-twist       │
                          │  HeadAngles    mount transform   │
                          │  AngleUnwrapper                  │
                          │  ManeuverDefinition (targets)    │
                          │  DwellDetector                   │
                          │                                  │
                          │  ← 27 JVM unit tests, no device  │
                          └──────────────────────────────────┘
```

### Why the module split

`:core` has **no Android dependency at all**. Every piece of maths that could be silently wrong —
the quaternion algebra, the singularity handling, the unwrapping, the dwell logic — is testable on
the JVM in milliseconds, with no emulator and no phone. `:app` is the thin shell that talks to the
sensor HAL and the screen.

This is the reason 27 tests run in 8 seconds instead of needing a device farm, and it is what let
the angle maths be proven correct before the phone was ever plugged in.

### Data flow, one frame

1. Sensor HAL delivers `GAME_ROTATION_VECTOR` at ~50 Hz with a hardware timestamp
2. `HeadTracker` builds a unit quaternion, computes angular rate from the previous sample
3. `HeadAngles` applies the mount calibration and returns neck extension, head rotation, swing
4. `AngleUnwrapper` makes head rotation continuous across the ±180° boundary
5. `ManeuverEngine` compares against the current step's target; gates the hold timer on
   *in tolerance* **and** *below stillness threshold*, sustained
6. Voice/haptics announce state changes; UI renders the same state
7. `SessionStore` appends to the local record

**All time values derive from sensor frame timestamps, never wall clock.** Android frame delivery
jitters, and we are timing a 30-second hold.

---

## 6. Security, privacy and safety architecture

### Data
| Question | Answer |
|---|---|
| What is collected? | Head angles, timestamps, hold outcomes. Nothing identifying |
| Where does it live? | App-private storage on the device |
| Does it leave the device? | **Never.** No network permission on the measurement path |
| Analytics on health values? | None |
| Accounts? | None |
| Health data near the purchase path? | Never. RevenueCat sees an entitlement check, nothing else |

The manifest requests **no INTERNET permission for the measurement flow**. That is not a policy
promise, it is an architectural guarantee: the code cannot exfiltrate what it cannot reach.

India's DPDP Act 2023 imposes verifiable parental consent for children's data. We hold no personal
data and are not aimed at children, so the obligation does not arise — by design, not by luck.

### Safety
| Control | Implementation |
|---|---|
| Red-flag screener | Blocking, unskippable, before any manoeuvre. Sudden onset with headache, double vision, weakness, slurred speech, gait ataxia, continuous rather than positional vertigo, new hearing loss, recent head/neck trauma → hard stop, seek urgent care |
| Never paywalled | Safety screener, abort gesture, and every angle reading are free forever. A paywall that lets someone proceed *unguided* is an active harm |
| No diagnosis | The app never says whether you have BPPV or which ear. It guides a manoeuvre you were already told to do |
| Abort always available | Shake or any volume key, works eyes-closed |
| Cervical contraindications | Screener asks about neck surgery, rheumatoid arthritis, known cervical spine disease |
| Regulatory posture | Prominent **NOT A MEDICAL DEVICE — unregulated prototype, must never be used on a patient** in README and in-app. Not "this is not a medical device", which regulators have explicitly rejected as a defence |

### Monetisation ethics
**No number is ever behind a paywall. Nothing safety-relevant is ever behind a paywall.**
The paid tier gates session history export and multi-profile support — workflow, never capability.
A subscription is inappropriate for a condition that resolves in 1–3 sessions; one-time unlock only.

---

## 7. Technology and resources

### Toolchain (installed and verified)
| Component | Version | Verified how |
|---|---|---|
| Gradle | 9.7.1 | `gradle --version` |
| Android Gradle Plugin | 9.4.0 | Queried Google Maven; AGP 9.4 needs Gradle 9.6+ |
| Kotlin | 2.4.20 | Queried Maven Central |
| Compose BOM | 2026.08.00 | Queried Google Maven; requires compileSdk 37 |
| JDK | Temurin 21.0.12.1 | AGP needs 17+; system JDK 24 is too new |
| compileSdk / targetSdk / minSdk | 37 / 36 / 24 | |
| adb / platform-tools | 37.0.1 | |
| RevenueCat SDK | `com.revenuecat.purchases:purchases:10.15.1` | Test Store needs 9.9.0+ |

Deliberately **no Android Studio and no emulator** — 1.4 GB instead of 10–15 GB, which matters at
99% disk. Everything builds from the command line.

### Hardware
| Item | Status |
|---|---|
| Motorola Edge 40 Neo, Android 15, InvenSense gyro + Game Rotation Vector | Available, tested |
| Digital angle finder, ±0.2°, ~₹600–1,500 | **Not yet purchased. Blocks NFR-1** |
| Headband or sleep mask, ~₹50 | Not yet purchased. Needed for a rigid mount |
| A second person to film | Needed for the demo video |

### Third-party dependencies
Compose, AndroidX Lifecycle, Kotlin coroutines, RevenueCat. Nothing exotic, no ML model, no
training data, no camera pipeline. **The shortest dependency chain of any candidate considered.**

---

## 8. Validation plan

Two layers, deliberately separated.

### Layer 1 — engineering, provable alone
| Test | Method | Status |
|---|---|---|
| Quaternion algebra | 27 JVM unit tests | **Passing** |
| Singularity continuity | Sweep through and past vertical; assert no step >45° | **Passing — 0 of 35,751 samples** |
| Unwrapping | Replay the real +166.4° → −169.7° step from hardware | **Passing** |
| Sample rate stability | Frame-timestamp histogram | **Passing — 50.0 Hz** |
| Drift | Return-to-reference after 5 min of motion | **Measured: −4.2° yaw, +1.7° tilt** |
| Static accuracy | Phone on a digital angle finder at 0/20/30/45/60/90/110/135° | **Blocked on hardware purchase** |
| Mount repeatability | 10 calibrations × 5 people; spread of the transform | Not started |
| Usability | 5 healthy volunteers, screen off, count protocol deviations | Not started |

### Layer 2 — clinical, NOT provable here
Efficacy on actual BPPV patients. **We will not claim it.** The clinical premise is cited from
Kwon 2026 and Callejas Pastor 2023, both of which used head-fixed IMUs rather than a hand-held
phone — and that transfer assumption is stated in the README as untested.

**100% of what needs proving about the engineering is disease-independent. 100% of what needs
proving about efficacy is not, and is out of reach.** Saying so plainly is the honest position.

---

## 9. Known weaknesses, stated up front

1. **A hand-held phone is not a rigid mount.** Every validated system in the literature used a
   head-fixed IMU. We cannot require one: most people trying this at 3am own no headband, and a
   reviewer opening the app for three minutes will not strap a phone to their head.

   The answer is not to demand equipment but to make the mount an explicit choice with an honest
   price. Three are supported — a hand pressing the phone flat against the cheekbone (±7°, needs
   nothing, and the cheek is bone-backed so it couples to the skull better than it sounds), a
   headband or cap (±5°), and an in-hand practice mode where the phone stands in for the head
   (±10°, labelled on screen as measuring the phone rather than the user).

   Two mechanisms keep this honest rather than hopeful. A single orientation sensor cannot tell
   "the head turned" from "the phone slid" — both are the same rotation — but it can tell them
   apart by speed, so a rotation past 250°/s is flagged as the mount moving and latches until
   recalibration. And because the Epley returns the user to sitting upright each cycle, which is
   the pose the calibration was taken in, the app can check itself against a known answer of zero
   and report the drift it actually finds. The residual is measured, not assumed.
2. **We cannot determine which ear is affected.** The user tells us which side felt worse. A 2023
   JAMA Neurology trial showed symptom-based lateralisation still produces 72.4% vs 42.9%
   resolution, so it is good enough to help — and it is labelled as a guess.
3. **DizzyFIX built essentially this, won an RCT, then delisted the app** and sold a plastic
   device instead. No public explanation exists. We cannot claim to know something they did not.
4. **The app category has almost no users.** Highest rating count across the entire iOS vertigo
   category is 67. This is a real market signal and we will not make market-size claims.
5. **Yaw drifts.** Gravity anchors tilt but not rotation-about-gravity. Measured, reported, and
   mitigated by recalibrating close to the manoeuvre.

---

## 10. Build schedule

| Dates | Deliverable | Blocking |
|---|---|---|
| Sept 8 | This spec; manoeuvre state machine in `:core` with tests | — |
| Sept 9–10 | Session flow, screens, wiring the engine to the tracker | — |
| Sept 11–12 | Audio + haptic guidance; eyes-closed completion | Phone |
| Sept 13 | Red-flag screener | — |
| Sept 14–15 | RevenueCat Test Store paywall + entitlement gating | — |
| Sept 16–17 | Static accuracy validation; numbers into README | **Angle finder** |
| Sept 18–20 | Usability run, 5 volunteers | Headband |
| Sept 21–23 | Polish, session history, export | — |
| Sept 24–27 | Demo video | Second person |
| Sept 28–29 | README, submission text, repo cleanup | — |
| Sept 30 | Submit, with buffer | — |
