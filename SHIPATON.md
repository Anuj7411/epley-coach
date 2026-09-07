# Shipaton 2026 — research notes and open decisions

Status: candidates parked, awaiting decision. Last updated 2026-09-06.

## Hard facts (from organiser sources, not inferred)

- Submission deadline: **Sept 30, 2026, 11:45pm PT**. Judging Oct 1-13. Winners Oct 21.
- Runs Aug 1 - Sept 30, 2026. App must be **first ever public release** inside that window.
- Platforms: iOS, iPadOS, macOS, Android. Stores: App Store, Google Play, Samsung Galaxy.
- **Web-only apps do not qualify.** This is a mobile hackathon.
- Every submission must use the RevenueCat SDK for at least one in-app/web purchase, or serve ads via RevenueCat Ads.
- 556 submissions already logged as of Sept 6.

## The two paths

There are effectively two ways in.

**Path A: the main competition.** All the big categories (Grand Prize, Design, Peace Prize,
HAMM, Best Game, the sponsor awards). Requires the app to be **live and downloadable** in a
real store by the deadline. Apps still under review do not count. TestFlight and closed testing
do not count.

**Path B: the Next Gen Award.** Students only. Organiser's words: **"no App Store or Google
Play release is required."**

### DECISION 2026-09-06: Path A is CLOSED on all three stores. Next Gen only.

User checked play.google.com/console and it prompted for the $25 registration fee. That means
**no existing Play Console developer account.** Registering now creates a post-Nov-13-2023
personal account, so the 12-testers exemption does not apply.

**Google Play arithmetic (best case, everything perfect):**

| Date | Step |
|---|---|
| Sept 6 | Register Play account, pay $25 |
| Sept 7 | App good enough for closed testing, 12 real testers opted in |
| Sept 7 - 21 | 14 continuous days. Google verifies real usage. One dropout resets |
| Sept 21 | Apply for production access. Google: "Review usually takes seven days or less, but can occasionally take longer" |
| Sept 28 | Production access granted, if perfect |
| Sept 28 - Oct 5 | App's own production review, 7-14 days for a first-ever submission |

Earliest realistic live date **Oct 5**. Deadline **Sept 30**. No branch where this works.
Production access is also not automatic: Google evaluates "how you recruited your testers, how
engaged they were, what feedback you received, what you changed as a result."

**Samsung Galaxy Store:** genuinely free, no sign-up or annual fee. But distributing any app
requires **commercial seller status**: "several days" to approve, plus "up to 10 business days"
to verify a D-U-N-S number and "up to 10 business days" for bank verification. Nominal app
review 2-5 business days, but developer forums carry threads titled "Under Device Test status
for a month" and "still in review for past 2 months." Coin flip on queues we cannot influence,
for a store with a fraction of the reach. Not a bet worth 24 days.

**Apple:** $99/year and no Mac. Closed, and not free.

| Store | Status | Why |
|---|---|---|
| Apple App Store | Closed | $99/year, Windows dev machine |
| Google Play | Closed | Earliest live Oct 5 vs Sept 30 deadline |
| Samsung Galaxy Store | Effectively closed | Seller verification 10-20 business days, review times documented in months |

**Why Next Gen is not a downgrade:**
- First place is $20,000, same as Design Award, Peace Prize and HAMM first place.
- Student-only field, not the 556+ general field.
- Nothing external can block the submission. No fees, no D-U-N-S, no testers, no review queue.
- The repo is a judging surface, true in no other category.
- All 24 remaining days go into app, repo and video instead of store bureaucracy.

### ARCHIVED: cost ledger from when Path A looked open

Kept for reference only. Superseded by the decision above.

Cost ledger for Path A on Android:

| Item | Cost |
|---|---|
| Google Play developer account | $25, already paid |
| Android Studio + Gradle, local builds on Windows | $0, unlimited |
| Emulator or own phone | $0 |
| RevenueCat | $0 below $2,500 monthly tracked revenue, no card required |
| Privacy policy hosting (required by Play) | $0 on GitHub Pages |
| Store listing assets | $0, time only |
| **Total additional spend** | **$0** |

iOS stays closed. EAS builds iOS in the cloud without a Mac, but distribution still needs the
$99/year Apple Developer Program. Not free.

### New Path A gates (these are now the real risk)

1. **Play review time depends on shipping history, not account age.** First-ever Google Play
   submissions in 2026 typically clear in **7 to 14 days**. Accounts with shipping history clear
   in **1 to 3 days**.
   - Never published before: submit to Play by **~Sept 16**
   - Published before: submit by **~Sept 26**
2. **Developer identity verification.** Google now requires government-ID verification on
   accounts behind published apps. Old accounts are getting verification tasks pushed to them.
   A pending verification blocks publishing and takes days. **Check Play Console immediately.**
3. **Target API 36 mandatory.** Since Aug 31 2026, new apps must target Android 16 (API 36).
   RN 0.81 defaults to it; Expo SDK 54 ships RN 0.81; Expo 53 can be forced with
   `expo-build-properties`. Stack is constrained to **Expo SDK 54+ or bare RN 0.81+**.

Plus standard listing work: 512x512 icon, 1024x500 feature graphic, 2-8 screenshots, public
privacy policy URL, content rating questionnaire, data safety form. Roughly half a day.

### Multi-category strategy (UNCONFIRMED, must ask organisers)

Rules permit multi-category in general: "A Project entered in an Influencer Award category may
also be submitted to and considered for any other non-Influencer prize category for which it is
eligible." Judging blog: screeners "score apps 1-5 in all the categories the app is targeting."

For Next Gen the wording is an **exemption**, not a prohibition: "Except for Projects submitted
for the Next Gen Award, include a URL to a fully published app." Reads as Next Gen not *needing*
a store link, not being *barred* from one. **Not stated explicitly. Ask on Shipaton Discord.**

If yes, the dominant play:
1. Build one app. Open-source the repo from commit one.
2. Publish a working v1 to Google Play by ~Sept 15. Grand Prize is scored on "early effective
   release" and growth numbers, so thin-and-early beats polished-and-late.
3. Submit targeting Next Gen AND the store categories that fit (Design, Peace Prize, HAMM).
4. If Play review stalls or rejects, Next Gen is untouched. That is the floor.

Honest counterweight: Path A means competing against 556+ entries instead of a student-only
field, and Grand Prize needs real install and revenue numbers that are near-impossible from zero
in three weeks. Path A realistically buys shots at Design, Peace Prize, HAMM and sponsor
categories, not the $100k.

Parked: OneSignal "Keep Them Coming Back" pays **$25,000** for 1st, more than Next Gen, and
needs only a clean OneSignal integration with one deployed campaign. Cheap bolt-on. Not chasing
yet.

Next Gen requires:
- A demo video showing the app working
- A link to a public open-source repo
- An OSS license file in the repo
- A clear description of what was built, what it does, why it matters

Prizes: $20,000 / $10,000 / $5,000.

Judging criteria: app idea clarity and usefulness, meaningful progress toward a working app,
thoughtful RevenueCat use, technical choices and care.

Note: the **repo is a judging surface here**, unlike every other category. Monetisation design
is scored on its own. This is the only track where code quality earns points.

Caveat: if under 18, Next Gen is the ONLY enterable category. Rules say minor entrants "may
compete only for the Next Gen Award."

## Gates Path B avoids

| Gate | Path A | Path B (Next Gen) |
|---|---|---|
| App live in store (under review does not count, TestFlight does not count) | Yes | No |
| Apple Developer account ($99) | Yes | No |
| Google Play 12 testers / 14 continuous days (personal accounts post Nov 13 2023; Google now verifies real usage; any dropout resets) | Yes | No |
| Apple review 2-5 days for new apps, September is peak | Yes | No |

## Next Gen eligibility: VERIFIED 2026-09-06

User's academic email domain is `oriental.ac.in`.

Checked against the JetBrains/swot database that the rules name by reference. Rules wording:
"Email-domain eligibility may be verified using JetBrains/swot."

```
lib/domains/in/ac/oriental.txt   HTTP 200
contents:                        Oriental College of Technology, Bhopal
control (fake college)           HTTP 404
stoplist                         not listed
```

**Domain qualifies.** Devpost account must carry the `@oriental.ac.in` address, not a personal
Gmail, because that is the field checked.

### Full Next Gen criteria

Eligibility:
- Active student in "high school, college, university, bootcamp, or another academic program"
- Aged 13+
- "use a qualifying student or academic email address on Devpost"
- Under age of majority: documented parent/guardian consent, and may enter ONLY Next Gen

Submission artifacts:
- Demo video showing the app working, **under 2 minutes**, public YouTube or Vimeo link
- Link to a public open-source code repository
- OSS license file in that repo
- Clear description of what was built, what it does, why it matters
- 1024x1024 app icon
- Screenshot 1179x2556, no device frames
- RevenueCat SDK powering at least one in-app/web purchase, or RevenueCat Ads
- First-ever release inside Aug 1 - Sept 30, 2026
- No store listing, no paid developer account

Scoring criteria:
1. App idea clarity and usefulness
2. Meaningful progress toward a working app  (note: does NOT say finished)
3. Thoughtful RevenueCat use
4. Technical choices and care

Shape implications:
- Video is under 2 min and screeners watch the first 2 min. The video IS the pitch, no slack.
- The repo is scored directly, true in no other category. Commit history, README, tests and
  honest measurement all count.

### Open action items
- [ ] Register on Devpost with `@oriental.ac.in`, then register for Shipaton 2026
- [ ] Create free RevenueCat account, enable Test Store, get `test_` API key
- [ ] Confirm whether user is 18+ (decides whether parental consent doc is needed)
- [ ] Grep all 556 submissions for overlap with the chosen candidate

## Mobile vs web: SETTLED 2026-09-06

**Must be a mobile app. Android. Not web.**

Rules, under "What to Create": **"Apps must be built for iOS, iPadOS, macOS, or Android."**
This applies to every submission including Next Gen.

Next Gen waives only distribution: **"Instead of a published app-store listing, submit a demo
video and a link to your public, open-source code repository."**

Platform requirement stands. Publishing requirement disappears.

Practical shape:

| | |
|---|---|
| Build on | Windows PC, Android Studio |
| Run on | User's own Android phone, or emulator |
| Record video from | Same phone or emulator |
| Publish to | Nowhere |
| Submit | Video link + public GitHub repo + description |

**Constraint dropped:** mandatory target API 36 is a *Google Play* requirement. We are not
publishing to Play, so it does not bind us. Stack versions are a free choice now. Still prefer
current Expo for tooling quality.

## Build constraints

- **Dev machine is Windows.** No Xcode, no iOS Simulator. Rules out native Swift, and the iOS
  half of Flutter and Kotlin Multiplatform. **Android only.**
- **Expo Go cannot run RevenueCat for real.** `react-native-purchases` is native code. Expo Go
  silently falls back to "Preview API Mode" with JS mocks. Fine for paywall UI, useless for
  demonstrating a purchase. Needs an Android dev build (EAS or local Gradle). Budget a day.
- **RevenueCat Test Store clears the SDK requirement with zero accounts and zero cost.**
  Auto-provisioned per project, `test_` prefixed API key. Test purchases update CustomerInfo,
  trigger entitlements, appear in the dashboard. Documented limit: does not exercise real store
  billing. Irrelevant for Next Gen.

## How judging actually works

Five-stage funnel. Two RevenueCat screeners score 1-5. Their stated minimum: watch the first
2 minutes of video, read the description, review screenshots. Downloading the app is
"encouraged but not mandatory."

Organiser's stated failure modes: incomplete submission info, unanswered category questions,
unclear first 2 minutes of video.

Their words on the video: "What we expect to see in the first two minutes ... What your app is
about, e.g. the elevator pitch; Your app in use; How and why your app is targeting the
different prize categories."

**The video is the judging surface. The repo is the second one.**

## Field saturation (71 of 556 sampled, 13%, extrapolated)

| Lane | In sample | Est. total |
|---|---|---|
| Games | 10 | ~78 |
| Habit / focus / productivity trackers | 9 | ~70 |
| Health, fitness, nutrition | 7 | ~55 |
| Photo & camera utilities | 6 | ~47 |
| Care / family coordination | 5 | ~39 |
| AI study & learning | 5 | ~39 |
| Notes / clipboard / link savers | 3 | ~23 |

Dead lanes: habit tracker, focus timer, clipboard saver, wardrobe app, calorie counter,
AI study buddy, subscription manager.

## The pattern worth exploiting

2025 winners included four sensor-and-measurement apps across four different categories:
Heartbeat Hero (ARKit, CPR compression depth), PitchLab (camera, baseball pitch analysis),
Posturely (camera and sensors, posture), Hearing Buddy (on-device speech captioning).

In the 71-project 2026 sample that lane is essentially empty. Nearly everything is an LLM
wrapper over a text box.

This maps directly onto Next Gen's "technical choices and care" criterion, and it enables the
tactic that worked last time: build a ground-truth harness, publish real accuracy numbers,
and name the failure class we cannot solve.

## Candidates (PARKED — decision pending)

### 1. Oral reading fluency coach  [recommended]

Problem: measuring whether someone can read aloud is done by a human with a stopwatch and a
printed passage, marking errors by hand. That is why it happens twice a year, not twice a week.
The metric (words correct per minute, plus miscue count) is standardised and objective.

Tractable because you know the target text. Constrained forced alignment, not open ASR.
Runs offline on Android via Vosk.

Prior art: strong academic literature (arXiv 2306.03444, arXiv 2406.07060, 783-recording
validation corpus). Method is proven and gives us published error rates to benchmark against.
Consumer side is thin: Amira Learning sells to US school districts at district prices. No free
Android app for a parent, an adult literacy tutor, or an ESL learner practising alone.

Honest weakness: ASR on children's speech and strong non-native accents is genuinely worse, and
the literature says so. We measure exactly how much worse and publish it next to the good number.

RevenueCat fit: free for one reader, subscription for progress history and multiple readers.
Natural, not bolted on.

### 2. Sideline reaction-time and balance baseline for amateur contact sport

Problem: concussion return-to-play in school and club sport is decided by eyeball. Pro teams
have baseline testing. Nobody else does.

Technical substance: honest reaction-time measurement on Android requires calibrating out touch
latency and display latency, which vary per device. Most attempts ignore this and are wrong by
40-80ms. Doing it right is verifiable against a reference.

Honest weakness: cannot claim to detect concussion, only deviation from personal baseline.
Needs real athletes to produce meaningful data.

### 3. Physiotherapy home-exercise verification

Problem: home exercise adherence runs 30-50%. Every physio app is a video library with a
checkbox. None verify the movement happened, at the right range of motion, at the right tempo.

Honest weakness: pose estimation is slow on mid-range Android, we cannot validate prescriptions
without a physio, and it sits adjacent to the crowded fitness lane.

## Killed candidates (with evidence)

- **Noise dosimeter.** NIOSH ships an official free sound level meter app validated to +/-2 dBA,
  plus multiple commercial NIOSH/OSHA dosimeter apps exist. Solved, by the government.
- **Gait asymmetry.** OneStep is a commercial product doing exactly this. Validation literature
  says asymmetry parameters show "poor validity and reliability except step length asymmetry."
  Lane occupied and the measurement is unreliable.

## Known unknowns

- Saturation table is extrapolated from a 13% sample, not counted.
- Have not yet grepped all 556 submissions for overlap with the three candidates. Do this
  before writing any code.
- User is in India / South Asia. No store geo-restriction applies to Next Gen, so this is a
  free edge if we find a problem visible from there and invisible to a US/Europe-heavy field.
- User reports no other particular edge (no access to test subjects, no specific domain).

## Sources

- https://www.shipaton.com/
- https://www.shipaton.com/faq
- https://www.shipaton.com/blog/how-we-judge-shipaton
- https://www.shipaton.com/categories/next-gen-award
- https://revenuecat-shipaton-2026.devpost.com/rules
- https://revenuecat-shipaton-2026.devpost.com/project-gallery
- https://revenuecat.github.io/codelabs/shipaton-2026-prep.html
- https://www.revenuecat.com/blog/company/revenuecat-test-store
- https://www.revenuecat.com/blog/company/shipaton-2025-winners
