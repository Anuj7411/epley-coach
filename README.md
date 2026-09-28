# Epley Coach

**An Android app that measures your head angle while you treat your own vertigo, and refuses to
count a position until your head is actually in it.**

Built solo for RevenueCat Shipaton 2026, Next Gen track.

---

## The problem

BPPV is the most common cause of vertigo. The Epley manoeuvre cures it by rolling displaced
crystals out of the wrong ear canal: free, drug-free, five minutes. It only works if the head
angles are right, and two things go wrong when people do it alone.

| | |
|---|---|
| BPPV lifetime prevalence | 2.4% |
| Of those, receive effective treatment | **8%** |
| Recurrence | ~15% per year; **50% within about 40 months** |
| Head-angle error, self-administered from a video | **40–51°** |
| Head-angle error, specialist-guided | 13.7–24.4° |
| Self-treating a recurrence from the **previous** diagnosis (RCT, n=585) | **42.9%** resolved |
| Self-treating it after a **six-question canal triage** (same RCT) | **72.4%** resolved |

The manoeuvre is not the bottleneck. **Treating the right canal** and **holding the right angles**
are, and each has a trial behind it.

## What this app does

1. **Safety check before every run.** Stroke warning signs, then reasons not to self-treat today.
   Unskippable, never behind the paywall, cleared after each run because symptoms change.
2. **Six questions** (Kim et al. 2020, as used in the JAMA Neurology 2023 trial) to work out which
   canal and which ear. If the answers point to the horizontal canal it **refuses to run the
   Epley** and names the manoeuvre that trial used instead. If they do not fit BPPV, it stops.
3. **Guided manoeuvre.** The phone's orientation sensor measures the head through all five
   positions. Voice leads, vibration confirms, the screen shows a head over its target zone.
4. **After-care.** Repeat once in an hour if still dizzy (trial protocol); no postural restrictions
   (AAO-HNS 2017 recommends against them); see a doctor if worse.
5. **A private log** of every run, so recurrences are visible to the person they keep happening to.

## What makes it different

Checked against the vertigo apps on the store (see `docs/DESIGN.md`): several guide the Epley with
animations and timers, one logs episodes, one uses motion sensors for a *balance test*.

**None of them measure the head during the manoeuvre, and none identify the canal.** This app does
both, and refuses the cases it should not treat.

## Measured, on hardware

From a 166-second recording on a Motorola Edge 40 Neo (`docs/RESEARCH.md`, `docs/SPEC.md`):

| | Measured | Requirement |
|---|---|---|
| Sample rate | 50.0 Hz | ≥ 25 Hz |
| Worst sample-to-sample jump | 3.08° | < 30° |
| Drift at rest, 107 s | **0.006°** | < 10° over 90 s |
| Drift while moving, 264 s | −4.2° | < 10° over 90 s |
| Sensor glitches, unreliable readings | 0 | 0 |

Still outstanding: accuracy against a physical inclinometer, and a run on a real head. Neither is
claimed until measured.

## Does this depend on the phone being accurate?

No, and that was tested on a phone that is not.

The test device's absolute tilt reads about **8.3° off true** — measured by reversal, with a level
floor confirmed at 0.35°. Published validation puts good phones within 1–2° of a clinical
goniometer, so this one is an outlier, and it still works. Here is why.

Every angle the app guides by is measured **relative to a calibration captured on the user's own
head** at the start of a run. A fixed device error appears in the calibration and in every reading
afterwards, so it subtracts out. `SensorBiasTest` runs offsets up to 15° about any axis through the
real code: the resulting head angles move by less than 1e-6 degrees.

That calibration is not an extra chore invented for this — it is needed anyway, because a phone
held against a cheek sits at whatever angle the user managed. Correcting the device is a free side
effect of a step the app already requires.

What remains is whether a device's error stays constant as it moves. A constant error cancels
exactly; one that varies with orientation cancels partly. Two independent measurements on the test
phone gave −9.23° and −8.32°, about **1° apart**, against per-position tolerance bands of 18.9° to
31°.

Absolute readings — the instrument screen and the accuracy self-check — do carry the device error,
which is exactly what the self-check measures. One tap stores a per-device correction. That is an
engineering tool; no user needs it to be treated correctly.

## Honest limits

- **Orientation-dependent sensor error is only bounded, not eliminated.** Measured at about 1° on
  the test device; a phone with a badly non-linear accelerometer would do worse.
- **A phone is not a strapped-on sensor.** The published outcome studies used a dedicated head-worn
  IMU. Using a phone has never been clinically tested, so this app claims angle accuracy only —
  never cure rates.
- **The questionnaire is about 71% accurate** against a specialist. The app says so on screen.
- **For people already diagnosed.** Positional vertigo can be a stroke, and an app cannot see the
  eye movements that tell them apart. Hence the safety check and the first-use wording.
- **NOT A MEDICAL DEVICE — an unregulated prototype, and it must never be used on a patient.**
  Not merely "this is not a medical device": regulators have rejected that phrasing as a defence,
  so this README and the app itself both say the stronger thing.

## How it works

```
:core   pure Kotlin, no Android — quaternions, head angles, triage, manoeuvre engine,
        cue planner, after-care rules.  158 tests, runs on the JVM in seconds.
:app    Android + Compose — sensor stream, voice and haptics, screens.  29 tests.
```

The measurement is deliberately boring: `TYPE_GAME_ROTATION_VECTOR` (no magnetometer, so a steel
bed frame cannot mislead it), quaternion **swing-twist decomposition** rather than Euler angles
(the manoeuvre passes straight through the ±90° singularity where Euler pitch flips sign), and
phase unwrapping so a pass through ±180° reads as motion rather than a 320° jump — which is what
the first hardware recording actually produced.

**The hold timer cannot lie.** It advances only while the head is inside the position's tolerance
band *and* below the stillness threshold, with hysteresis so tremor does not break a hold that has
started. A countdown that runs regardless is a stopwatch with extra steps.

**Tolerance bands are the published specialist ranges** — 26.1°, 31.0° and 18.9° for the three
therapeutic positions (Kwon et al., *Scientific Reports* 2023), not numbers we picked. Holding a
patient tighter than an experienced specialist manages would leave them hunting for a position that
was already good enough.

## Design

Dark `#121212` (pure black under off-white text causes halation), **no animation** except the hold
bar (the WCAG rule on motion exists because moving interfaces make vestibular patients dizzy),
and status colours from the **Okabe–Ito** colour-blind-safe palette with a word beside every colour.
Buttons ≥ 56 dp, body text ≥ 16 sp, one action per screen. Sources in `docs/DESIGN.md`.

## Build and run

```bash
gradle :app:assembleDebug          # needs JDK 21+ and the Android SDK (compileSdk 37)
gradle :core:test :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Documents

| File | What it holds |
|---|---|
| `docs/RESEARCH.md` | Every clinical claim, its source, and the claims of our own we had to correct |
| `docs/DESIGN.md` | Who the user is, competitor teardown, the visual system and its sources |
| `docs/SPEC.md` | Requirements, architecture, validation plan, known weaknesses |
| `DECISIONS.md` | Every idea considered and killed, with the evidence that killed it |

## Sources

- [Choi et al., *JAMA Neurology* 2023 — self-treatment of recurrent BPPV, n=585](https://pmc.ncbi.nlm.nih.gov/articles/PMC10011937/)
- [Kim et al., *Neurology* 2020 — questionnaire-based diagnosis of BPPV](https://www.neurology.org/doi/10.1212/WNL.0000000000008876)
- [Subtype questionnaire reliability in older patients](https://pmc.ncbi.nlm.nih.gov/articles/PMC10318130/)
- [Kwon et al., *Scientific Reports* 2023 — IMU-guided repositioning](https://pmc.ncbi.nlm.nih.gov/articles/PMC9950366/)
- [*Clinical and Experimental Otorhinolaryngology* 2026 — wearable IMU-guided CRP, n=88](https://www.e-ceo.org/journal/view.php?doi=10.21053%2Fceo.2026-00070)
- [AAO-HNS clinical practice guideline: BPPV (update), 2017](https://aao-hnsfjournals.onlinelibrary.wiley.com/doi/10.1177/0194599816689667)
- [Nunez et al. 2000 — recurrence after canalith repositioning](https://pubmed.ncbi.nlm.nih.gov/10793340/)

Written with Claude Code. Every clinical number in this README is linked to its source, and the
ones that turned out to be overstated are listed in `docs/RESEARCH.md` §5.
