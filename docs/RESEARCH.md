# Validation research — 2026-09-16

Done with 14 days to the deadline, to answer four questions honestly before building further:
is the problem real, would a doctor endorse this, can actual patients use a phone for it, and
what have Shipaton judges rewarded. Every claim below was checked against the source linked
beside it. Claims from earlier research that could **not** be re-verified are marked as such.

---

## Verdict

**Stay with BPPV. Reposition it, and fix one design flaw the evidence exposes.**

Pivoting with 14 days left would throw away the only part of this project that was hard and is
now proven on hardware. But the pitch we had — "a phone instrument so anyone can self-treat
vertigo" — is the weak version, and a judge who reads the underlying papers would find three of
our claims overstated. The strong version is narrower, and every sentence of it is backed:

> **BPPV comes back.** When it does, the patient usually repeats what worked last time — and a
> 585-patient randomised trial showed that fails most of the time, because recurrences often
> move to a different canal. Epley Coach does the two things the trials show actually matter:
> it works out which canal is involved using the questionnaire validated in that trial, then
> guides the manoeuvre at the right angles using the phone's own sensor.

---

## 1. Would a doctor recommend this?

A random vertigo app for an undiagnosed person: **no, and they should not.** Positional vertigo
can be a stroke, and an app cannot see the eye movements that tell them apart.

A tool for a patient a doctor has **already diagnosed**, to use when it comes back: **that is
the model clinicians themselves designed and tested.**

| Evidence | Source |
|---|---|
| AAO-HNS guideline: posterior canal BPPV **should** be treated with a repositioning manoeuvre; vestibular rehabilitation may be offered self-administered or with a clinician | [AAO-HNS 2017 CPG](https://aao-hnsfjournals.onlinelibrary.wiley.com/doi/10.1177/0194599816689667) |
| JAMA Neurology 2023 RCT, 585 patients, four Korean medical centres: **previously diagnosed** patients self-treated recurrences at home. Web questionnaire + video: **72.4%** resolution. Video using the previous diagnosis: **42.9%** | [Choi et al., JAMA Neurol 2023](https://pmc.ncbi.nlm.nih.gov/articles/PMC10011937/) |
| Home repositioning after office treatment: 8.3% more effective, recurrence reduced 9% | Secondary source only, cited in [Medscape guideline summary](https://emedicine.medscape.com/article/884261-guidelines) — **not verified against the primary paper** |

**Implication:** the user is a diagnosed patient. The app is what happens *after* the doctor, not
instead of one. Onboarding must say so, and the red-flag screen must stop anyone whose symptoms
do not match their diagnosed episodes.

---

## 2. Is the problem real, and who has it?

### Recurrence is the market, not first episodes

| | Source |
|---|---|
| ~15% recur per year; **50% by 40 months** | [Nunez et al., Otolaryngol HNS 2000](https://pubmed.ncbi.nlm.nih.gov/10793340/) |
| One-third recur over ~5 years (posterior canal) | [Acta Oto-Laryngologica 2010](https://www.tandfonline.com/doi/abs/10.3109/00016481003629333) |
| Higher recurrence: women, older age, head trauma, Ménière's | [Meta-analysis, PMC11983856](https://pmc.ncbi.nlm.nih.gov/articles/PMC11983856/) |

A first episode needs a doctor. A recurrence is the same person, months later, often waiting
weeks for an appointment for a problem they already know they have.

### Who the users actually are

| | Source |
|---|---|
| India, 3,975 BPPV patients: **median age 51, 56.6% women** | [Neurology Clinical Practice](https://www.neurology.org/doi/10.1212/CPJ.0000000000200191) |
| Idiopathic BPPV in India: women 2.3 : 1 | [Egyptian J Otolaryngol 2023](https://link.springer.com/article/10.1186/s43163-023-00456-6) |
| Trial participants mean age 56–61 | JAMA 2023; CEO 2026 |

---

## 3. Can patients actually use a phone for this?

**Mostly yes. The oldest users need a helper, and the design should expect that.**

| | Source |
|---|---|
| US smartphone ownership: **90%** aged 50–64, **78%** aged 65+ | [Pew Research 2025](https://www.pewresearch.org/internet/fact-sheet/mobile/) |
| JAMA trial: 15.1% excluded because they **could not use the internet**; 1.1% spinal problems; 0.3% cognitive | JAMA 2023 |
| Patients who needed assistance were older: **68 vs 58** | JAMA 2023 |
| **Safety across 585 self-treating patients: no falls.** Only nausea, vomiting, mild headache | JAMA 2023 |

The core user (50s) is overwhelmingly phone-capable. The 70+ tail is where a family member helps
— so "a helper holds the phone and reads the screen" is a real use case, not an edge case.

---

## 4. Does sensor guidance actually improve the manoeuvre?

| | Source |
|---|---|
| Head-angle error: specialist-guided **13.7–24.4°**, self-administered **40.0–51.5°** | [Sci Rep 2023 pilot, n=19](https://pmc.ncbi.nlm.nih.gov/articles/PMC9950366/) |
| IMU + audio feedback reduced error on every manoeuvre step | Sci Rep 2023 |
| 95.9% of IMU-guided attempts within the specialist-derived target range | [CEO 2026](https://www.e-ceo.org/journal/view.php?doi=10.21053%2Fceo.2026-00070) |
| 88 patients, IMU-guided vs specialist: Epley **61.1% vs 66.7%**, barbecue roll 50.0% vs 62.5%, both P>0.05 | CEO 2026 |
| **23.5%** of initially resolved IMU patients had early recurrence within 24 h; authors call for **post-manoeuvre guidance** | CEO 2026 |

---

## 5. Corrections — claims in our own documents that were wrong

These are the ones a judge who reads the papers would catch.

1. **"Statistically equivalent to a specialist (n=88)"** — overstated. The 2026 study was a
   *prospective comparative* study with matched groups, **not randomised**, and the authors state
   the findings are "hypothesis-generating because the noninferiority margin was not
   pre-specified." Correct wording: *no significant difference in a non-randomised comparison.*

2. **That study used a dedicated wearable IMU, not a phone.** Using a phone as the sensor has
   never been clinically tested. We can claim *angle accuracy* (once bench-tested); we cannot
   claim *clinical outcomes* for a phone.

3. **"DizzyFIX won an RCT"** — misleading. The randomised study was **41 medical students
   performing the manoeuvre on a healthy volunteer**, scored for correctness (9.65 vs 4.67 of 11,
   P<.0001). No patients, no cure rates. The authors note it "did not explicitly demonstrate that
   a more correct PRM translates into higher treatment success."
   [JABFM 2015](https://www.jabfm.org/content/28/1/118). Its current store status was **not
   re-verified**.

4. **NeuroEquilibrium "$11M Series B, Jan 2026"** — not found. What *is* verified: 300+ clinics,
   90+ cities, 150,000+ patients. They are clinics — where diagnosis happens — so they are the
   step before this app, not a substitute for it.

---

## 6. The design flaw the evidence exposes

**The app currently asks "which ear were you told is affected?"** That is exactly the control arm
of the JAMA trial — treating a recurrence based on the previous diagnosis — and it got **42.9%**.
Among the control group's failures, **56% had a different type of BPPV** from the one diagnosed
at enrolment. Recurrences move.

The fix is published. Six questions ([JAMA 2023](https://pmc.ncbi.nlm.nih.gov/articles/PMC10011937/)):

1. Do you have a spinning or whirling sensation of the surroundings or yourself?
2. Do you feel dizzy mostly when your head is moved?
3. Does the dizziness last less than 3 minutes?
4. Which causes more dizziness: lying down / getting out of bed, **or** turning your head while lying down?
5. Which direction causes more dizziness: turning your head right **or** left?
6. Does the dizziness from turning your head last less than 1 minute, **or** more?

Questions 1–3 screen out non-BPPV causes. Questions 4–6 identify the canal and side.

Honest limits to state alongside it: the questionnaire's accuracy in the authors' earlier study
was **71.2%**, and this app only guides the **posterior canal** Epley — if the answers point to
the horizontal canal, the app must say the Epley is the wrong manoeuvre rather than run it.

---

## 7. Safety boundary

| | Source |
|---|---|
| 20–25% of posterior circulation strokes are misdiagnosed at first presentation; dizziness is a key confounder | [Case report and review, 2026](https://pubmed.ncbi.nlm.nih.gov/42017872/) |
| Red flags: weakness on one side, falling to one side, hearing loss, skew deviation; atypical nystagmus | [ED vertigo review, PMC12923994](https://pmc.ncbi.nlm.nih.gov/articles/PMC12923994/) |

Rules that follow from this:
- Previously diagnosed BPPV only. Stated at the first screen.
- Unskippable red-flag screen before every run. Never behind the paywall.
- If this episode feels different from the diagnosed ones, stop and see a clinician.
- Questionnaire questions 1–3 failing ends the session, not just the manoeuvre.

---

## 8. What Shipaton judges rewarded in 2025

[Shipaton 2025 winners](https://www.revenuecat.com/blog/company/shipaton-2025-winners)

| Winner | What it was | Why it matters to us |
|---|---|---|
| **Heartbeat Hero** — Peace Prize, 1st | CPR coaching: **ARKit + IMU** measure compression depth; voice guidance, haptic cues | **Our exact archetype, and it won.** Phone sensors measure a medical procedure in real time and coach by voice. Also means we are not novel in archetype and will be compared to it |
| PitchLab — Design, 3rd | iPhone measures pitch velocity and spin | "Democratizes access to elite-level data" — the same argument as specialist-grade angles at home |
| Posturely — KMP, 2nd | Posture monitoring from phone/AirPods sensors | Sensor-measurement health apps place |
| Vector Guard — HAMM, 1st | Disease-vector identification; each subscription funds 50 free accounts | Monetisation tied to the mission is rewarded explicitly |
| Payout — Grand Prize | Built with Claude Code and Cursor | AI-assisted builds are celebrated, not penalised |

**2026 Next Gen:** judged on a demo video (judges not required to watch past **2 minutes**) and
**open-source code**. $20k / $10k / $5k. 26,048 registered across the whole hackathon.
[Devpost](https://revenuecat-shipaton-2026.devpost.com/)

Heartbeat Hero sets the bar. We beat it on evidence, not archetype: a published triage, measured
drift, verified accuracy, and honest limits written down.

---

## 9. Risks that remain after all this

1. **Phone-as-sensor has no clinical outcome data.** We can only ever show angle accuracy. The
   bench test against a digital angle finder is the one number the video should lead with.
2. **Position four puts the screen face-down.** Audio is not a nice-to-have; without it the
   manoeuvre cannot be completed.
3. **The triage is 71% accurate.** Say so. Treat it as better than guessing from last time —
   which the trial shows it is — not as a diagnosis.
4. **Market size is small and unproven.** Earlier research found near-zero ratings across the
   vertigo app category; **not re-verified today**. Do not make market-size claims.
5. **We will be compared with Heartbeat Hero.** Lead with what it did not have: a randomised
   trial behind the method.

---

## 10. Build priorities for the remaining 14 days, in order

1. Replace "which ear were you told" with the six-question triage, including the exit paths
2. Guidance in two channels: illustrated pose preview and live head-over-target figure on screen, plus voice and haptics for the moments the screen is out of sight — the successful trial arms all showed patients the manoeuvre (video), not only told them
3. Unskippable red-flag screen
4. Post-manoeuvre guidance and a recurrence log (the gap the 2026 authors named)
5. Bench accuracy test with an angle finder → the headline number
6. RevenueCat paywall — triage, safety and a first run always free
7. Helper mode: instructions addressed to the person holding the phone
8. Demo video, under 2 minutes, open-source repo public

---

## 11. Hardware finding — 2026-09-23: the test phone's tilt is 9° out, and it does not matter

The accuracy self-check on a Motorola Edge 40 Neo, phone flat on a hard floor:

| Measurement | Result |
|---|---|
| Fused orientation vs raw gravity sensor | agree to **0.01°** — so this is not a maths error |
| Reversal (two readings, 180° apart in place) | floor **−0.93°**, device offset **−9.23°** |
| Face-down cross-check | **−169.0°** rather than 180°, the same ~11° offset |
| Rotating the phone in place | tilt barely changed — the offset follows the phone, not the floor |

The phone's accelerometer reports gravity about 9–10° off true. That is a fault in the device's
factory calibration, not in this app.

**It does not affect the guidance.** Every angle the app acts on is measured relative to a
calibration captured on the user's own head, so a fixed device-frame offset appears in both the
reference and the reading and cancels exactly. `SensorBiasTest` proves this for offsets up to 15°
about any axis: the measured head angles change by less than 1e-6 degrees.

**What it does affect** is any absolute tilt reading — which is precisely what the self-check
measures, and why the check exists. The screen now says so when the offset is large.

**Two defects were found while chasing this**, both fixed:
- The display slept when the phone lay face-down, and non-wakeup sensors stop when the processor
  suspends. A phone against a cheek looks identical to a pocket, so a manoeuvre would have stalled
  silently. The app now holds a partial wake lock while it is in front.
- The CSV log only wrote rows once a calibration existed, so nothing could be recorded when the
  screen could not be read. It now logs raw device and gravity tilt from the first sample.
