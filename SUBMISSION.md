# Shipaton 2026, Next Gen submission text

Paste into Devpost; the headings match the fields they ask for. Requirements and the judging
funnel are in [SHIPATON.md](SHIPATON.md).

---

## Elevator pitch (one line)

Epley Coach turns your phone into the angle gauge the vertigo cure needs, and refuses to count a
hold until your head is actually in position.

---

## What it does

BPPV is the most common cause of vertigo. It is cured by the Epley manoeuvre: five head positions,
about five minutes, free, no medication. The manoeuvre only works if the angles are right, and
that is exactly where home treatment fails: self-administered from a video, head angles are off by
**40 to 51°**. Specialist-guided, the error is 14 to 24°.

Epley Coach does the three things that matter when BPPV comes back:

1. **Checks for danger first.** A blocking safety screener runs before every run. Positional
   vertigo can be a stroke, and an app cannot see the eye movements that tell them apart, so any
   red flag stops the app and sends the user to emergency help. It cannot be skipped and is never
   paywalled.
2. **Identifies which ear.** Six questions from a 585-patient randomised trial (JAMA Neurology
   2023). Reusing your previous diagnosis resolves 42.9% of recurrences; the questionnaire resolves
   72.4%, because recurrences move to a different canal. If the answers point to the horizontal
   canal, the app refuses the Epley and says why.
3. **Guides the angles.** The phone's orientation sensor (gyroscope and accelerometer, fused, about
   50 Hz) measures head rotation and neck extension, both at once, against each position's
   tolerance. Each position is shown on a 3D figure. The hold timer will not start until the head
   is inside the band *and* still. Voice and vibration carry every instruction, because by
   position four the user is lying down and cannot watch the screen.

After the run it gives the AAO-HNS after-care advice and, if the user wants, a PDF of every run for
their doctor.

---

## Why it matters

2.4% of people get BPPV in their lifetime, up to 10% of over-70s. **8% of them receive effective
treatment.** Half recur within about three and a half years. The cure is free and takes five
minutes; the bottleneck is doing it correctly, alone, at 3 a.m., while the room is spinning.

The median patient is 51 and, while using the app, dizzy. Every design decision follows from that:
one action per screen, large high-contrast type, 48 dp touch targets, a night theme for 3 a.m.,
audio-first guidance, and calm motion only (fades and small rises; nothing spins, slides or
bounces, and "Remove animations" on the phone turns it all off).

---

## What is proven, and what is not

Stated plainly, because a health app that blurs this is not trustworthy.

**Proven, and cited:** the Epley manoeuvre is recommended by the AAO-HNS 2017 clinical practice
guideline. The six-question triage is validated in a 585-patient randomised trial. The target
angles and per-position tolerance bands come from Kwon et al. (Sci Rep 2023), derived from
specialists' own measured accuracy.

**Not proven, and not claimed:** that *this app* improves outcomes. No trial of it exists. So the
app claims angle accuracy, which was measured, and nothing about cure rates, which were not. It is
labelled as not a medical device on every relevant screen.

The accuracy claim does not rest on trusting the handset. Every guided angle is relative to a
calibration taken on the user's own head, so a fixed sensor error appears in both the reference
and the reading and subtracts out. `SensorBiasTest` pushes offsets of 3°, 9.2° and 15° about all
three axes through the real code; the guided angles move by less than 1e-6°.

---

## How RevenueCat is used

One non-consumable purchase, **the doctor's PDF**: every run with its date, ear, duration, and each
position's hold time and measured angles. It is wired through RevenueCat end to end:

- the paywall reads the **current offering** and shows the **store's own price string** (no
  hard-coded price);
- the purchase grants the **`export` entitlement**, and the app listens for customer-info updates,
  so restores and other devices just work;
- in this build the key is a **RevenueCat Test Store** key, so the paywall says on screen that
  purchases are simulated, and judges can unlock it with "Test valid purchase" at no cost.

The rule was decided before the paywall was built and is enforced in the code:

> **Nothing that treats you is ever behind the paywall.**

The safety check, the six questions, every guided run, practice mode, after-care and the history
on the phone are free forever. A paywall that lets someone proceed *unguided* is a harm, not a
growth tactic. What is paid is workflow, a document a clinician can read in a few minutes, never
capability. It is a one-time purchase, not a subscription, because a condition that resolves in
one to three sessions does not justify recurring billing. `Entitlements` is read by exactly one
thing: the export. No clinical code path depends on a purchase, a network call or a store.

---

## Technical choices

- **`:core` has no Android dependency.** Quaternion algebra, swing-twist decomposition, angle
  unwrapping, the hold gate, the triage and the after-care rules are pure Kotlin, testable on the
  JVM in seconds. **212 tests**, no emulator, no device farm; the angle maths was proven before the
  phone was ever plugged in.
- **Pixel parity with the design.** Every screen was rebuilt against the design handoff and
  verified with Robolectric + Roborazzi screenshots compared to the design's own renders (72
  states, day and night).
- **Reversal for accuracy, not bought hardware.** Two readings 180° apart separate a surface's
  tilt from the sensor's own error; the app ships this as an accuracy self-check anyone can run.
- **A single sensor cannot tell "the head turned" from "the phone slid".** It can tell them apart
  by speed, so implausibly fast rotation latches a warning and demands recalibration. And because
  the manoeuvre ends upright, the pose the calibration was taken in, the app checks itself against
  a known answer of zero and reports the drift it finds.
- **The 3D figure** is a real head model rendered with three.js: held frames are pre-rendered,
  the moving figure runs live in a WebView.
- **No Android Studio.** Everything builds from the command line on a Windows laptop. Gradle
  wrapper committed, release build signed, and a guard stops a Test Store key from ever shipping
  in a release build.

---

## Repository

https://github.com/Anuj7411/epley-coach

MIT licensed. The commit history, the test suite and `docs/RESEARCH.md` are part of the
submission: every clinical number in the app traces to a citation, including the ones that
weakened the pitch.

---

## Optional award entries

### RevenueCat Peace Prize (social good)

Vertigo from BPPV sends people to emergency rooms, causes falls in older adults, and has a cure
that takes five minutes and costs nothing, yet only 8% of sufferers ever receive it. The reason is
not access to medicine; it is access to a trained pair of hands to get the head angles right.
Epley Coach puts that guidance in a phone people already own (90% of 50 to 64 year olds), screens
for the dangerous look-alikes first, and keeps everything that treats you free forever. The only
paid feature is a PDF for your doctor.

### Design Award

Designed for someone who is dizzy right now: one action per screen, large high-contrast type, a
night theme for 3 a.m., and a voice that guides you when you cannot look at the screen. Each
position is shown on a 3D figure, the angle meters show both head angles live, and motion is
deliberately calm because, for this audience, a moving interface is a symptom.

### Notes for judges

The build uses a RevenueCat **Test Store** key, so no real payment is possible. To try the paid
feature: Home → Settings → Your runs → Share with your doctor → Unlock the PDF → **Test valid
purchase**. The `export` entitlement activates and the PDF opens in the share sheet. Everything
else in the app is free and needs no purchase. Practice mode lets you try a run with the phone in
your hand, no lying down needed.
