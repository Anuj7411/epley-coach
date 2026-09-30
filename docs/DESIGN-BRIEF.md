# Design brief — Epley Coach

For a designer picking this up cold. Everything below is sourced from `docs/DESIGN.md`,
`docs/RESEARCH.md` and `docs/SPEC.md`; where a rule has a clinical reason, the reason is given,
because several of them look arbitrary until you know why.

---

## 1. What it is, and who is holding the phone

An Android app that measures your head angle while you perform the Epley manoeuvre at home, and
**refuses to let you count a hold until you are actually in position.**

The person using it:

- **Median age 51.** Trial means 56–61. A tail of 70+ who need a helper.
- **Dizzy while using it.** Often nauseous. Frequently lying down.
- **Eyes often shut.** By position four they are face-down and cannot see the screen at all.
- Doing this **during an attack**, at 3am, not at leisure.

Design follows from that, not from a dribbble shot of a fitness app. Every decision below is
traceable to one of those four lines.

---

## 2. What "award-level" has to mean here

The target is the RevenueCat Design Award: *"the app that best represents the craft of app
development, separate from viability as a business — innovative ideas and/or beautiful app design
and animations."*

For this app, craft is **not** decoration. It is visible evidence that each choice was made for a
dizzy 51-year-old with their eyes closed. A judge should be able to look at any screen and see a
decision that a generic app would not have made.

The strongest card is counter-intuitive: **this app deliberately refuses animation.** Not from
inability — from clinical reason. Lead with that, don't hide it.

---

## 3. Non-negotiable constraints

| Rule | Why | Source |
|---|---|---|
| Touch targets ≥ 48dp; buttons ≥ 56dp tall | declining motor control | Material accessibility |
| Body text ≥ 16sp, labels ≥ 14sp, all in `sp` | system text scaling must work; **users run 115–160%** | NN/g older adults |
| Nothing fainter than `#B3B3B3` on the ground | `#666` on black is ~3.7:1, below WCAG AA | NN/g |
| Background is `#121311`, never `#000000` | off-white on pure black causes halation, worst with astigmatism | Material dark theme |
| Colour is never the only signal | every state also carries a word **and** a shape | Okabe–Ito |
| One question or instruction per screen | dizziness | Ada Health |
| Audio leads, the screen supports | position four is face-down | — |
| Sentence case, not capitals, for buttons and labels | — | — |

**It must survive 160% font scale.** This is not theoretical — the owner's own device runs at
115–160% and layouts have already broken there twice. Design at 160% first; if it works there it
works everywhere.

---

## 4. The motion rule — read this before proposing any animation

Current rule, verbatim from `docs/DESIGN.md`:

> **No animation** except the hold progress bar (essential feedback, exempt under WCAG 2.3.3).
> No transitions, no parallax, no pulsing.

The reason is not taste. **The users have a vestibular disorder.** Motion on screen can provoke
the symptom the app exists to treat. For this audience a drifting, pulsing or parallaxing
interface is not "premium" — it is a trigger.

### The line that actually matters

WCAG 2.3.3 exempts motion that is **essential to the function**. That gives a precise boundary,
and it is the boundary a designer must work inside:

| Allowed | Forbidden |
|---|---|
| Motion the **user themselves caused** — a figure that mirrors their real head, live from the sensor | Motion that **plays by itself** while the user is mid-attack |
| The hold progress bar filling | Decorative loops, ambient drift, pulsing, breathing, shimmer |
| A pose preview animating **while the user is upright, eyes open, before the run starts** | Anything animating **during a hold**, when they are lying down and symptomatic |
| Screen changes that are instant, or a plain crossfade under 150ms | Slides, parallax, spring overshoot, staggered cascades |

**Honour `prefers-reduced-motion` on everything**, including the exempt items, with a static
fallback that still communicates.

---

## 5. The 3D head figure — yes, but in one specific form

The request is a 3D rotating head/body during the manoeuvre. **This is a genuinely good idea and
it fits the essential-motion exemption — provided it is driven by the sensor, not by a timeline.**

There are already two flat visuals doing this job, and the 3D figure should replace them rather
than sit beside them:

- `PoseIllustration` — static drawing of the target pose, shown **before** each position while
  the user is still upright and can see the screen (requirement FR-19)
- `HeadDials` — live head-over-target figure during the hold: from above for the turn, in profile
  for the tilt, inside a shaded band as wide as that step's tolerance (requirement FR-20)

### What the 3D figure must do

| | |
|---|---|
| **Driven by** | the live sensor quaternion — it rotates *because the user's head rotated*, never on its own |
| **Shows** | current head orientation against the target pose, with the tolerance band as a visible volume in space |
| **In position** | the whole status card turns `#7AD9E0` and says so in words |
| **Out of position** | outlined `#FFC857` with the correction **in words** — "turn further to your left", never just an arrow |
| **Frame rate** | must not stutter; the sensor runs at 50Hz |
| **Reads at 160% text** | the figure must not be the thing that gets squeezed |

### What it must not do

- **No idle animation.** When the head is still, the figure is still. A head that drifts or
  breathes while the user holds a position for 45 seconds is exactly the wrong thing.
- **No auto-playing demo during a hold.** If you want an animated demonstration of the whole
  manoeuvre, it belongs on the pose-preview screen *before* the run, when the user is sitting up
  with their eyes open — never while they are lying back and dizzy.
- **No camera orbit.** The viewpoint is fixed. The head moves; the world does not.

### One caution on which half is shown

Practice mode measures **the phone, not a head**, and says so on screen (FR-18). If the 3D figure
is a head, practice mode must not show a head — it would be a lie about what is being measured.
Either show a phone in practice mode, or label the figure unmistakably.

---

## 5b. Showing rather than telling — the language problem

The app will be used by people who do not read or speak English comfortably. Spoken instructions
are the weakest possible channel for them, and this app leans on speech heavily. A wordless
visual demonstration is the right answer.

Two facts decide where it can actually go.

### The screen is against their face

By position two the phone is **flat against the cheekbone, screen facing out**. The user cannot
see it. By position four they are face-down with their eyes shut. **No animation shown during a
hold reaches the person doing the manoeuvre** — it is physically out of view.

So the demonstration has to happen **before** they move into each position, while they are still
upright and looking at the screen. That is the pose-preview moment the app already has (FR-19),
and it is exactly where a looping 3D demonstration belongs:

| Moment | Who can see the screen | What belongs there |
|---|---|---|
| **Pose preview**, before each position | the user, upright, eyes open | **the looping 3D demonstration** — where the phone goes, which way the head turns, how far |
| **During the hold** | nobody (user) / a helper, or the user in practice mode | sensor-driven figure, voice, haptics |
| **Practice mode** | the user, phone in hand | demonstration and live figure both work |

This also resolves the motion question cleanly: the demonstration loops while the user is upright
and asymptomatic, and stops before they lie down. It never plays at them while they are dizzy.

### What the demonstration must convey, without a single word

1. **Which cheek** the phone goes against, for the affected side — mirrored correctly
2. **Screen facing out**, not in
3. **Which way the head turns**, and roughly how far — 45° reads as "halfway to your shoulder"
4. **How far back the head tips**, and that it hangs off the edge
5. **That the position is then held still** — the figure settles and stops

A person who understands none of the audio should be able to copy the figure and be right.

### The voice has to be rewritten to match

Right now the spoken lines stand alone. If a figure is showing the movement, the words become a
second channel describing the *same* motion in the *same* order, so the two reinforce rather than
compete. Short, imperative, no numbers a dizzy person has to convert — "halfway to your shoulder"
rather than "forty-five degrees", which is already the rule in `Maneuver.kt`.

### A defect this exposes

`GuidanceOutput` calls `setLanguage(Locale.getDefault())` — it tries to speak in the device's
language. But **every string in the app is hardcoded English in Kotlin; there is no
`strings.xml`.** On a phone set to Hindi, the engine selects a Hindi voice and is then handed
English words, which it will pronounce as Hindi phonetics. The result is likely *less*
intelligible than plain English.

Two options, and they are not equivalent:

- **Short term:** force `Locale.US` for speech so English is at least pronounced as English, and
  let the 3D demonstration carry meaning for everyone else.
- **Properly:** extract all copy to `strings.xml`, translate, and let both the text and the voice
  follow the device locale. This is a real piece of work and it is not a design task.

Whoever designs the demonstration should assume **the visual may be the only channel the user
understands.** Design it to stand completely alone.

### None of this may cost accuracy

The measurement path is the product. Protect it:

- The demonstration renders **only on preview screens, while the engine is not measuring.** No 3D
  scene runs during a hold.
- During measurement the sensor arrives at **50Hz** and every hold is timed from sensor
  timestamps. A renderer competing for the main thread can drop samples, and dropped samples move
  the measured angle. Keep the in-run figure light — it is a readout, not a scene.
- Tolerance bands, dwell gating and the stillness threshold are clinical values from Kwon et al.
  **No visual change may alter them.** If a band looks too wide on screen, the band is right and
  the drawing is wrong.
- Nothing added here may delay the abort gesture or the safety screener.

---

## 6. Screens to design — all eleven

Current state is honest, not flattering.

| Screen | What it does | Current state |
|---|---|---|
| **Launch / splash** | logo on `#121311` | mark in lilac, static, ~1.3s (= cold start). Wants a considered reveal that is still motion-safe |
| **HOME** | the landing page | **recently redesigned** — five bento cards, hero action, side doors, warning, history. Use this as the design language for everything else |
| **SAFETY** | blocking red-flag screener, 2 steps + a hard-stop | functional, plain, lots of empty space |
| **TRIAGE** | six questions, one per screen, with a 71% caveat | question card + caveat by the buttons. Still sparse |
| **PRACTICE_SIDE** | which ear to rehearse | plain |
| **HOLD** | choose the mount: cheek / headband / in-hand | plain |
| **CALIBRATE** | capture an upright, still reference pose | plain; this is where trust is won or lost |
| **DIRECTION** | learn which way "toward the affected side" is | plain |
| **READY** | final confirmation before the run | plain |
| **RUN** | **the centrepiece** — live angle, tolerance band, dwell-gated timer, voice + haptics | functional; this is where the 3D figure goes |
| **AFTER-CARE** | what to do next, drift report, how you felt | plain |
| **PAYWALL** | one-time unlock for clinician export | **already strong** — leads with what stays free |
| **INSTRUMENT / ACCURACY** | engineering probe and reversal self-check | deliberately utilitarian; low priority |

Priority if time is short: **RUN**, then **CALIBRATE**, then **SAFETY**, then the rest.

---

## 7. Tokens — use these, do not invent

```
Ground        #121311   screen background (warm near-black, never #000000)
Surface       #1C1D1A   cards, list rows
Divider       #2A2B27   hairlines inside cards
Outline       #4A4B45   secondary button borders
Ink           #F3F1EA   primary text, primary button fill
InkBody       #D9D6CD   running paragraphs (softer, reduces glare)
InkMuted      #B9B6AC   secondary text
Seeking       #FFC857   searching for the position — always with an arrow + dashed outline
Holding       #7AD9E0   in position, done — always with a check + solid fill
Urgent        #FF8F7A   stop, emergency — always with a cross or warning icon
Selected      #C4B5FF   selection, links, the brand lilac
```

**Type:** Atkinson Hyperlegible throughout — chosen because it was designed for low vision, not
for looks. Display 34sp / Title 26sp / Subtitle 22sp / Body 17–18sp / Label 16sp / floor 16sp.

**Brand mark:** lowercase "e" monogram with a rail — the level line the name stands on. Already
implemented as `res/drawable/ic_brand_mark.xml` and used on the launcher icon, splash and home
screen. Keep it.

---

## 8. What to deliver

1. **Screen designs at 160% font scale**, 1080×2400, dark only — there is no light theme and
   there should not be one
2. **The 3D figure**: model, the two camera framings (above for turn, profile for tilt), the
   tolerance-band treatment, and the in/out-of-position states
3. **Motion spec** for anything that moves, with its WCAG 2.3.3 justification written down
4. **A `prefers-reduced-motion` fallback** for each
5. Exported assets as **vector where possible** — the app has no raster assets except the icon

---

## 9. Things that will get rejected

- Any light theme
- Pure black backgrounds
- Text below 16sp, or any layout that breaks at 160%
- Colour as the only carrier of state
- Green/red pairs — they fail for the most common colour blindness
- Decorative motion of any kind, however tasteful
- A spiral, swirl or rotating-circle motif. It is the vertigo cliché **and** it is visually
  rotation-inducing for exactly this audience
- Stock medical iconography — crosses, stethoscopes, ear cross-sections
- Anything implying diagnosis. The app guides a treatment someone was already told to do
