# Decision log

> **Current product: Epley Coach (BPPV), not CRT.** CRT was abandoned after the red-team review below. BPPV, killed once in this log, was revived; the objections recorded against it are re-examined and partly corrected in [docs/RESEARCH.md](docs/RESEARCH.md).

Everything decided, and everything killed, with the evidence. Written so any future session
can pick this up cold without re-running the research.

Last updated 2026-09-07.

---

## The decision

**Build an Android app that measures Capillary Refill Time (CRT) from a fingertip on the rear
camera lens with the torch on, and works across skin tones where the naked-eye method
demonstrably does not.**

Target: **Shipaton 2026 Next Gen Award only.** Deadline 30 Sept 2026, 11:45pm PT.

---

## Hard constraints (verified, not assumed)

| Constraint | Evidence |
|---|---|
| **Next Gen is the only enterable prize** | Every other category requires a published store listing. No Play Console account exists; registering now triggers the 12-testers/14-days rule for post-Nov-2023 personal accounts, making the earliest possible live date **Oct 5** against a **Sept 30** deadline |
| **Android only** | Windows dev machine. No Xcode, no iOS Simulator. Rules out native Swift and the iOS half of Flutter/KMP |
| **No store publication needed** | Next Gen: *"no App Store or Google Play release is required"* — submit video + open-source repo |
| **Must contain a working IAP** | RevenueCat SDK powering at least one purchase, or RevenueCat Ads. Test Store satisfies this with zero accounts and zero cost |
| **Video under 2 minutes** | Rules say judges need not watch past two minutes. The "How to win" blog says three. **Build for two** |
| **Student eligibility confirmed** | `oriental.ac.in` verified present in JetBrains/swot: HTTP 200, contents "Oriental College of Technology, Bhopal"; control (fake college) returns 404; not stoplisted. Devpost account must carry the `@oriental.ac.in` address |
| **Disk is tight** | ~6GB free at 99% used. Forced the command-line-tools path over Android Studio (1.4GB vs 10-15GB) |

### Next Gen scoring criteria, verbatim
1. "Is the app idea clear, useful, interesting, or original?"
2. "Does the submitted project demonstrate meaningful progress toward a working app?"
3. "Does the project thoughtfully use RevenueCat to support subscriptions, in-app purchases,
   web purchases, ads, or another monetization flow?"
4. "Does the submission show thoughtful technical choices, product thinking, and care in how the
   app was built and presented?"

Note criterion 2: *"meaningful progress toward"*, not "finished". Incompleteness is expected.

---

## Why CRT — the evidence chain

### 1. The archetype that wins

Measured across 39 RevenueCat winners (2024+2025) benchmarked against a 96-project sample of the
2026 field, plus ~46 named student-competition winners:

| Archetype | Share of submissions | Share of winners |
|---|---|---|
| **Accessibility / assistive for one named condition** | ~1% | ~5% (**5x**) |
| **Phone-as-instrument (sensor measurement)** | ~3% | ~9% (**3x**) |
| Surfaces value you already had | ~2% | 3% (won the $100k) |
| LLM wrapper / content generator | ~16% | 15%, **zero grand prizes** |
| Habit tracker / logger | **~35%** | 26% (worst per capita) |
| B2B / marketplace | ~5% | **0 of 39** |

Corroboration: Imagine Cup produced **three consecutive world champions** that were assistive tech
for a named disability (2023 auditory processing, 2024 blindness, 2025 low vision). Apple's 2026
Swift Student Challenge headline was literally "AI meets accessibility". Among ~46 student winners,
**zero** were content generators.

2025 Shipaton winners included four sensor-measurement apps across four different prize tracks:
Heartbeat Hero (ARKit CPR depth), PitchLab (camera pitch analysis), Posturely (posture),
Hearing Buddy (on-device captioning).

### 2. The demand is documented and unusually explicit

Survey of 418 intensivists (Jacquet-Lagrèze 2022, BMC Emerg Med):

| | |
|---|---|
| Use CRT routinely | **82%** (paediatric **99%**) |
| Assess it **without** a chronometer | **98%** |
| Use a stopwatch | **3%** |
| **Want a dedicated automated device** | **54%** |
| Believe trained non-medical staff could perform it | 90% |

Documentation gap: 82-99% say they use it; only **4.2%** of UK GP records contain it numerically.

Published calls for exactly this product:
- Fleming 2015 (Arch Dis Child): *"we are aware of one automated method for measuring CRT and
  **encourage further studies of similar devices**"*
- Cloutier 2025 (Am J Emerg Med): *"**emerging technologies making bedside measurement more
  reproducible and reliable must be further developed**"*
- Sheridan 2020 (Front Med): *"The difficulty to date with capillary refill is its subjective
  nature"*

### 3. Guidelines are moving toward CRT, not away

- **ANDROMEDA-SHOCK-2**, JAMA Nov 2025, 86 centres / 19 countries / n=1,501: win ratio
  **1.16 (95% CI 1.02-1.33, P=.04)**. CRT-targeted resuscitation beat usual care.
- **Surviving Sepsis Campaign 2026** (March 2026) **broadened** the recommendation from
  "septic shock" to "sepsis or septic shock".
- **NICE NG143 mandates it**: *"Measure and record temperature, heart rate, respiratory rate and
  capillary refill time as part of the routine assessment of a child with fever."*
- WHO ETAT shock criterion: capillary refill > 3 s.
- Driker 2026 (JAMA Pediatr, 52 studies, n=140,885): prolonged CRT **OR 12.06** for mortality in
  infants 0-59 days.

### 4. Humans are provably bad at it — this is the wedge

- Fleming 2015 systematic review (21 studies, 1,915 children): inter-observer reliability ranged
  from **kappa < 0.15** to 0.65.
- Anderson 2008, paper titled *"Capillary Refill Time in Adults Has Poor Inter-Observer
  Agreement"*: 95% limits of agreement **-1.7 to +1.9 s**, kappa 0.38.
- Meyer 2025 (BMC Emerg Med), 62 observers vs objective imaging: correlation
  **r = 0.14 for paediatricians**, 0.19 nurses. Categorical agreement 41-44%.

### 5. The skin-tone gap — the unique angle

- **Polfer 2018** (J Hand Surg Am), limb ischemia detection: **92.9% of Caucasian patients
  correctly identified, 23.3% of African American patients.**
- **Kelly 2026** scoping review, verbatim: *"**No documents reported on assessment of mottling or
  capillary refill time for children with dark-coloured skin.**"*
- **Jog 2023** (Indian J Crit Care Med), Fitzpatrick IV-VI patients: CRT >3 s **did not correlate
  with mortality**, while mottling did.
- Prior art that it is solvable: **Bachour 2023** (J Biophotonics),
  *"Skin-color-independent robust assessment of capillary refill time"*, 22 volunteers,
  controlled 7 kPa compression, **80% of readings within 20% of expected**.
- Precedent for why this matters: **Sjoding 2020** (NEJM), pulse oximetry — Black patients had
  ~**3x** the rate of occult hypoxaemia. FDA now expects pigment-stratified validation.

### 6. Nobody has shipped it

**240 App Store apps scanned across six query terms. Two hits, neither measures anything:**
Capillary Refill Trainer (OSF Healthcare, a training *game*, last updated Nov 2021, 0 ratings) and
SepsisToolkit (manual hold-release-tap stopwatch). Google Play: zero relevant results.

---

## The honest weaknesses (do not hide these — put them in the README)

1. **There is no gold standard.** No laboratory value *is* CRT. The reference standard in the
   literature is the median of blinded human observers — the same observers whose agreement runs
   as low as kappa 0.15. **We can claim repeatability. We cannot claim accuracy.**
2. **DiCART failed.** A purpose-built device with fixed optics and a calibrated spring scored
   **ICC 0.46** against clinicians, with device reproducibility **0.41 vs clinicians' 0.86**.
   Authors: *"does not support its use in routine practice."*
3. **Pressure cannot be measured.** Philips' smartphone-CRT patent (US11622690B2) has claims that
   **require a servo-driven cuff** — they concluded a bare phone cannot standardise pressure.
   Literature recommends 4.5-10.5 N. Our plan is to use blanch depth as an optical pressure proxy.
4. **Ambient heat degrades CRT.** In a 30.2°C tropical environment, CRT sensitivity for
   hypovolaemia was only **29%**, and simple limb-coolness palpation outperformed it.
5. **The biomarker correlation is thin.** Spearman rho 0.681 in one 23-patient ICU series;
   rs 0.24 in another.
6. **We cannot test on diverse skin tones.** The developer cannot recruit participants. The
   skin-tone claim is currently supported by algebra and synthetic tests, not by human data.
   This must be stated as such.

---

## Candidates killed, with reasons — do not revisit

### Killed on the domain (education assessment)
Education measurement is **the most given-away category in software**, and Shipaton requires a
working IAP:

| Product | Price | Scale |
|---|---|---|
| Google Read Along | Free | 40M readers, **India-first**, 6 Indian languages |
| Microsoft Reading Coach | Free since Jan 2024 | Bundled into Teams and Windows |
| XtraMath | Free | 6.2M students, since 2009 |
| Wadhwani AI ORF | Free | 34M assessments, 100,255 Indian schools |
| IIT Bombay TARA | Free | ~700,000 KVS students, live on Play India |

Specific kills: **oral reading fluency** (miscue typing is beyond SOTA at 17.9-36.3% diagnosis
error; Google/Microsoft free), **math automaticity** (XtraMath shipped per-fact latency since
2009; India's real problem is upstream — 69% of Grade 5 can't do 3-digit division untimed),
**handwriting** (human raters agree at ICC 0.49 / kappa 0.15 on image-based legibility; no
ground truth exists).

### Killed: BPPV / Epley angle guidance
Was the recommendation for a day. Killed by:
- **DizzyFIX built exactly this** — phone on forehead, accelerometers, real-time angle path,
  timers — won an RCT (9.65/11 vs 4.67/11, p<.0001), then **delisted the app and now sells a
  $59.95 plastic hat clip.**
- **Kwon 2026** published the validation this year: 88 patients, IMU + audio guidance vs
  specialist, 61.1% vs 66.7% success, equivalent. No longer novel.
- The design is **geometrically impossible**: by position four of the Epley the face points at
  the floor. Phone falls off, screen faces away. Everyone who shipped converged on audio-first.
- **Demand is near-zero**: max rating count in the entire iOS vertigo category is **67**. Top
  Android Epley app: 1K+ installs. Every BPPV app in the India App Store: zero ratings.
- **Free videos already work**: JAMA Neurology 2023, web questionnaire + videos, no sensors,
  72.4% vs 42.9% resolution.
- **India is occupied**: NeuroEquilibrium runs sensor-based BPPV guidance at ±4° across 300+
  clinics in 90+ cities, and raised $11M Series B in Jan 2026. Abbott gives its vertigo app away
  free.

Counterweight worth remembering if this is ever revisited: von Brevern 2007 found **only 8% of
BPPV sufferers receive effective treatment**, and EDs reach correct diagnosis 17-38% of the time.
The need is real; the app market is dead.

### Killed: Tinnitus psychoacoustic measurement
- **NICE, verbatim**: *"Do not offer psychoacoustic tests, for example pitch and loudness
  matching, to assess tinnitus"* — partly on **harm** grounds. AAO-HNSF rejects all four measures
  by name. British Society of Audiology has a section titled *"Assessments that should be avoided
  in a clinical setting"*; pitch matching is item one.
- **The spec was inverted.** Test-retest ICC: pitch matching 0.55, loudness in dB SL **0.29 with
  a CI including zero**. The reliable measures are minimum masking level (0.70) and residual
  inhibition (0.84).
- **Manning 2019, n=223**: psychoacoustic measures *"appear unrelated to the impact of tinnitus"*.
  A 0-10 self-rating beat the entire battery.
- **Two live patents** cover 2AFC adaptive tinnitus pitch matching on a smartphone: US10682078B2
  (Univ. of California, **expires 2035**) and US11419526B2 (Starkey).
- **FDA explicitly regulates it**: its published list names software that uses the phone speaker
  *"to produce controlled levels of test tones... for conducting diagnostic hearing evaluations"*
  — product code EWO, audiometer.
- **Hzera** (March 2026) already ships the exact battery. **Whist** shipped it on Android for a
  decade at $2 and was **retired Sept 2023**.
- **Samsung Galaxy Buds** ship an FDA-cleared hearing test on Android in Q4 2026, free.

### Killed on the ground-truth filter (cannot validate without patients)
Anemia from conjunctival pallor (needs venous Hb; Sanguina's Ruby ships it), neonatal jaundice
(needs serum bilirubin + newborns; Picterus, BiliCam), **oral cancer screening** (needs biopsy;
**MeMoSA is live on Google Play in India**), cataract, cerebral palsy infant movement (best
clinical evidence of any candidate at 95% sens / 97% spec — but needs infants), spasticity/clonus,
cough/TB.

Oral cancer was the trap option: best story in any scan (India has the world's highest burden,
~55% of diagnosed cases die), undefendable measurement.

### Killed: free giant already there
Visual acuity (**Peek Acuity** — free, NGO, validated in India 2025), hearing screening
(**WHO hearWHO** — free, >85% sens and spec), respiratory rate (**Google Fit** shipped it,
MAE 0.78 br/min; **RRate** free on Play, updated Aug 2025), leukocoria (Baylor CRADLE, free,
*Science Advances*), MUAC (**a UNICEF tape costs $0.06**; POSHAN Tracker is nationwide).

### Killed on Android hardware
**Diabetic peripheral neuropathy** vibration threshold was second place until Android haptics
killed it: amplitude is a 0-255 integer that many devices round to 100%, and LRA actuators couple
amplitude to frequency. Cross-device absolute thresholds are not defensible.

### Killed as wrong shape
Reaction time (excellent method — accelerometer impact peak recovers true tap to ~4 ms vs the
touchscreen's 8-10 ms floor — but it is a technique looking for a disease), grip strength
(±2.6-4.4 kg error straddles the 28/18 kg sarcopenia cutoffs), stuttering (only self-generatable
ground truth is *acted* stuttering — circular, and likely to read as mockery), wound area
(enterprise buyers, dull demo), OSA (needs an overnight recording), scoliosis (Scoliometer in 75+
countries), essential tremor (StudyMyTremor used in 30+ studies over 20 years).

---

## Monetisation design (criterion 3)

RevenueCat's own *State of Subscription Apps 2026* (115,000+ apps, $16B+ revenue) — design the
paywall from the judges' own published benchmarks and say so in the submission:

| Finding | Number | What we do |
|---|---|---|
| Hard paywall vs freemium, D35 download-to-paid | **10.7% vs 2.1%** | — |
| D60 revenue per install | **$3.09 vs $0.38** | — |
| 1-year retention penalty for hard paywall | **None** (27% vs 28%) | — |
| Long trials (17-32d) vs short (<=4d) conversion | **42.5% vs 25.5%** | Use a long trial |
| Apps using <=4 day trials | **46.5%, and rising** | Deliberately go the other way |
| 3-day trial cancellations occurring on Day 0 | **55.4%** | — |

**Ethical constraint:** a paid gate on a paediatric-shock measurement is indefensible. The
measurement itself must always be free. Paid tier is history, trend tracking, export for a
clinician, and multi-person profiles.

---

## Build status as of 2026-09-07

### Toolchain (installed and verified)
```
adb                 1.0.41 (platform-tools 37.0.1)
build-tools         35.0.0
platforms           android-35
JDK                 Temurin 21.0.12.1 LTS   (system Java 24 is too new for AGP)
Gradle              9.7.1
ffmpeg              8.1.1
```
Deliberately **no Android Studio, no emulator** — 1.4GB instead of 10-15GB.

### Code
`core/` — pure Kotlin, JVM-testable, no Android dependency.
- `Signal.kt` — timestamped samples, median and median-absolute-deviation helpers
- `CrtAnalyzer.kt` — level estimation, release detection, threshold crossing, repeatability
- Tests: **19/19 passing**, round-trip against synthetic curves with known refill times

### Three bugs the tests caught (each would have shipped a silently wrong number)
1. **Flat plateau broke release detection.** A `>=` walk-back ran backwards through the entire
   press on a cleanly held compression. Fixed by anchoring on plateau *level*, not slope.
2. **Asymptotic baseline made every reading short.** Estimating resting perfusion from the tail
   left the reference ~1% high, inflating the threshold from 0.100 to 0.109 of the excursion.
   Predicted 1.925s against a 2.0s target; measured 1.900s. Now measured from genuinely at-rest
   frames before compression.
3. **Fixed tolerance biased release late.** Allowing frames within 5% of excursion to count as
   "still compressed" meant slow refills lingered in the band after release. The error **grew
   with refill time** — smallest on healthy fingers, largest on the slow readings that matter.
   Tolerance now derives from the plateau's own median absolute deviation.

### Next
CameraX capture with torch on and exposure locked, per-frame lightness extraction, guided
press-hold-release flow, RevenueCat Test Store paywall. Needs a physical Android phone with USB
debugging enabled.

---

# Red-team review + technical research, 2026-09-07 — DESIGN CHANGED

Two adversarial and technical reviews were commissioned. They broke two things previously treated
as strengths and found one bug in shipped code. **This section supersedes everything above it.**

## Positioning: research and teaching instrument, NOT patient-facing

Never a verdict. No colour-coded gauge, no threshold line at 2 or 3 seconds, no "seek care".
Output is a waveform plus a number plus an uncertainty interval.

Framing: **"the first CRT tool that records the covariates CRT has always needed"** — ambient
temperature, hold duration, release velocity, site, time. No bedside clinician records any of
these, and they are why the sign is unreliable. Converts the confounder criticism from fatal into
the reason the project exists.

## THE KEY TECHNICAL INSIGHT: tau is scale-invariant

If the tissue signal is `I(t) = A - B*exp(-t/tau)`, then melanin attenuation, LED brightness,
locked-but-unknown exposure, ISO and white-balance gains **all multiply the signal by an unknown
constant k**. That scales A and B and **leaves tau completely unchanged.**

This is why Bachour's method survived Fitzpatrick I-VI. Consequences:
1. We never need to know the exposure value, only that it does not change mid-measurement.
2. We do not need MANUAL_SENSOR. AE/AWB *lock* is sufficient and far more widely available.

## BUG FOUND IN SHIPPED CODE: use linear intensity, not CIE L*

CIE L* is a cube-root function of luminance. **An exponential in linear intensity is NOT an
exponential in L*.** Fitting or threshold-crossing in L* gives a bias that depends on the DC
level, which is exactly skin tone. This would have silently reintroduced the dependence the
project claims to remove, and the synthetic tests could not catch it because they generate
exponentials directly in the sample space.

**Fix: mean LINEAR GREEN-channel intensity.** Green because oxyhaemoglobin absorbs at 540 nm and
577 nm; it is what the CRT literature uses. Red saturates against a torch-lit fingertip and a
clipped channel carries zero information — log it anyway as a saturation quality flag.

## THE THREE ATTACKS WITH NO GOOD DEFENCE

### 1. Blanch depth cannot proxy applied force — KILLED
Capillary pressure is 25-35 mmHg. The recommended CRT force of 4.5-10.5 N over about 1 cm2 of
fingertip is **340-790 mmHg**, five to twenty times capillary pressure. The bed is fully emptied
at the bottom of that range; above it, more force changes arteriolar occlusion depth and the
reactive-hyperaemia response but produces almost no further lightness change. **The proxy is flat
exactly where the variance lives.** Force-to-blanch mapping also shifts with tissue compliance,
altered by oedema and dehydration, so the nuisance variable is confounded with the target.

Replacement: a lower-bound occlusion detector claiming nothing more than "past the blanch
plateau"; enforced hold duration; and **release-velocity gating** — a slow finger lift records as
slow refill, is plausibly a larger error than force magnitude, and IS detectable from the
leading-edge shape. The video must state force is uncontrolled and **vary press force on camera
to show the output move.**

### 2. Amplitude normalisation does not deliver skin-tone invariance
Melanin absorption scales about lambda^-3.5 over a double epidermal pass, so signal is attenuated
superlinearly while shot noise, read noise and quantisation are unchanged. **Normalisation
rescales signal and noise together and cannot recover SNR.** Reported ~15% signal-quality drop
from Fitzpatrick I to VI.

Second-order effect: **auto-exposure is itself skin-tone dependent.** A dark fingertip drives
longer exposures, giving darker-skinned users a lower effective frame rate on top of a lower
signal.

Narrowed claim: *"removes the observer's absolute-colour-judgement failure mode; it does not
remove the melanin SNR limit, which we measured and report."* Ship a per-measurement quality
metric from fit residuals, refuse to emit a number below threshold, and **publish rejection rate
as a function of baseline lightness even if it climbs on darker skin.**

**Decision: recruit 5-10 participants across skin tones.** Small sample, wide error bars, honest.

### 3. No reference standard means this can never be validated
Reference is the median of human observers, kappa < 0.15, r = 0.14. Agreement is capped by
reference unreliability, so **a perfect device scores badly** — plausibly why DiCART "failed".
Does not improve with effort, data or hardware.

"We only claim repeatability" is partly evasion: a device can be perfectly repeatable because it
is dominated by a stable per-person artifact such as finger size, habitual press force or phone
model. Deliverable is an **open instrument plus an honest error budget**, permanently.

## FATAL TO VALIDITY, UNFIXABLE IN SOFTWARE: the lens is the compressor

In every published CRT method the camera observes from a distance while a **separate** mechanism
applies compression — Bachour uses a controlled 7 kPa applicator. In our design the lens IS the
compressor. Releasing pressure changes contact area, skin flattening, lens distance and specular
geometry **simultaneously with and confounded with** perfusion recovery. Some of the measured
brightness change is mechanical, not physiological. Bachour's co-circular polarisers, central to
their subsurface selectivity, have no smartphone analogue.

**No software fix exists. State it explicitly in README and video.**

Related: finger-on-lens never reaches zero contact pressure. Optimum PPG contact pressure is
about 35-95 mmHg, trivially reached by *resting* a finger on glass. So the resting baseline is
perfusion under partial occlusion and the recovery asymptote is suppressed below true resting
perfusion.

## Four further findings that change the build

**Fast refill is also pathological.** Warm septic shock presents with *flash* refill under 1 s.
Any "long equals bad" threshold hands the parent of a vasodilated septic child a reassuring 0.8 s.
Structural blind spot in the sign. State it in the first ten seconds of the video.

**Thermal drift is instrumental.** CRT falls about 5% per degree C of skin temperature and 1.2%
per degree C ambient. Torch LED and SoC heat the contact patch. AOSP warns max-strength torch
causes thermal throttling, and flash drivers can reduce LED current with **no API telling you** —
a falling LED output mimics or masks a refill curve. Keep torch windows under 30 s, torch off
between measurements, cool-down after 3-4 runs, include a baseline-drift term in the fit.

**Reactive hyperaemia inflates test-retest ICC.** Sustained occlusion changes the vascular state
for the next measurement, so back-to-back repeats are autocorrelated and the repeatability number
is **fake in a known direction**. Report **between-session ICC** (finger repositioned, app
restarted, 2+ minute gap) next to within-session, and present the gap as a finding.

**Never report milliseconds.** 0.1 s resolution with an explicit uncertainty interval from fit
residuals. Biological within-person CRT variability is hundreds of milliseconds; the third digit
is meaningless even with a perfect sensor.

## Confounders vs the 2-3 s decision threshold (Schriger and Baraff)
- Age: CRT rises about 3.3% per decade. Sex: about 7% lower in men.
- Ambient temperature about 1.2% per degree C; skin temperature about 5% per degree C.
- **95th-percentile upper limit of normal: 2.9 s in adult women, 4.5 s in the elderly** — so
  "abnormal" for a child sits inside the normal range for a healthy grandmother.
- **Site mismatch:** guideline thresholds derive from the nail bed pressed for 5 s. We measure
  finger pulp against a lens. Not the same vascular bed, not the site the thresholds came from.

## CAMERA IMPLEMENTATION (verified against AOSP and CameraX docs)

**Design for AE/AWB LOCK, not MANUAL_SENSOR.** `CONTROL_AE_LOCK_AVAILABLE` and
`CONTROL_AWB_LOCK_AVAILABLE` are guaranteed true on any device with `BURST_CAPTURE`, far more
common than `MANUAL_SENSOR`. No published statistic exists for MANUAL_SENSOR availability, so do
not gate on it. LEGACY devices may report false: probe at runtime and refuse rather than report a
wrong number.

**Sequencing is the part people get wrong:**
1. Bind Preview + ImageAnalysis. **Do NOT bind ImageCapture** — `takePicture()` turns the torch off.
2. Torch on via `cameraControl.enableTorch(true)`, which requires a bound use case.
3. Wait for the user to place the finger. Detect it: mean Y jumps, chroma goes strongly red,
   spatial variance collapses.
4. **Wait 1.5-3 s** for AE/AWB to converge ON THE FINGER SCENE and for the LED to warm up.
5. Only then apply `CONTROL_AE_LOCK` and `CONTROL_AWB_LOCK`.
6. **Verify** via CaptureResult that `AE_STATE_LOCKED` and `AWB_STATE_LOCKED` actually took.

Locking before step 3 sets exposure for room light, so the lit fingertip blows out to saturation
and there is no signal. Never locking means AE fights the blanch, because blanching IS a large
luminance change and AE exists to cancel exactly that.

**Always set:** `CONTROL_AF_MODE_OFF`, `NOISE_REDUCTION_MODE_OFF` (temporal NR is cross-frame
leakage), `EDGE_MODE_OFF`, `CONTROL_SCENE_MODE_DISABLED`, `CONTROL_AE_ANTIBANDING_MODE_OFF`,
video stabilisation off, `CONTROL_AE_TARGET_FPS_RANGE` of Range(30,30) after querying
`CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES`. Do not use CameraX Extensions.

**Use `Camera2Interop.Extender`** (applied to every request) for anything that must stay fixed.
`Camera2CameraControl.setCaptureRequestOptions` can be cleared by CameraX internal updates.

**Log per frame and invalidate the run if any change:** `SENSOR_EXPOSURE_TIME`,
`SENSOR_SENSITIVITY`, `COLOR_CORRECTION_GAINS`, `SENSOR_TIMESTAMP`.

**Timing:** `ImageProxy.imageInfo.timestamp` is start-of-exposure in nanoseconds from
`android.sensor.timestamp`. The clock depends on `SENSOR_INFO_TIMESTAMP_SOURCE` (REALTIME vs
UNKNOWN) and CameraX gives no direct API to query it. **Rule: derive every time value from frame
timestamps only, never `System.nanoTime()` or `SystemClock`.** Detect release from the image data,
not from a button press.

**YUV to mean RGB cheaply:** BT.601 YUV to RGB is affine, so mean-of-outputs equals the transform
of mean-of-inputs. Sum Y over the ROI honouring `rowStride`; sum U and V honouring `rowStride`
AND `pixelStride` (2 for NV21/NV12-backed buffers, which bites everyone); then apply the inverse
once to three scalars. True mean R, G, B at Y-plane cost. Invalid if pixels clip, so track the
fraction with Y above 250. Note some Android phones capture full-range 0-255 YUV rather than
limited-range 16-235; this affects scale only, so tau survives.

**ROI:** centred rectangle about 20-40% of frame width. Edges have vignetting and poor contact.
**Resolution:** 640x480, the ImageAnalysis default, is correct since we spatially average.
**Frame rate:** 30 fps. CRT is a 0.5-5 s phenomenon and 15-150 samples is ample. Do not chase 60.
**Analyzer must run under about 5 ms** or `STRATEGY_KEEP_ONLY_LATEST` silently drops frames.
Monitor inter-frame delta-t and invalidate runs with a gap over 100 ms in the first second after
release. **Discard the first 1-3 s** for camera stabilisation and LED warm-up.

## Reference method to follow: Bachour 2023
22 volunteers, Fitzpatrick I-VI, controlled 7 kPa compression, co-circularly polarised white LED,
**mean GREEN channel over the ROI**, **CRT = the time constant tau of an exponential regression**,
adaptive window selection, flags inadequate readings. Result: about 80% of readings within 20% of
expected, repetition SD 17%, and **about 20% of measurements flagged and discarded.**
That 20% rejection rate is the benchmark to compare our own quality gate against.

## Demo design
1. **Synthetic video with a programmed exponential tau** that the estimator recovers within a
   stated tolerance. Described as "the single most credible artifact you can produce in twenty days".
2. **Bidirectional physiological response**, far more persuasive than one direction: cold
   immersion **slows** refill (Schriger: healthy subjects 1.3 s to **2.9 s**, so this is ALSO the
   app's number one false-positive pathway and MUST be labelled as such); post-occlusive reactive
   hyperaemia (BP cuff suprasystolic for 60 s, then release) **speeds it up**.
3. **Show a measurement being rejected** by the quality gate. Highest credibility density in the
   whole video.
4. **Vary press force on camera** and let the output move.

## Regulatory posture
FDA revised **General Wellness guidance January 2026**: intended use must be unrelated to
diagnosis, and outputs must not be characterised as abnormal or pathological — a red "4.2 s, seek
care" screen fails both prongs. EU MDR Rule 11 puts diagnostic decision-support at Class IIa or
above. India CDSCO issued medical device software guidance in **October 2025** bringing screening
and clinical-decision-support software under Medical Device Rules 2017. "Not published to a store"
is a fig leaf. Ship a prominent **NOT A MEDICAL DEVICE** notice in README and in-app.

## Prior art to cite (claiming novelty is a credibility hit)
- **US11712165** — smartphone measurement of cyanosis and capillary refill time, CNN-based
- **US11622690B2** (Philips) — claims require a servo-driven cuff; also uses the Pade-Laplace
  method to extract multiple time constants, allowing tau extrapolation before full recovery
- **Cap App** (Minnesota) — n=152, bias 0.01 s but **SD 1.31 s**
- **Miyazawa 2026**, Instruments 10(1):15 — closest published analogue, smartphone quantitative CRT
- **CRTApp / NCT07473869** (AP-HP Paris) — prospective validation examining skin tone, lighting
  and phone variation, starting June 2026
- **DiCART** — Descamps 2025, ICC 0.46, "does not support its use in routine practice"
- **Bachour 2023** — J Biophotonics, the method we follow

Honest contribution: industry patented this behind a servo cuff and concluded pressure must be
mechanically standardised, and the registered trials are closed-source. **Ours is the open,
auditable, reproducible implementation plus a published error budget.**

## Monetisation
**No number is ever behind a paywall. Nothing safety-relevant is ever behind a paywall.**
Gate only research utility: CSV and raw-waveform export, longitudinal history, multi-subject
study sessions, batch analysis. A researcher and educator licence, one-time, low price, free on
request. No health data touches the purchase path.

## Privacy (a scoring opportunity, and rare)
100% on-device, zero frame retention, no network on the measurement path, no analytics on health
values. India's DPDP Act 2023 imposes verifiable parental consent for children's data, another
reason the tool is not aimed at parents.

## Things nobody could verify — do not state as fact
- No published statistic exists for the fraction of Android devices supporting MANUAL_SENSOR.
- LED warm-up drift on smartphones is practitioner lore, not a characterised quantity.
- **No CRT-from-video work using CIE L\*a\*b\* was found** despite targeted searching.
- CameraX RGBA_8888 byte order in the docs appears inconsistent with PixelFormat convention.
  Test empirically against a known-colour target.
- Miyazawa 2026 full text was inaccessible (MDPI returned 403).
