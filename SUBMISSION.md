# Shipaton 2026 — Next Gen submission text

Draft. Paste into Devpost; the headings match the fields they ask for. Requirements and the
judging funnel are in [SHIPATON.md](SHIPATON.md).

---

## Elevator pitch (one line)

Epley Coach measures your head angle while you treat your own vertigo, and refuses to count a
hold until you are actually in position.

---

## What it does

BPPV is the most common cause of vertigo. It is cured by the Epley manoeuvre — five head
positions, five minutes, free, drug-free, no medication. The manoeuvre only works if the angles
are right, and that is exactly where home treatment fails: self-administered from a video, head
angles are off by **40–51°**. Specialist-guided, the error is 14–24°.

Epley Coach does the two things trials show actually matter when BPPV comes back:

1. **Identifies which ear.** Six questions from a 585-patient RCT (JAMA Neurology 2023). Reusing
   your previous diagnosis resolves 42.9% of recurrences; the questionnaire resolves 72.4%,
   because recurrences move to a different canal. If the answers point to the horizontal canal,
   the app refuses the Epley and says so — that is a different manoeuvre.
2. **Guides the angles.** The phone's own orientation sensor measures neck extension and head
   rotation at 50 Hz. The hold timer will not start unless the head is both inside the target
   band *and* still. Voice and vibration carry every instruction, because by position four the
   user is face-down with their eyes shut and cannot see the screen.

A blocking red-flag screener runs before anything else — positional vertigo can be a stroke, and
an app cannot see the eye movements that tell them apart. It is unskippable and never paywalled.

---

## Why it matters

2.4% of people get BPPV in their lifetime, up to 10% of over-70s. **8% of them receive effective
treatment.** Half recur within about three and a half years. The cure is free and takes five
minutes; the bottleneck is doing it correctly, alone, at 3am, while the room is spinning.

The median patient is 51 years old. Every design decision follows from that and from the fact
that they are dizzy while using it: 48dp+ touch targets, nothing smaller than 16sp, audio-first
guidance, a colour-blind-safe palette, and **no animation anywhere** — for this audience
specifically, a moving interface is a symptom.

---

## What is proven, and what is not

Stated plainly, because a health app that blurs this is not trustworthy.

**Proven, and cited:** the Epley manoeuvre itself is recommended by the AAO-HNS 2017 clinical
practice guideline. The six-question triage is validated in a 585-patient randomised trial. The
target angles and the per-position tolerance bands come from Kwon et al. (Sci Rep 2023), derived
from specialists' own measured accuracy — mean error plus half a standard deviation, because
holding a patient to a tighter band than an experienced specialist achieves would leave them
hunting for a position that was already good enough.

**Not proven, and not claimed:** that *this app* improves outcomes. No trial of it exists. The
published outcome studies used head-fixed IMUs rather than a hand-held phone, and that transfer
assumption is untested. So the app claims angle accuracy, which was measured, and nothing about
cure rates, which were not.

The angle accuracy claim does not rest on trusting the handset. Every guided angle is relative to
a calibration taken on the user's own head, so a fixed sensor error appears in the reference and
in the reading and subtracts out. `SensorBiasTest` puts offsets of 3°, 9.2° and 15° about all
three axes through the real code; the resulting angles move by less than 1e-6°. It was developed
on a phone whose absolute tilt is 8.3° off true — an outlier — and it works anyway.

---

## How RevenueCat is used

A one-time unlock for **exporting the run history as a plain-text report for a clinician**, via
the RevenueCat Test Store.

The design rule was decided before the paywall was built and is not negotiable in the code:

> **No number is ever behind a paywall. Nothing safety-relevant is ever behind a paywall.**

The safety screener, the abort gesture, the triage and every angle reading are free forever. A
paywall that lets someone proceed *unguided* is an active harm, not a growth tactic. What is paid
is workflow — turning a log into something a doctor can read in four minutes — never capability.

It is a one-time purchase rather than a subscription, because a condition that resolves in one to
three sessions does not justify recurring billing. `Entitlements` is read by exactly one thing:
the export button. No health data goes near the purchase path.

---

## Technical choices

- **`:core` has no Android dependency at all.** Quaternion algebra, swing-twist decomposition,
  angle unwrapping, the dwell gate, the triage, the after-care rules — all pure Kotlin, all
  testable on the JVM in seconds. **187 tests across 25 suites**, no emulator, no device farm.
  The angle maths was proven correct before the phone was ever plugged in.
- **Reversal for accuracy, not bought hardware.** Two readings 180° apart separate a surface's
  tilt from the sensor's own error. Folded paper gives exact 30/45/60°; gravity makes face-up and
  face-down exactly 180° apart. Measured: device offset −8.32°, confirmed face-down, noise 0.07°
  peak-to-peak, drift 0.006° over 107s.
- **A single sensor cannot tell "the head turned" from "the phone slid"** — both are the same
  rotation. It can tell them apart by speed, so rotation past 250°/s latches a warning and
  demands recalibration. And because the manoeuvre returns the user to upright, which is the pose
  the calibration was taken in, the app checks itself against a known answer of zero and reports
  the drift it actually finds.
- **No Android Studio, no emulator.** Everything builds from the command line on a Windows
  machine at 99% disk. Gradle wrapper committed, release build signed.
- Three mounts with honest, separate tolerances: cheek hold ±7°, headband ±5°, in-hand practice
  ±10° — and practice mode says on screen that it is measuring the phone, not your head.

---

## Repository

<!-- TODO: paste the public URL -->
MIT licensed. The commit history, the test suite and `docs/RESEARCH.md` are part of the
submission: every clinical number in the app traces to a citation, including the ones that
weakened the pitch.
