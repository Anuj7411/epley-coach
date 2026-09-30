# Submission checklist — Shipaton 2026, Next Gen

Pulled from the Devpost API on **2026-09-30 10:44 UTC**, not from notes. Requirements are the
host's own field definitions; nothing here is inferred.

## The clock

| | |
|---|---|
| Submissions close | **2026-10-01 06:45 UTC** (11:45pm Sep 30, Pacific) |
| In your time | **12:15pm IST, Wed 1 October** |
| Remaining at time of writing | **≈20 hours** |
| Project on Devpost | id `1440694`, name **"Untitled"**, state `submission_pre_draft` |

The project exists but is a pre-draft — nothing in it is filled in yet.

---

## A. Required by Devpost — submission fails without these

| # | Field | Devpost id | Status | Owner |
|---|---|---|---|---|
| A1 | **Demo video**, ≤2 min, public on YouTube or Vimeo | `video_required` | ✗ **not made** | both |
| A2 | Text description of features and functionality | project body | ✓ drafted in `SUBMISSION.md` | done |
| A3 | 1024×1024 app icon attached | `27378` checkbox | ✓ `docs/app-icon-1024.png` | ready to upload |
| A4 | ≥1 screenshot at 1179×2556, **no device frame** | `27379` checkbox | ✓ 3 in `docs/screenshots/` | ready to upload |
| A5 | App type — select **Android** | `27382` dropdown | ✗ | you, in the form |
| A6 | **RevenueCat project ID** | `28118` text | ✗ **you must fetch this** | **you** |
| A7 | Project name (currently "Untitled") | project | ✗ → "Epley Coach" | you, in the form |

**A6 is the one nobody can do for you.** RevenueCat dashboard → Project Settings → copy the
project ID. It is a required field and the form will not submit without it.

**Store URL is *not* required for you.** Fields `27383` / `27384` / `28117` are for Apple, Google
and Samsung listings. Next Gen replaces that with the repository link, so leave all three blank
and do **not** tick `27380` ("first version released on a store between Aug 1 and Sep 30") —
it is optional and untrue for your path.

---

## B. Next Gen specific — these are what qualify you for the student award

| # | Field | Devpost id | Value | Status |
|---|---|---|---|---|
| B1 | URL to your code repository | `27793` | `https://github.com/Anuj7411/epley-coach` | ✓ public, MIT |
| B2 | Student/academic email | `27792` | `0105cb231013@oriental.ac.in` | ✓ |
| B3 | Minor entrant consent | `28375` | tick it — you are 18+, so no minor is involved | ✗ tick in form |

---

## C. The video — the only large piece left

Devpost's exact words: *"Should show the app running on the device it was built for"*, *"no longer
than 2 minutes of essential footage"*, *"uploaded to and publicly visible on YouTube or Vimeo"*,
*"must not include third-party trademarks or copyrighted music"*.

That last clause matters: **no licensed music.** The `motion` skill generates its own procedural
score, which sidesteps it entirely.

Script is written and timed: `docs/VIDEO-SCRIPT.md`, 1:55.

### C1. Footage

| Shot | Status | Owner |
|---|---|---|
| Home screen, slow scroll | ✓ recorded | done |
| Splash / cold start | ✓ recorded | done |
| Safety screener → stop screen | ✗ | me, over adb |
| Triage questions | ✗ | me, over adb |
| Paywall → Test Store purchase → export | ✗ (needs entitlement cleared first) | me, over adb |
| **Phone at cheek, live angle, timer refusing then locking** | ✗ | **you — needs a head and a bed** |

The last row is the centrepiece and the only thing that satisfies *"the app running on the
device"* in a way animation cannot fake.

### C2. Production — `motion` skill stages

| Stage | State |
|---|---|
| 0 Preflight | ✓ node, ffmpeg, ffprobe present; 12 GB free |
| — HyperFrames skills | ✗ `npx skills add heygen-com/hyperframes`, then restart the session |
| — Kokoro TTS (only if you want a generated voice) | ✗ `pip install kokoro-onnx soundfile` |
| 1 Intake → `brief.md`, `ui-tokens.json` | ✗ |
| 2 Structure | ✗ |
| 3 Hook | ✗ (script already has one; needs mapping to H01–H15) |
| 4 **Storyboard + lint** — your approval gate | ✗ |
| 5 Look | ✗ (`app-native`, using the app's own tokens) |
| 6 Build | ✗ |
| 7 Audio — procedural bed + SFX, no licensed music | ✗ |
| 8 Review — stills, contact sheet, quality gates | ✗ |
| 9 Finish — BT.709 grade, encode, `check_video` | ✗ |
| — Upload to YouTube, set **public**, paste link | ✗ **you** |

### C3. Voice-over — unresolved, and it blocks the audio stage

Three options, pick one:
1. **You record it.** Script is at the end of `docs/VIDEO-SCRIPT.md`, 229 words.
2. **Kokoro TTS**, local and free — needs the two installs above.
3. **No voice-over.** The skill's default. On-screen type and sound carry it. Fastest, and given
   the clock, the safest.

---

## D. Optional award entries — free text, cheap, worth doing

Entering costs nothing and each is scored separately.

| Award | Devpost id | Fit | Draft? |
|---|---|---|---|
| **RevenueCat Peace Prize** — *"greatest social good… big benefits to communities or society"* | `27389` | **Very strong.** A free cure that 8% of sufferers receive. | ✗ worth writing |
| **Design Award** — *"innovative ideas and/or beautiful app design and animations"* | `27391` | **Strong.** Accessibility-first dark system, Atkinson Hyperlegible, no-animation rule derived from the users' own condition. | ✗ worth writing |
| HAMM — *"smartest use of RevenueCat to drive real revenue"* | `27388` | Weak — Test Store, no real revenue. | skip |
| Notes for judges | `27392` | Worth using: explain the Test Store unlock so they can try premium features. | ✗ |

`27392` matters more than it looks. The rules ask for *"a free trial or a promo code so judges can
unlock the in-app purchase"*. You have neither — but with the Test Store, any judge who sideloads
the APK taps **TEST VALID PURCHASE** and it unlocks instantly, free. Say that in the notes or a
judge may mark you down for a missing promo code.

---

## E. Already done — no action

- Repository public, MIT licence detected by GitHub
- App icon, adaptive + themed + splash, verified on device
- Three screenshots at exactly 1179×2556
- Release build signed and installable
- RevenueCat integration verified end to end — paywall, Test Store purchase, entitlement, export
- 187 tests passing
- Description drafted, video script written
- Registered on Devpost with the academic email

---

## F. Honest risk note

The video is the only large task and it has a hard external dependency: **you filming the
manoeuvre shot**, and then a YouTube upload that must be public before you paste the link.

With ≈20 hours, the full nine-stage premium pipeline is achievable but tight, and it has no slack
if a render fails. If time runs short, the order that protects the submission is:

1. Fill every Devpost field except the video link — so the form is one paste from complete
2. Film the manoeuvre shot
3. Cut the simplest honest video that satisfies the rules
4. Only then make it premium

A submitted good video beats an unsubmitted perfect one. The deadline does not move.
