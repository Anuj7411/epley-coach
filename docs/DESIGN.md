# Design research and decisions — 2026-09-19

What the screens must do for the people actually using them, what competitors ship, and what is
still missing. Written before building the home flow, so the flow follows the evidence rather
than a template.

---

## Who is holding the phone

- Median age **51**, trial means **56–61**, a tail of 70+ who need a helper (docs/RESEARCH.md §2–3)
- Dizzy, possibly nauseous, lying down, **eyes often shut**
- Doing this during an attack, not at leisure

Design follows from that, not from a Dribbble shot of a fitness app.

## Rules (with sources)

| Rule | Source | Status in the app |
|---|---|---|
| Touch targets at least **48 dp**; bigger is better for declining motor control | [Material accessibility](https://m2.material.io/design/usability/accessibility.html), [Android touch target](https://support.google.com/accessibility/android/answer/7101858?hl=en) | Answer buttons 60–64 dp. Some secondary buttons are default size — **raise to 56 dp** |
| Avoid small type; let users scale text | [NN/g, usability for older adults](https://www.nngroup.com/articles/usability-for-senior-citizens/) | All text is in `sp`, so system font scaling works. Labels at 11–12 sp — **raise user-facing text to 14 sp minimum** |
| High contrast; no light-grey text | NN/g, same | `#666` on black is about 3.7 : 1, below WCAG AA for small text — **raise faint text to `#9E9E9E` or brighter** |
| Plain, forgiving errors; tolerate mistakes | NN/g, same | Messages are plain. **No way to undo a mis-tapped answer** in the questionnaire — a mis-tap on "which way is worse" picks the wrong ear. **Add "change my last answer"** |
| One instruction at a time | Dizziness; established in the run screen | Done |
| Never rely on the screen alone | Position four is face-down | Voice + vibration done |

## Competitors checked today

| App | What it does | Sensors during the manoeuvre? | Identifies ear / canal? |
|---|---|---|---|
| [Vertigo BPPV Vestibular Coach](https://apps.apple.com/us/app/vertigo-bppv-vestibular-coach/id6764793454) | Animated Epley, Brandt-Daroff tracker, **episode diary**, balance sway test, AI coach. $9.99/month, $49.99/year. Released April–May 2026, no ratings yet | **No** — sensors only for a balance test | **No** |
| [Epley Assist](https://apps.apple.com/us/app/epley-assist-dizziness-relief/id6448395635) | Animations, suitability survey, education. Free, 4.9 from 15 ratings | No | No |
| Vertigo Help, EpleyManeuver | Audio / visual step guides, countdowns | No | No |

**Still unique to us:** measuring the head during the manoeuvre, the validated canal triage, and
refusing the Epley for the horizontal canal.

**They have, we lack:** an episode diary (step 4) and animated pose illustrations.

**Pricing benchmark:** $9.99 / month, $49.99 / year exists in the category.

Pinterest was not usable — it requires login and JavaScript — so these findings come from
published guidance and the apps themselves rather than mockup galleries.

## Missing features and issues, in priority order

1. **Episode log and after-care** — competitor parity, and the gap the 2026 IMU authors named
2. **Change-my-last-answer** in the questionnaire — a mis-tap can pick the wrong ear
3. **A real home screen** — the current first screen is the engineering probe
4. **Stop gesture** (spec FR-8) — someone face-down cannot find a STOP button
5. **Text size and contrast** fixes above
6. **Helper mode** — instructions addressed to the person holding the phone
7. **Animated poses** — nice to have; the static pictures already work

## After-care content (verified)

- **No restrictions afterwards.** AAO-HNS 2017 makes a *strong recommendation against*
  post-procedure postural restrictions — no need to sleep upright or avoid lying flat.
  [AAO-HNS summary](https://www.entnet.org/resource/aao-hnsf-updated-cpg-bppv-press-release-fact-sheet/)
- **Still dizzy: repeat once, an hour later.** The JAMA Neurology 2023 trial protocol had patients
  repeat the manoeuvre one hour later. [PMC10011937](https://pmc.ncbi.nlm.nih.gov/articles/PMC10011937/)
- **Worse, or still dizzy after the repeat: see a doctor.**
