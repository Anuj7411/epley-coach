# Shipaton 2026 strategy analysis

Next Gen (student) track. Solo builder, India, Android, Windows dev machine.
Deadline 30 Sept 2026. Last updated 2026-09-06.

Every number is sourced. Anything I could not verify is marked. Two research
streams on hackathon winner archetypes are still running; conclusions here are
provisional until they land.

---

## 1. What the organisers say wins

RevenueCat publishes a four-part "How to win Shipaton" series. It is effectively
an answer key and most entrants will not read it.

On ideas:
> "Solve a real problem / For real people / In a way that delivers delight or an
> 'aha' moment."

Three named failure modes: starting with a solution rather than a problem; scope
creep; and "building something for yourself and assuming others share your pain."
They recommend a "Minimum Lovable Product" that "ruthlessly cuts scope, while
preserving the delight."

On the pitch:
> "In the opening 15–20 seconds, start with Setup + Need."
> "A great Shipaton video is proof first, polish second."

Logline template to use literally:
> "For [who], [App Name] helps [job to be done] by [distinct approach], so they
> can [valuable outcome]."

And explicitly: **"Don't try to jam your app into every prize category — focus on
the ones where your app makes the most sense."**

Judging dimensions named: Innovation, Execution, Feasibility, Integration.

### How judging actually runs
Five-stage funnel. Two screeners score 1–5. Stated minimum: watch the first two
minutes of video, read the description, review screenshots. Downloading is
"encouraged but not mandatory." Named failure modes: incomplete submission info,
unanswered category questions, unclear first two minutes.

**The video is the primary judging surface. For Next Gen the repo is the second.**

---

## 2. Which prizes are actually winnable

23,853 registered participants. 568+ projects already in the gallery on 6 Sept.

- **Grand Prize ($100k)** is judged on "user traction and growth momentum during
  the event." Unwinnable for a solo student launching cold with 24 days left.
- **Next Gen ($20k / $10k / $5k)** — students only, video + open-source repo, **no
  store release required**. Scored on: idea clarity and usefulness, meaningful
  progress toward a working app, thoughtful RevenueCat use, technical choices and
  care.
- **Peace Prize ($20k / $10k / $5k)** — impact and feasibility.

**Next Gen and Peace Prize are the realistic targets. Neither is scored on revenue
or market size.** That matters: market data below informs whether the story is
credible, not whether we win.

---

## 3. Two years of Shipaton winners: the pattern moved

**2024** winners were conventional: Karo (social task manager), Zerocam Mono,
Party Animals (truth or dare), Flowmino (time blocking, Design 1st), Galatea.

**2025** winners were not:
- **Payout** — Grand Prize. Finds class-action settlements you already qualify for.
- **Heartbeat Hero** — Peace Prize 1st. ARKit measuring CPR compression depth.
- **PitchLab** — Design 3rd. Camera analysing baseball pitches.
- **Posturely** — KMP 2nd. Camera and sensors measuring posture.
- **Hearing Buddy** — Peace Prize 2nd. On-device speech captioning.

Four winners across four different prize tracks **measure something physical**.
The Grand Prize winner is a second archetype: **surfacing value the user already
had but did not know about**.

---

## 4. The 2026 field, measured

All 24 gallery pages scraped, keyword-counted across the full HTML.

Saturation (71-project sample extrapolated to 568):

| Lane | In sample | Est. total |
|---|---|---|
| Games | 10 | ~78 |
| Habit / focus / productivity trackers | 9 | ~70 |
| Health, fitness, nutrition | 7 | ~55 |
| Photo & camera utilities | 6 | ~47 |
| Care / family coordination | 5 | ~39 |
| AI study & learning | 5 | ~39 |
| Notes / clipboard / link savers | 3 | ~23 |

Full-field keyword counts across all 568:
```
reading fluency 0   read aloud 0    oral reading 0   pronunciation 0
fluency 0           phonics 0       dyslex 0         words per minute 0
speech therapy 0    stutter 0       literacy 1
```

**The measurement lane that swept 2025 is empty in 2026.** The field converged on
LLM wrappers over a text box.

---

## 5. RevenueCat's own market data

*State of Subscription Apps 2026*: 115,000+ apps, $16B+ revenue. The judges wrote
these benchmarks.

**Paywall design:**

| | Hard paywall | Freemium |
|---|---|---|
| D35 download-to-paid | **10.7%** | 2.1% |
| D60 revenue per install | **$3.09** | $0.38 |
| 1-year retention | 27% | 28% |

5x conversion, 8x revenue per install, no retention penalty.

**Trial length:** 17–32 day trials convert at **42.5%**, ≤4 day trials at 25.5%.
Yet 46.5% of apps use trials of 4 days or less, *up* from 42.1%. **55.4% of all
3-day trial cancellations happen on Day 0.**

**AI apps:** 41% Year-1 LTV premium, but monthly plans retain **36% worse**.

**Growth polarisation:** top quartile +80% YoY MRR, bottom quartile −33%.

**Platform:** Google Play billing failures cause 31% of cancellations vs 14% on
App Store. We are Android-only.

**Region — this is grim for an India-first product:**

| | North America | India / SEA |
|---|---|---|
| Y1 realised LTV per payer | $32 | **$14** |
| D35 download→paid | 2.6% | **1.4%** |
| D60 revenue per install | $0.55 | **$0.11** |
| Trial→paid | 34.2% | **15.2%** (lowest of any region) |

India's entire IAP market was ~$300M in Q1 2026 on ~6.2bn quarterly downloads.
Apps launched 2025 or later account for 3% of all subscription revenue.

**Tactical use:** "thoughtful RevenueCat use" is a scored Next Gen criterion.
Designing the paywall directly from these published benchmarks, and saying so in
the submission, is a free differentiator almost nobody will take.

---

## 6. THE REVERSAL: education assessment is the most given-away category in software

This broke my original thesis and is the most important finding so far.

| Product | Price | Scale |
|---|---|---|
| Google Read Along | **Free** | 40M readers, **India-first**, 11 languages incl. 6 Indian |
| Microsoft Reading Coach | **Free** since Jan 2024 | Bundled into Teams and Windows |
| XtraMath | **Free** | 6.2M students, 50+ countries, since 2009 |
| Zearn | **Free** | ~1 in 4 US elementary students |
| Wadhwani AI ORF | **Free** | 34M assessments, 9.2M students, 100,255 Indian schools |
| IIT Bombay TARA | **Free** | ~700,000 KVS students, live on Play Store India |
| ConveGenius | **Free to govt** | B2G across 7 Indian states |

Google, Microsoft and three nonprofits give away exactly what a "measure the
learner" app would sell. **Shipaton requires a working in-app purchase.** Education
measurement is therefore the one domain where our business model is structurally
impossible.

---

## 7. VRIN scorecard

| | Valuable | Rare | Inimitable | Non-substitutable |
|---|---|---|---|---|
| **A. Oral reading fluency** | Yes. ASER 2024 (n=649,491): only **27.1%** of Grade 3 read a Grade 2 text. NIPUN Bharat states literacy targets in **WPM** | **No.** Google Read Along free, India-first, 40M readers | **No.** Vosk + alignment is a weekend. And miscue typing is beyond SOTA (see below) | **No.** A teacher with a stopwatch |
| **B. Handwriting** | Partly. Kanji Kentei: **~1.37M paying ¥3,200–6,700/test/yr** in Japan. But India has no institutional hook | **Yes.** Genuinely empty | **Depends on method** — see §8 | Weak |
| **C. Math automaticity** | Yes | **No.** XtraMath, free, since 2009 | **No** | **No** |
| **D. Spoken English** | Yes. **Speak: $100M ARR**, mostly Korean consumers at $20/mo. SpeakX India: $7.5M ARR, 200k paying subs | **No.** ELSA: 10M India installs, $60M raised, shipping "get scored on delivery" as a promoted Play event *this month* | **No.** API wrapper | **No** |

**A, C and D each fail three of four.** A is rare inside the 568-project field but
not rare in the world, and judges can search.

### Technical ceiling on A, specifically
Published SOTA (Interspeech 2025, Spanish children, real classrooms, mobile):
- WCPM-style **balanced error rate 4.9–13.3%** — achievable
- **Diagnosis error rate 17.9–36.3%** for typing the miscue — not achievable

Substitution vs omission vs insertion, the exact spec I prototyped and tested on
6 Sept, is beyond current tooling. Indian-accented adult English: Whisper large
7.2 WER, Azure India model 21.3 WER. **No public Indian child-reading corpus
exists.**

---

## 8. Why handwriting deserves a second look

I killed B on the Bamberg 2025 thesis: 30 raters, 24,969 labels, human agreement
**ICC 0.49, κ 0.15**, and narrowing to "objective" sub-features made agreement
*worse*.

That result measures **image-based legibility**. It does not apply to
**stroke-based** measurement.

iFlytek publishes the industry rubric, ten dimensions. Four are deterministic:
**笔顺 stroke order, 笔画数量 stroke count, 笔画方向 stroke direction, 书写速度
writing speed.** Stroke order is right or wrong. It is a sequence comparison — the
same shape of problem as the alignment engine already built and tested here.

This matches the sharpest structural finding from the China research:
**every accuracy claim in Chinese edtech that holds up is a deterministic-answer
claim.** 小猿AI: 99.9% on 110,000 arithmetic problems, beating human graders.
Chivox: r≈0.95 on pronunciation. **Nobody anywhere publishes a
handwriting-quality accuracy number**, because there is not one to publish.

A touchscreen captures strokes natively. The data that makes photo-based
handwriting unmeasurable is free on a phone.

Counter-evidence to weigh: Dynamilis (EPFL spinout, stylus-based) has 82 US App
Store ratings. Kaligo sells B2B at £0.30–0.60/child/month with no visible traction.
河小象, the best-funded player, has taken no new capital in seven years and now
markets *against* AI scoring: "we don't do vague AI template scoring."

---

## 9. Two warnings

**AI-judges-your-child has active reputational risk right now.**
Korea's AI Digital Textbook went from **4,095 schools to 1,686 in one semester**
and was stripped of legal status as official teaching material. In the US:
Education Week, 4 Aug 2026, "School Districts Push Back Against State-Required AI
Reading Assessments"; NBC News, 7 Aug 2026, on parents alarmed by Amira in New
Mexico.

**Assessment is subscription-hostile.** One measurement, one number, no reason to
open the app tomorrow. Speak reached $100M ARR precisely because it wraps
assessment inside a daily practice loop. Any assessment product we build must
carry that loop: measure → show a gap → practise → re-measure.

---

## 10. Where this leaves us

The measurement thesis is validated — it won four 2025 categories and is empty in
2026. **The domain was wrong.** Education measurement is where the free giants live.

Open fork, pending the two archetype research streams:
1. **Stroke-based writing measurement.** Keeps education, moves to the part that is
   deterministic and genuinely empty. Reuses the alignment engine. Devanagari has
   strict stroke order and no app measuring it.
2. **Health measurement.** RevenueCat's best-monetising category (D14 RPI $0.48,
   download-to-trial 6.9%, both highest) and three 2025 winners were sensor-health
   apps. The ~55 health apps in the 2026 field are trackers, not measurers.

---

## 11. Known unverified — do not quote as fact

1. Exact NIPUN Bharat Lakshya WPM figure. Sources conflict (45–60 wpm vs PARAKH's
   operational 30–35 cwpm). Primary PDF returned 403 on every mirror.
2. Whether NIPUN numeracy Lakshyas contain any latency target (my read: no).
3. All Google Play install counts. Play detail pages do not render for automated
   fetching; every aggregator refused connections.
4. Google Read Along's install base and offline capability — **the most important
   competitor for candidate A, completely unverified.**
5. Amira's total funding: five aggregators give five figures ($20M–$43.6M).
6. Kanji Kentei FY2024 examinee count not confirmed against the Foundation's own
   data page (404).
7. iFlytek automated handwriting/exam-marking deployment scale — capability
   confirmed, scale not found. Highest-value remaining lead for candidate B.
8. Squirrel AI has **never** disclosed revenue; its own user claims are internally
   inconsistent (24M in TIME 2025 vs 52M in TIME 2026).
9. DIBELS / Acadience usage numbers — not obtainable.
10. Vosk / sherpa-onnx on-device Indian-English accuracy — not yet checked.

---

## Sources

RevenueCat: [how-to-win pt1](https://revenuecat.com/blog/engineering/how-to-win-shipaton-part-1-coming-up-with-an-idea) ·
[pt4](https://www.revenuecat.com/blog/engineering/how-to-win-shipaton-part-4-pitching-your-app) ·
[how we judge](https://www.shipaton.com/blog/how-we-judge-shipaton) ·
[benchmarks 2026](https://www.revenuecat.com/blog/growth/subscription-app-trends-benchmarks-2026) ·
[education breakout](https://www.revenuecat.com/state-of-subscription-apps-2026-education) ·
[2025 winners](https://www.revenuecat.com/blog/company/shipaton-2025-winners) ·
[2024 winners](https://www.revenuecat.com/blog/company/2024-ship-a-ton-winners) ·
[rules](https://revenuecat-shipaton-2026.devpost.com/rules) ·
[gallery](https://revenuecat-shipaton-2026.devpost.com/project-gallery)

Evidence base: [ASER 2024](https://asercentre.org/wp-content/uploads/2022/12/ASER-2024-National-findings.pdf) ·
[PARAKH FLS](https://parakh.ncert.gov.in/foundation-learning-study) ·
[Interspeech 2025 ORF](https://www.isca-archive.org/interspeech_2025/vidal25_interspeech.pdf) ·
[Svarah Indian-accent ASR](https://arxiv.org/abs/2305.15760) ·
[Bamberg legibility thesis](https://www.uni-bamberg.de/fileadmin/xai/studies/theses/2025/Master_Thesis_Aaron_Lukas_Pieger___Deep_Learning_based_Legibility_Evaluation_using_Images_of_Children_s_Handwriting.pdf) ·
[iFlytek handwriting rubric](https://edu.iflytek.com/solution/school/calligraphy-practicing) ·
[Wadhwani AI ORF](https://www.wadhwaniai.org/programs/oral-reading-fluency/) ·
[Speak $100M ARR](https://readthesignal.co/p/speak-hit-100m-arr-the-ai-that-answers) ·
[Korea AIDT collapse](https://minssam.com/en/blog/korea-aidt-crisis-2025/)

---

# FINALISTS — four A-grade candidates (2026-09-07)

Four scans complete: 9 conditions (motion/general), 10 camera-based, 15 audio/motion.
34 conditions assessed. Four cleared. Everything else is recorded below as killed, with
the reason, so we do not circle back to it.

## Comparison

| | BPPV / Epley | Capillary refill | Pupillary light reflex | Tinnitus matching |
|---|---|---|---|---|
| Grade | A | A | A− | A− |
| Sensor | Gyroscope | Rear camera + torch | Front camera | Audio out + touch |
| Unit measured | degrees, seconds | **milliseconds** | mm, ms, % | Hz, dB SL, seconds |
| Deterministic | yes | yes | yes | yes (output) |
| **Can claim ACCURACY?** | yes, vs protractor | **NO — no gold standard exists** | yes, vs printed mm discs | **yes — inject a known tone** |
| Self-generate pathology | office-chair nystagmus | **ice water lengthens CRT** | stimulus-intensity sweep | synthetic tinnitus in other ear |
| Incumbent | $60 hardware; Android apps are video only | **none shipped** (only a trainer sim) | Brightlamp Reflex **iOS only $399/yr** | naive sliders only |
| Consumer pays? | $9.99/mo iOS app exists | unproven | clinics $399/yr | **proven: £10/mo, €320/yr** |
| Prevalence | 2.4% lifetime | a sign, not a condition | n/a | **740M adults, 120M severe** |
| Demo strength | good (angle dial) | **best** (curve stretches after ice) | most cinematic (slow-mo pupil) | moderate (a converging graph) |
| Peace Prize fit | good | **best** (paediatric dehydration, WHO/IMCI) | moderate (TBI) | moderate (quality of life) |
| Biggest risk | stroke differential needs hard triage gate | "a stopwatch with extra steps" | Android can't use rear torch + see own eye | subjective input; notched-music therapy failed an RCT |

## Key discriminator: accuracy vs repeatability

CRT is the only finalist with **no external referent**. There is no lab value that *is*
capillary refill time — the literature's reference standard is the median of blinded human
observers, the same observers whose inter-rater ICC runs as low as 0.12.

Mitigation: validate exactly the way the published trials do (blinded frame-by-frame
annotation of our own recorded video), plus publish a coefficient of variation across 30
self-trials and the cold/warm dose–response curve. That is honest and defensible. It is
NOT an accuracy claim and must never be written as one.

Tinnitus is the opposite: you can inject a known 6,300 Hz tone as synthetic tinnitus in one
ear, run the matching staircase in the other, and check convergence against the value you
chose. Real accuracy, sweepable across the whole frequency/level grid, zero patients.

## Killed, with reasons — do not revisit

**Failed "can a healthy dev generate ground truth alone":**
anemia from conjunctival pallor (needs venous Hb; also Sanguina's Ruby ships it),
neonatal jaundice (needs serum bilirubin + newborns; Picterus and BiliCam exist),
oral cancer screening (needs biopsy; **MeMoSA is already live on Google Play in India**),
cataract, cerebral palsy infant movement (best clinical evidence of any candidate — 95%
sens / 97% spec — but needs infants), spasticity/clonus (cannot self-induce),
cough/TB (a healthy cough is a healthy cough).

**Free giant already there:**
visual acuity (Peek Acuity — free, NGO, validated in India 2025),
hearing screening (WHO hearWHO — free, >85% sens and spec, 200k+ users),
respiratory rate (Google Fit shipped it, MAE 0.78 br/min; **RRate** free on Play, updated
Aug 2025), leukocoria (Baylor CRADLE, free, *Science Advances*),
MUAC (**a UNICEF tape costs $0.06** and POSHAN Tracker is nationwide).

**Measurement itself is unreliable:**
spirometry from the mic (PEF r = **−0.257**), dysphagia via cervical auscultation
(sens 23–95%), 6MWT via GPS (95% LoA −77.6 to +103.9 m, exceeds the 100 m allowable),
cough/TB (cough-only AUROC 0.69–0.74, below WHO triage targets).

**Crowded or commoditised:**
scoliosis (Scoliometer in 75+ countries), essential tremor (StudyMyTremor used in 30+
studies; Tremor Test and Steady Hands on Play), Parkinson's (mPower, cloudUPDRS, Roche),
joint range of motion (clinometer already ICC 0.98 vs goniometer).

**Killed by Android hardware:**
diabetic neuropathy vibration threshold — was 2nd place until Android haptics killed it.
Amplitude is a 0–255 integer that many devices round to 100%, and LRA actuators couple
amplitude to frequency. Cross-device absolute thresholds are not defensible.

**Wrong shape:**
reaction time (excellent method — accelerometer impact peak recovers true tap to ~4 ms vs
the touchscreen's 8–10 ms floor — but it is a technique looking for a disease),
grip strength (novel, but ±2.6–4.4 kg error straddles the 28 kg / 18 kg sarcopenia cutoffs),
stuttering (only self-generatable ground truth is *acted* stuttering — circular, and on a
demo video likely to read as mockery in a Peace Prize track),
wound area (enterprise buyers, dull demo), OSA (needs an overnight recording).
