# Handoff: Epley Coach (Android)

> **Start here (Claude Code):** read this README top to bottom, look through `screenshots/`, then open `Epley Coach Prototype.dc.html` to feel the flow and motion. Build in the order in §13 and tick the acceptance checks there. `CLAUDE_CODE_PROMPT.md` has a ready-to-paste kickoff prompt.

**About the design files:** everything in this folder is a **design reference built in HTML/JS**, not production code. Recreate it natively in **Kotlin + Jetpack Compose (Material 3)**, following the target codebase's patterns. Only the Android resources in `logo-kit/android/` and the 3D model `figure/models/human_head.glb` are meant to be used as-is.

**Fidelity: high.** Colours, type, spacing, radii, copy and motion values are final. Clinical copy must be used verbatim.

---

## ★ Pixel-parity protocol (read first)

The goal is a native app that matches the design **on every phone size**, not only at 390 × 844. Four rules make that mechanical instead of guesswork.

### P1. One responsive rule for every screen
The design is authored on a **390 dp wide frame**. On a device of width W dp and height H dp:
- `s = clamp(W / 390, 0.85, 1.25)` — **every** size in this spec (type, spacing, radii, icons, borders, button heights, figure sizes) is multiplied by `s`.
- The design frame height is `H / s` design-dp. The extra height (or shortfall) goes **only** into the regions marked **flex** below. Nothing else stretches.
- In Compose this is one wrapper at the root — no per-screen maths:
```kotlin
@Composable fun DesignFrame(content: @Composable () -> Unit) {
  val cfg = LocalConfiguration.current; val d = LocalDensity.current
  val s = (cfg.screenWidthDp / 390f).coerceIn(0.85f, 1.25f)
  CompositionLocalProvider(LocalDensity provides Density(d.density * s, d.fontScale)) {   // 1 design-dp = s real dp
    Box(Modifier.fillMaxSize()) { content() }                                               // maxHeight is now H / s design-dp
  }
}
```
  Then write every screen with the literal numbers from this README (e.g. `44.sp`, `24.dp`) — the wrapper does the scaling. Use `Modifier.weight(1f)` exactly where a screen lists a flex region.
- **Flex regions per screen** (the only elements that grow): Splash — centre block · Welcome — spacer above the notice · **Home — the lilac Start card (min 232)** · Safety — spacer above the buttons · Emergency — title block · Questions — spacer above "Change my last answer" · Ear result — lilac card · Can't treat / Not BPPV — main card · Find — spacer above Stop · Hold — spacer between "Stay still." and the count (night: inside the mint card) · Done — spacer above Finish · Your runs — spacer above Share · Practice — spacer above Start practice.
- Top 48 design-dp is the status-bar zone and bottom 32 design-dp the gesture zone (draw edge-to-edge; the zones are at least as tall as the system insets on all target phones).
- System font scale: references are captured at fontScale 1.0. At larger font scales text may wrap; cards grow (never clip).

### P2. References exist for any device — generate them, don't eyeball
`tools/render-references.mjs` renders the design itself (the `.dc.html` files) with Playwright at any device size and density and writes:
- `reference/<device>/<day|night>/<screen>.png` — exact device-pixel reference, no bezel, fake status/gesture bars hidden, 3D figure in its held frame;
- `reference/<device>/<day|night>/<screen>.json` — **layout spec**: every painted element with its box (x, y, w, h in design-dp), background, ring/border, radius, text, icon name, font family/size/weight/line-height/letter-spacing, and image source.
Devices and the 30 screen states are listed in `tools/screens.json` (add the user's phone there). Pre-rendered copies for **412 × 915** (6.5" class) are in `screenshots/412x915/`.

### P3. Screenshot tests + diff until it passes
- Add **Roborazzi** (or Paparazzi) screenshot tests: one test per entry in `screens.json`, per theme, per device (`@Config(qualifiers = "w412dp-h915dp-xxhdpi")` etc.), with the fake sensor values from §4b, animations disabled and the figure's held frame. Name outputs `<theme>/<id>.png`.
- Run `node tools/compare.mjs --device pixel-412x915 --actual <roborazzi output dir>`. It writes red diff images and fails any screen above **0.5 %** differing pixels.
- Fix, re-run, repeat until every screen passes on every device in `screens.json`. Do not ask the user to judge by eye before this is green.
- Native text rasterisation differs slightly from Chrome; 0.5 % absorbs anti-aliasing. Anything above that is a real layout, colour, size or font error.

### P4. Same assets, same numbers
- **Fonts:** Bricolage Grotesque variable (Google Fonts; axes opsz 12–96, wght) and Material Symbols Rounded variable (opsz 24, wght 500, FILL 0/1, GRAD 0). Use the variable files, not static cuts. Compose: `includeFontPadding = false`, `LineHeightStyle(Alignment.Center, Trim.None)`, letter-spacing in **em** exactly as listed, `fontFeatureSettings = "tnum"` on live numbers.
- **Colours:** the hex values in §3 only. No Material dynamic colour, no tonal elevation, no ripples (`indication = null`), no default shadows.
- **Icons:** Material Symbols Rounded by the exact names in the layout spec JSON (`icon` field).
- **3D figure:** do **not** rebuild or approximate it. Use the supplied renderer (§7): `figure/figure-view.html` in a transparent WebView, or pre-rendered frames produced from it. Same code → same pixels.
- **Logo/icon:** `logo-kit/` files as-is.

---

## 0. Files in this folder

| File | What it is |
|---|---|
| `screenshots/412x915/` | **Device references for a 6.5" phone (412 × 915 dp) at 2×**, no bezel: all 15 screens day + night. |
| `screenshots/390x844/` | 45 mockups (with bezel) at the 390 × 844 design size: every screen, questions 2–6, all five positions, left-ear variants. Index in §15. |
| `EpleyScreen.dc.html` | **Source of truth for every screen.** One component; the `screen` prop picks the screen (15 day + 15 night), `pos` 1–5, `ear` R/L, `tq={n}` for a question. |
| `Screens.dc.html` | Every screen side by side (the page the screenshots were taken from). |
| `Epley Coach Prototype.dc.html` | Clickable prototype of the full run: real question order and branching, all five positions, live hold countdown, Day/Night and Reduced-motion switches. |
| `App Icon.dc.html` | App icon sheet: masks, themed icon, sizes, construction, system splash. |
| `figure/` | 3D instruction figure: `epley-figure-app.js` (renderer used by the screens), `epley-figure-v8-base.js`, `epley-figure-v4.js`, `models/human_head.glb` (CC BY 4.0). |
| `logo-kit/` | Wordmarks (`svg/epley-wordmark-ink.svg`, `-bone.svg`), app icon SVG/PNG, and drop-in Android resources in `logo-kit/android/`. |
| `tools/` | **Parity tools**: `render-references.mjs` (renders pixel references + layout-spec JSON for any device), `compare.mjs` (pixel diff vs Android screenshots), `screens.json` (devices and the 30 screen states). |
| `figure/figure-view.html` | Host page for the 3D figure in an Android WebView (transparent). |
| `CLAUDE_CODE_PROMPT.md` | Kickoff prompt for Claude Code. |
| `Screenshot Rig.dc.html` | Renders one screen at any device size from URL params (`?screen=home-night&w=412&h=915&pos=4&ear=R&q=1&bare=1&still=1`). Used by the tools. |
| `support.js` | Runtime for the `.dc.html` files. Not production code. |

Open the `.dc.html` files through a local server: `npx serve design_handoff_epley_coach_v2`.

---

## 1. Product and clinical rules (non-negotiable)

Epley Coach guides a person with BPPV through the Epley manoeuvre at home. The phone rests on the affected-side cheekbone and reads head orientation. Users: median age ~51, often older, dizzy, lying down, often at night, many with limited English.

1. **Not a medical device.** Home and Welcome must show: "NOT A MEDICAL DEVICE — An unregulated prototype. It must never be used on a patient." Splash shows "NOT A MEDICAL DEVICE".
2. **Safety check first, every run, cannot be skipped.** Six red flags: Weakness or numbness · Trouble speaking · Double vision · Can't walk · Sudden severe headache · Sudden hearing loss. "Yes, one or more" → Emergency stop (hard stop, no way forward, one action: **Call emergency**). "No, none of these" → Triage.
3. **The ear comes from triage, never from the user.** Questionnaire from Kim HJ et al., Neurology 2020 (accuracy 71.2%), used in the JAMA Neurology 2023 trial (resolution 72.4% vs 42.9% for self-chosen side). Persistent note on every question and on the ear result: "These questions agree with a specialist about 71% of the time. They are a guide, not a diagnosis." Credit on the ear result: "Questions from Kim HJ et al., Neurology 2020." Full copy and branching in §4a.
4. **An L/R picker exists only in Practice mode** ("Which side to rehearse?"). Nothing is measured there.
5. **Hold times:** position 1 = 3 s check, positions 2–4 = **45 s**, position 5 = 5 s.
6. **After-care (AAO-HNS 2017, no postural restrictions).** Exactly:
   - No restrictions afterwards. You can lie flat and sleep normally.
   - Still dizzy? Repeat once, an hour later.
   - Worse, or still dizzy after the repeat? See a doctor.
7. **No spirals, swirls or rotating-circle motifs anywhere in the app** (they read as rotation to users with vertigo). This includes: loading spinners, `CircularProgressIndicator`, rotating arrows, and the spiral brand symbol. Loaders are straight lines.
8. **Every state has four signals:** word + icon + shape + colour. Never colour alone.
9. **Stop** is on every run screen: full width, 64 dp, bottom.

---

## 2. Flow

```
Splash (≤1.4 s) → Welcome (first run only) → Home
Home ─ Start treatment → Safety check ─ Yes → Emergency stop (terminal)
                                      └ No  → Q1 → Q2 → Q3 → Q4 → Q5 ─┬ (Q4 = lying down)  ───────────→ result
                                                                        └ (Q4 = turning) → Q6 ─────────→ result
   result = any "No" on Q1–3 → "This doesn't sound like BPPV" (stop)
          else Q4 = turning       → "This app can't treat this type" (stop, no side)
          else                     → Ear result (side from Q5) → Find position ⇄ Hold → … ×5 → Done → Home
Home ─ Practise in hand → Practice mode (L/R) → Home
Home ─ (last run row / menu) → Your runs → Home
Stop on any run screen → Home (run logged "stopped at N")
Question screens have no Back circle. Every question after the first, and both stop results, carry **Change my last answer** (removes the last answer and returns to that question). Practice/History have a Back circle → Home. Safety and Emergency have no Back.
```
Find → Hold is automatic when both angles are in range. Hold → next position's Find is automatic when the count reaches 0 (the prototype demonstrates position 4 only, then jumps to Done).

---

## 3. Tokens

### 3.1 Colour — Day
| Token | Hex | Job |
|---|---|---|
| ground | `#EFEDE8` | screen background (bone) |
| surface | `#FFFFFF` | cards, list groups, icon circles |
| ink | `#17161C` | text, primary button fill, icons (18.1:1 on white) |
| muted | `#55535E` | secondary text (6.4:1 on ground) |
| line | `#D4D2DC` | outlines, empty progress, note border |
| divider | `#EFEDE8` | 1 dp row dividers inside white cards |
| lilac | `#C9BCFF` | primary/choice (home start card, result card, splash ground) — ink 10.3:1 |
| butter | `#FFE38A` | finding position — ink 14.2:1 |
| mint | `#A8EBD6` | holding, done, after-care — ink 13.2:1 |
| coral | `#FFC4B8` | stop, warnings, red flag — ink 11.8:1 |
| translucent white | `rgba(255,255,255,.6)` / `.55` | icon tiles and chips sitting on pastel fills |

### 3.2 Colour — Night (auto after 21:00 or when system is dark)
Rule: **no white or pastel floods.** Pastels become a 2 dp inset outline over a dark tint; only 64 dp action buttons are filled.
| Token | Hex | Job |
|---|---|---|
| ground | `#111015` | screen |
| surface | `#1D1C23` | cards |
| surface-2 | `#2A2931` | icon tiles, dividers, meter track |
| line | `#34333C` | outlines, empty progress |
| ink | `#F1F0F5` | text (17:1 on ground) |
| muted | `#A9A7B3` | secondary text (8:1) |
| soft | `#D8D6E0` | body text inside tinted cards |
| lilac tint / outline | `#221E30` / `#C9BCFF` | choice cards; primary button fill is `#C9BCFF` with ink `#17161C` |
| butter tint / outline | `#2B2718` / `#FFE38A` | finding |
| mint tint / outline | `#1B2B26` / `#8FDCC4` | holding; dim mint `#3F6B5E` for empty hold blocks; range zone `#2E5C4F` |
| coral tint / outline | `#33211D` / `#F5AE9F` | stop / red flag; Stop button fill `#F5AE9F` |

### 3.3 Type — Bricolage Grotesque (Google Fonts, variable, opsz 12–96)
| Role | Size / line | Weight | Tracking |
|---|---|---|---|
| Count (hold) | 232 sp day / 200 night, line 0.72 | 800 | −0.075em, tabular |
| Display | 44 / 44 | 800 | −0.035em |
| Emergency title | 64 day / 56 night, line 0.95 | 800 | −0.045em |
| Result headline / big numbers | 56, line 0.9–0.95 | 800 | −0.045em, tabular |
| Title | 34 / 1.02–1.05 | 800 | −0.03em, `text-wrap: balance` |
| Card title | 20–24 | 800 | −0.01 to −0.02em |
| Action (buttons) | 20 | 700 | 0 |
| Body | 17 / 1.35–1.45 | 500–600 | 0 |
| Label | 16 | 600 | 0 |
| Caps caption | 14 | 700–800 | +0.06em (NOT A MEDICAL DEVICE only) |
All live numbers use tabular figures. Minimum readable size 16 sp (14 sp only for caps captions and case-study labels). Icons: **Material Symbols Rounded**, weight 500, 24 dp (20 in chips, 28 in buttons/tiles), FILL 1 for check/selected states.

### 3.4 Space, shape, targets
- 4 dp base: 4 · 8 · 12 · 16 · 24 · 32 · 48. Between cards 8. Inside cards 24 (16 for rows). Text blocks outside cards inset 8 from the card edge.
- Radii: icon tile 16 · note/pill-card 24 · card 28 · hero card 32 · phone-like pills 999 · buttons 32 (64 tall).
- Targets ≥ 48 dp; primary actions 64 dp full width in the bottom third; icon buttons 48 dp circles.

---

## 4. Screens (see `EpleyScreen.dc.html`, prop `screen`)
Each exists as `name` (day) and `name-night`.

1. **splash** — Lilac ground (night: `#111015`). Centred Rail wordmark 208 dp wide + "coach" 20/700. Bottom: 120 × 4 dp loading line + "NOT A MEDICAL DEVICE". Shown only while the app loads after the system splash.
2. **welcome** (first run) — Wordmark lockup (28 dp + "coach"); white card with the figure (208 dp, position 1, plays once) and "The Epley manoeuvre, one position at a time."; three rows (A safety check before every run · Six questions find the ear · Your phone checks each angle); the not-a-medical-device line; **Get started**.
3. **home** — Header row: date (16/600 muted) + settings (48 dp). "Ready when you are." (night: "Take it slow. Sit up first."). Lilac Start treatment card (232 dp min, chip "Safety check first", "5 positions · about 6 min", 64 dp arrow). Two tiles: Practise in hand (→ Practice), Check sensors. Coral NOT A MEDICAL DEVICE card. **No logo on Home** (space goes to content).
4. **safety** — chip "Safety check" + "Before every run"; "Do you have any of these right now?"; "This step can't be skipped."; the six flags in one white card (52 dp rows); **Yes, one or more** (coral) above **No, none of these** (ink).
5. **emergency** — Whole screen coral (night: coral-outlined tint card). "Red flag" chip, "Stop. Get emergency help now.", "Do not start the treatment.", **Call emergency** (dials local emergency number). Nothing else.
6. **triage** (one screen per question, see §4a) — 6-segment progress (done = ink fill, current = 2 dp ring, todo = line) + "Step 2 of 6 · question N of 6"; question 34/800; Q1 only: sub-text; answers; spacer; "Change my last answer" row (64 dp, from Q2 on); note card at the bottom.
   - **Yes/No (Q1–3):** two tiles side by side, 168 dp min, check / close icon, 56 sp word.
   - **Two-option (Q4–6):** two full-width tiles stacked, 104 dp **min** height (grow with text), radio outline 28 dp, label 22/800 line 1.15, `text-wrap: balance`, must fit two-line labels at 160% font scale. Q5 adds a trailing 48 dp arrow circle (→ right, ← left) as a non-verbal cue.
7. **result** (a, ear) — Lilac card: chip "Your result", headline **"Right ear" / "Left ear" at 96 sp / 800, line 0.86, on two lines ("Right" / "ear")**, body "Posterior canal, the type the Epley manoeuvre treats. We will set up for your right side."; "First move" card (figure 136 dp held frame, position 1, mirrored for left) + "Turn your head 45° toward your right ear"; note; **Start treatment**; credit 14/600 muted "Questions from Kim HJ et al., Neurology 2020."
7b. **horizontal** (b) — Surface card with 2 dp ink inset ring (night: bone ring): chip "Not treated here" (`block` icon), headline 44/800 "This app can't treat this type", body "Your answers point to the horizontal canal. The Epley manoeuvre does not treat it, so this app will not guide one. Ask your doctor to show you the right manoeuvre." **No side shown.** Then Change my last answer, note, **Back to home**.
7c. **notbppv** (c) — Hard-stop family: whole screen coral day (night: coral tint card + outline). Chip "Stop" (filled warning), headline 56/800 (night 48) "This doesn't sound like BPPV", body 19/600 "Don't do the manoeuvre now. See a doctor. Get emergency help straight away if you also have weakness or numbness, trouble speaking or seeing, a sudden severe headache, or you can't walk steadily." Then Change my last answer (translucent white on coral), **Call emergency** (outlined 2 dp ink), **Back to home** (ink).
8. **find** (prop `pos` 1–5, `ear`) — 5-segment progress + "Position N of 5" + outlined chip "Finding position"; butter card with the position's instruction and sub-line; white card: figure 248 dp (plays the move once, **Still/Play** pill bottom-right), then live row: big remaining angle (signed, "In range" when reached), direction line, "now X° · aim A–B°", 12 dp meter (mint range zone on the position's scale, 4 × 24 dp ink marker); **Stop**. Content per position in §4b.
9. **hold** (prop `pos`, `ear`) — Whole screen mint (night: mint-outlined tint card). Filled chip "Holding", "Stay still.", count (seconds remaining), "seconds left of T", blocks filling left→right (**5 s blocks for 45 s holds = 9 blocks; 1 s blocks for the 3 s and 5 s holds**); translucent card with the figure held frame (144 dp) + Turn / Tip readings with checks; **Stop**. Label "Position N of 5 · <short>".
10. **done** — Lilac card (min 128 dp) with a 64 dp check tile + "All five positions held."; list of 5 positions with hold times (3 s, 45 s, 45 s, 45 s, 5 s) and filled checks; mint after-care card (exact copy §1.6); **Finish**.
11. **history** — "Your runs", grouped by month, rows with mint check / coral cross tile, "Right ear · all 5 held" / "stopped at 2", duration; lilac "Share with your doctor" (PDF of angles and hold times).
12. **side** (Practice mode) — "Which side to rehearse?", "Nothing is measured…", L / R tiles (selected = lilac + 3 dp ink inset ring + filled radio + word "Selected"), "Not sure which ear? Take the 6 questions" (→ Triage), **Start practice**.

---

## 4a. Triage questions (copy is final — use exactly)
| # | Question | Answers (index 0 / 1) | Asked |
|---|---|---|---|
| 1 | Does it feel like the room, or you, is spinning or whirling? | Yes / No | always |
| 2 | Do you get dizzy mainly when you move your head? | Yes / No | always |
| 3 | Does each spell of dizziness last less than 3 minutes? | Yes / No | always |
| 4 | Which brings the dizziness on more? | Lying down, or getting out of bed / Turning my head or body while lying down | always |
| 5 | Which way makes it worse? | Turning my head to the right / Turning my head to the left | always |
| 6 | When turning your head brings it on, how long does it last? | Less than 1 minute / More than 1 minute | only if Q4 = turning |

Q1 sub-text: "For people a doctor has already diagnosed with BPPV. BPPV often comes back in a different ear, so this is asked every time."
Q1–3 are **all** asked even after a "No" (no early exit), so the stop result can list every reason. The label stays "question N of 6" even when Q6 is skipped. Reference: `QS` in `EpleyScreen.dc.html`; branching in `answer()` / `order()` in `Epley Coach Prototype.dc.html`.

---

## 4b. The five positions (right ear; a LEFT ear swaps every right/left word and mirrors the figure)
| Pos | Instruction (title / sub) | Direction line | Hold label | Metric · scale · aim (illustrative) | Hold | Figure step |
|---|---|---|---|---|---|---|
| 1 | Turn your head toward your right ear / Stay sitting up. Turn about 45°. | Keep turning right | head turned right | Turn · 0–90° · 30–60° | 3 s check | 1 (crown) |
| 2 | Lie back / Keep your head turned. Let it hang just past the pillow. | Tip your head back | lying back | Tip · −90…+90° · −15 to −35° | 45 s | 2 (profile) |
| 3 | Turn your head to the left / Slowly, about 90°. Keep it hanging back. | Keep turning left | head turned left | Turn · 0–180° · 75–105° | 45 s | 3 (crown) |
| 4 | Roll onto your left side / Keep your head turned, nose to the bed. | Keep rolling left | left side | Turn · 0–180° · 120–150° | 45 s | 4 (crown) |
| 5 | Sit up slowly / Face forward. Keep your chin level. | Lift your chin a little | sitting up | Tip · −90…+90° · −10 to +10° | 5 s | 5 (profile) |

The **target ranges and scales are placeholders for the layout**; use the app's existing thresholds. Remaining angle = distance to the nearest edge of the range, "+" below it, "−" above it. The Done list mirrors too ("Turn head left / Lie back / Turn head right / Roll onto right side / Sit up" for a left ear). Reference: `POS` and `mirror()` in `EpleyScreen.dc.html`; simulation per position in `sim()` of the prototype, which now runs all five positions in sequence.

---

## 5. Logo and app icon
- In-app mark = **Rail wordmark** ("epley" on its level line) + "coach" set in Bricolage 700. Ink on day, bone on night. Used on **Splash and Welcome only**, not on Home or run screens.
- **App icon (v2, no spiral):** the first "e" of the Rail wordmark, cut from the same path, with its tail running into the rail. Lilac `#C9BCFF` background, ink `#17161C` glyph. See `App Icon.dc.html`.
- Ready-to-drop Android resources in `logo-kit/android/`:
  - `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` — adaptive icon (background colour + foreground + **monochrome** for Android 13 themed icons).
  - `drawable/ic_launcher_foreground.xml`, `drawable/ic_launcher_monochrome.xml` — 108 dp VectorDrawables, glyph 50 dp wide inside the 66 dp safe zone.
  - `drawable/ic_splash.xml`, `drawable-night/ic_splash.xml` — 288 dp splash icon, inside the 192 dp circle. Static.
  - `values/ic_launcher_background.xml`, `values-night/…` — `ic_launcher_background` = lilac; `splash_background` = lilac (day) / `#111015` (night).
  - `values/themes_splash.xml` — `Theme.Epley.Starting` (core-splashscreen). Call `installSplashScreen()` before `setContent`, keep it on while the figure model loads, then hand over to the in-app Splash (M6).
- Play Store icon: `logo-kit/png/app-icon-512.png` (full bleed; Play applies the mask). Favicons: `favicon.svg`, `favicon-32.png`, `favicon-16.png`.
- Retired: all spiral files (`epley-symbol-*`, `epley-monogram-e-*`). Do not ship them.
- Credit (CC BY 4.0) must appear in Settings › About: "Head model: “Human head” by ADAMA on Sketchfab, CC BY 4.0."

---

## 6. Motion spec
Principle: premium = **fast, quiet and consistent**, not showy. For vestibular users motion must never imply the world moving: **no horizontal slides, no parallax, no zoom transitions, no spinners, no springs with overshoot, no looping decoration.** Only opacity, ≤ 8 dp vertical rise, ≤ 3% scale, and colour.

### 6.1 Easing tokens (Material 3)
| Token | Curve | Compose |
|---|---|---|
| `emphasizedDecelerate` | cubic-bezier(0.05, 0.7, 0.1, 1) | `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)` |
| `standard` | cubic-bezier(0.2, 0, 0, 1) | `CubicBezierEasing(0.2f, 0f, 0f, 1f)` |
| `linear` | — | `LinearEasing` (timers only) |

### 6.2 Where motion happens
| # | Where | What | Duration / easing |
|---|---|---|---|
| M1 | **Every screen change** | New screen replaces the old one instantly; its content children fade 0→1 and rise 8 dp→0, staggered 24 ms per child (max 5 steps). Status and gesture bars never animate. | 280 ms, emphasizedDecelerate |
| M2 | **Background change between screens** (bone→mint on Hold, bone→coral on Emergency, etc.) | Background colour cross-fades. | 360 ms, standard |
| M3 | Triage question → next question | Same as M1 (same screen, new content). Progress segments step, they don't slide. | 280 ms |
| M4 | **Press** on any button, card, tile, back circle | Scale to 0.97 while pressed, back on release. No ripple flood (use `indication = null` + scale). | 160 ms, standard |
| M5 | Chip / toggle selection (Still/Play, L/R, Day/Night) | Background + text colour cross-fade. | 200 ms, standard |
| M6 | **Splash** | Wordmark revealed left→right along its rail (clip), loading line scales X 0→1 from the left. Exit = M1 into Welcome/Home. Total ≤ 1.4 s. | 700 ms emphasizedDecelerate / 1000 ms standard |
| M7 | **Find: live marker** | Marker follows the sensor. Sensor readings low-pass filtered, UI updated at 10 Hz; marker position animates to each new value. Remaining-angle numeral swaps instantly (no rolling digits). | 240 ms, standard |
| M8 | **Find → Hold** (both angles in range for 0.9 s) | M2 (butter→mint) + M1, plus haptic `CONFIRM`, plus spoken "Hold still". | 360 ms |
| M9 | **Hold blocks** | Each 5 s block fills left→right continuously. Count numeral steps once per second, no tween. | linear, 1000 ms per second |
| M10 | Leaving range during Hold | Back to Find (M2 mint→butter), long haptic `REJECT`, spoken "Move back…". Count pauses. | 360 ms |
| M11 | Hold complete | 2 haptic ticks, spoken next instruction, then M1 to the next Find. | — |
| M12 | **3D figure** | Plays **once** per screen entry: fade in 0.35 s → hold start 1.0 s → move 2.4 s (sine in-out, no overshoot) → hold still 4.6 s → fade 0.35 s, then loops the same single move. Held-frame screens (Hold, Result) never move. Still/Play pill pauses it. | `T` in `figure/epley-figure-app.js` |
| M13 | Emergency | M2 to coral + M1 with no stagger beyond 24 ms. No pulsing, no flashing. | 360 / 280 ms |
| M14 | Done | M1 only. Nothing loops, no confetti. | 280 ms |

### 6.3 Reduced motion
If `Settings.Global.ANIMATOR_DURATION_SCALE == 0` or the user enables "Remove animations": every M1/M2/M3/M8/M13 becomes a 150 ms linear cross-fade of the whole content, M4 scale is off, M6 shows the final frame, M12 shows the held frame only (no move). Timers (M9) keep working without tweening. The prototype's "Motion: Reduced" switch shows this.

### 6.4 Haptics & voice
Enter range: `HapticFeedbackConstants.CONFIRM` (API 30+, fallback short 20 ms). Hold complete: two short. Leave range: `REJECT` / long 80 ms. Every instruction is spoken with TextToSpeech in the device language.

---

## 7. 3D instruction figure (in-app integration)
Geometry, poses, angles, camera and timings are fixed (pose table in §4b; timings in M12). For the app it is rendered:
- **Transparent ground.** The shoulders fade by alpha (shirt shader) and a radial mask dissolves everything except the head and phone toward the frame edge, so there is no crop line on any surface. Overlays are drawn last and are never faded.
- **Themed** (`THEMES` in `epley-figure-app.js`):
  - Day: skin `#C9BCFF`, shirt `#9A89E0`, phone ink `#17161C` with mint screen `#A8EBD6`, contour ink, target line dashed ink with white halo, reference line `#55535E`, label pills ink with white Bricolage 800, start-pose ghost ink ring at 30%.
  - Night: same skin/shirt, phone `#F1F0F5`, target dashed butter `#FFE38A` with dark halo, reference `#A9A7B3`, butter pills with ink text, ghost lilac ring 55%.
- **Minimum legibility:** line widths and label size are defined in CSS px × pixel ratio (labels ≥ 14 dp, target line ≥ 1.8 dp), so the 152–168 dp held frames stay readable. Label pills are clamped 4 dp inside the frame.
- **Sizes:** Welcome 208 · Find 248 · Hold 144 · Result 152. Always square.
- **One renderer, many views:** `getFigure()` builds the model once per process; `attach(canvas, {ear, step, view, playing, theme})` per view; nothing re-renders while the figure is still.
- **On device — use the supplied renderer, never an approximation.** Option A (live, exact): a transparent `WebView` (`setBackgroundColor(Color.TRANSPARENT)`, hardware accelerated, JS on) loading `figure/figure-view.html#step=4&ear=R&theme=light&playing=1` from app assets via `WebViewAssetLoader`; update with `evaluateJavascript("setFigure({...})")`. Vendor `three@0.184.0` (`build/three.module.js` + `build/three.core.js`) into `figure/vendor/` and point the three imports there so it works offline. Option B (static, exact): **pre-render** 5 steps × 2 ears × 2 themes = 20 clips + 20 held frames with alpha (WebM VP9 alpha or PNG sequence). The camera is fixed, so this is exact.
- Accessibility: every figure has a content description (see `aria-label`s in the source).

---

## 8. Components (build once, reuse)
- **Card** (surface, radius 28/32, padding 24). Night variant: tint + 2 dp inset outline.
- **Chip** (radius 999, 16/700, 20 dp icon, padding 6/12/6/8): outlined = finding/choice, filled ink = holding, translucent white on pastels.
- **Primary button** 64 dp, radius 32, 20/700, optional 28 dp leading icon. Day ink/white; night lilac/ink. **Stop** = coral fill + ink text + `close`.
- **Icon circle** 48 dp surface, 24 dp icon (back, settings).
- **Row** min 48–72 dp, 48 dp icon tile radius 16, title 17/700, subtitle 16/600 muted, trailing chevron.
- **Progress segments** 8 dp tall, radius 4, gap 4: done = ink fill, current = 2 dp ink outline, todo = line colour.
- **Hold blocks** 16 dp tall, 9 × 5 s, radius 4, outline 2 dp, fill grows linearly.
- **Range meter** 12 dp track radius 6, zone fill, 4 × 24 dp marker.
- **Note card** 1.5 dp line outline, radius 24, `info` icon muted, 16/600.
- **Warning card** coral (day) / coral tint + outline (night), `warning` icon tile, caps caption + body.

---

## 9. Accessibility checklist
- All text ≥ 4.5:1; ink on pastels ≥ 10:1; night body 17:1, muted 8:1.
- Targets ≥ 48 dp; primary 64 dp in the bottom third.
- State never by colour alone (§1.8). Selected tiles add a 3 dp ring + "Selected".
- TalkBack: figure descriptions; count announced every 5 s (not every second); triage progress "Question n of 6".
- Font scale to 160%: text wraps, the count scales down to fit width, buttons grow in height.
- Reduced motion honoured (§6.3).

---

## 10. Known open items
1. Settings › About screen (credit, version, not-a-medical-device statement) is not drawn yet.
2. Trademark search for "Epley Coach" before registering.

---

## 11. State and navigation model
Single-activity Compose app, `NavHost` with **no platform transitions** (`EnterTransition.None` / `ExitTransition.None` everywhere); motion M1–M3 is applied inside each screen's content (see §6).

```kotlin
enum class Ear { R, L }
sealed interface Route { Splash; Welcome; Home; Safety; Emergency; Question(n: Int); EarResult; Horizontal; NotBppv; Find(pos: Int); Hold(pos: Int); Done; History; Practice; About }

data class TriageState(val answers: List<Int> = emptyList()) {        // 0 = Yes / first option, 1 = No / second option
  val order get() = buildList { addAll(1..5); if (answers.getOrNull(3) == 1) add(6) }
  val next get() = order.getOrNull(answers.size)                        // null = finished
  val outcome get() = when {
    answers.take(3).any { it == 1 } -> Outcome.NotBppv                   // precedence 1
    answers.getOrNull(3) == 1       -> Outcome.Horizontal                // precedence 2 (no side)
    else -> Outcome.Ear(if (answers.getOrNull(4) == 1) Ear.L else Ear.R)
  }
  fun undo() = copy(answers = answers.dropLast(1))                      // "Change my last answer"
}

data class RunState(val ear: Ear, val pos: Int = 1, val phase: Phase = Phase.Finding, val value: Float = 0f, val remaining: Int = 0, val log: List<PosLog> = emptyList())
enum class Phase { Finding, Holding }
```
- **Find → Hold:** both angles in range continuously for 0.9 s. **Hold → Find (same pos):** leaves range; count pauses, resumes on re-entry (does not restart). **Hold complete:** pos < 5 → Find(pos + 1); pos = 5 → Done.
- **Stop** on any run screen → Home; the run is logged as "stopped at N".
- **Theme:** night when `isSystemInDarkTheme()` **or** local time ≥ 21:00 or < 06:00. Evaluated on screen entry, never mid-run.
- **Welcome** shows only on first launch (persist a flag). **Safety** is shown before every run, no skip, no persistence of answers.
- **Persist:** run history (date, ear, positions held, stopped-at, per-position angles and hold times) for "Your runs" and the doctor PDF. Do not persist triage answers.

## 12. Compose starter (tokens)
```kotlin
object Day   { val ground = Color(0xFFEFEDE8); val surface = Color(0xFFFFFFFF); val ink = Color(0xFF17161C); val muted = Color(0xFF55535E); val line = Color(0xFFD4D2DC)
               val lilac = Color(0xFFC9BCFF); val butter = Color(0xFFFFE38A); val mint = Color(0xFFA8EBD6); val coral = Color(0xFFFFC4B8) }
object Night { val ground = Color(0xFF111015); val surface = Color(0xFF1D1C23); val surface2 = Color(0xFF2A2931); val line = Color(0xFF34333C); val ink = Color(0xFFF1F0F5); val muted = Color(0xFFA9A7B3); val soft = Color(0xFFD8D6E0)
               val lilacTint = Color(0xFF221E30); val lilac = Color(0xFFC9BCFF); val butterTint = Color(0xFF2B2718); val butter = Color(0xFFFFE38A)
               val mintTint = Color(0xFF1B2B26); val mint = Color(0xFF8FDCC4); val mintDim = Color(0xFF3F6B5E); val zone = Color(0xFF2E5C4F); val coralTint = Color(0xFF33211D); val coral = Color(0xFFF5AE9F) }
val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f); val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
object Motion { const val enter = 280; const val stagger = 24; const val bg = 360; const val press = 160; const val toggle = 200; const val marker = 240; const val reduced = 150 }
object Space  { val xs = 4.dp; val s = 8.dp; val m = 12.dp; val l = 16.dp; val xl = 24.dp; val xxl = 32.dp; val xxxl = 48.dp }
object Radius { val tile = 16.dp; val note = 24.dp; val card = 28.dp; val hero = 32.dp; val button = 32.dp }
// Font: Bricolage Grotesque variable (res/font), weights 500/600/700/800; set fontFeatureSettings = "tnum" on every live number.
```
Press effect (M4): `Modifier.graphicsLayer { scaleX = s; scaleY = s }` with `s` animated to 0.97 while `interactionSource.collectIsPressedAsState()`, `tween(160, easing = Standard)`, `indication = null`.
Enter (M1): per child `alpha 0→1`, `translationY 8.dp→0`, `tween(280, delay = min(i,5)*24, Emphasized)`.

## 13. Build order and acceptance checks
1. Tokens, fonts, icons, theme switch (§3, §12). ✔ Night after 21:00 even with system light.
2. Components (§8). ✔ All targets ≥ 48 dp; buttons 64 dp.
3. Splash + system splash + app icon (§5). ✔ No spiral anywhere; no spinner.
4. Home, Welcome, Practice, History.
5. Safety → Emergency. ✔ No Back, no skip; Call emergency dials.
6. Triage (§4a, §11). ✔ Q1–3 always asked; Q6 only after Q4 = turning; precedence NotBppv > Horizontal > Ear; undo works from Q2 and both stop results; label "question N of 6".
7. Results a/b/c (§4). ✔ Horizontal shows no side; ear headline ≥ 96 sp.
8. Sensor + Find/Hold for all five positions and both ears (§4b, §11). ✔ Left ear mirrors every side word and the figure; hold times 3/45/45/45/5 s; blocks 1 s for short holds, 5 s otherwise.
9. 3D figure (§7). ✔ Transparent on every surface, no crop line, labels ≥ 14 dp, held frames never move.
10. Done + after-care (copy §1.6 verbatim). ✔ No "stay upright", no "sleep propped up".
11. Motion (§6) + reduced motion (§6.3) + haptics/voice (§6.4).
12. Accessibility pass (§9) at 160% font scale and TalkBack.

## 14. Edge, error and empty states
- **Sensor unavailable / not calibrated:** do not start Find. Show a coral warning card "Your phone's motion sensor isn't responding. Restart the app, or practise in hand." with **Back to home**. (Copy is a placeholder — confirm.)
- **Signal lost mid-hold:** treat as leaving range (M10); after 5 s without data, pause the run with the same warning card and **Stop**.
- **Phone call / app backgrounded during a run:** pause; on return resume at the same position in Finding.
- **History empty:** "No runs yet." 17/600 muted inside a white card, no illustration.
- **Emergency call fails / no SIM:** keep the screen; show "Call your local emergency number now." under the button.
- **Figure fails to load:** hide the figure area; instructions and live readings still work (the figure is never required to proceed).

---

## 15. Screenshot index (`screenshots/`, 2×)
Files are in `screenshots/390x844/`. Figure frames on some Find mockups are captured mid-move; the `412x915` set and the tool output use the held frame. Live numbers are sample values.

| File | Screen |
|---|---|
| `01-splash-day.png` | Splash · day |
| `02-welcome-day.png` | Welcome · day |
| `03-home-day.png` | Home · day |
| `04-safety-day.png` | Safety check · day |
| `05-emergency-day.png` | Emergency stop · day |
| `06-triage-day.png` | Question 1 (Yes/No) · day |
| `07-triage4-day.png` | Question 4 (two options) · day |
| `08-result-day.png` | Ear result · day |
| `09-horizontal-day.png` | Can’t treat this type · day |
| `10-notbppv-day.png` | Doesn’t sound like BPPV · day |
| `11-find-day.png` | Find position (pos 4) · day |
| `12-hold-day.png` | Hold (pos 4) · day |
| `13-done-day.png` | Done · day |
| `14-history-day.png` | Your runs · day |
| `15-side-day.png` | Practice mode · day |
| `16-splash-night.png` | Splash · night |
| `17-welcome-night.png` | Welcome · night |
| `18-home-night.png` | Home · night |
| `19-safety-night.png` | Safety check · night |
| `20-emergency-night.png` | Emergency stop · night |
| `21-triage-night.png` | Question 1 (Yes/No) · night |
| `22-triage4-night.png` | Question 4 (two options) · night |
| `23-result-night.png` | Ear result · night |
| `24-horizontal-night.png` | Can’t treat this type · night |
| `25-notbppv-night.png` | Doesn’t sound like BPPV · night |
| `26-find-night.png` | Find position (pos 4) · night |
| `27-hold-night.png` | Hold (pos 4) · night |
| `28-done-night.png` | Done · night |
| `29-history-night.png` | Your runs · night |
| `30-side-night.png` | Practice mode · night |
| `31-triage-q2-day.png` | Question 2 · day |
| `32-triage-q3-day.png` | Question 3 · day |
| `33-triage-q5-day.png` | Question 5 · day |
| `34-triage-q6-day.png` | Question 6 · day |
| `35-find-pos1-day.png` | Find · position 1 · day |
| `36-hold-pos1-day.png` | Hold · position 1 · day |
| `37-find-pos2-day.png` | Find · position 2 · day |
| `38-hold-pos2-day.png` | Hold · position 2 · day |
| `39-find-pos3-day.png` | Find · position 3 · day |
| `40-hold-pos3-day.png` | Hold · position 3 · day |
| `41-find-pos5-day.png` | Find · position 5 · day |
| `42-hold-pos5-day.png` | Hold · position 5 · day |
| `43-result-left-ear-day.png` | Ear result · left ear · day |
| `44-find-pos4-left-ear-day.png` | Find · position 4 · left ear · day |
| `45-done-left-ear-day.png` | Done · left ear · day |
