# Kickoff prompt for Claude Code

Paste everything below the line into Claude Code, from the root of the Android project (or an empty folder to start one). Keep `design_handoff_epley_coach_v2/` next to the app.

---

You are implementing **Epley Coach**, an Android app (Kotlin, Jetpack Compose, Material 3), so that it matches the design **pixel for pixel on every phone size**. The design lives in `design_handoff_epley_coach_v2/`. Work in the order below and do not skip the verification loop.

## 0. Read
1. Read `design_handoff_epley_coach_v2/README.md` completely, starting with **★ Pixel-parity protocol**. It is the single source of truth.
2. Look at `screenshots/412x915/` (device references) and `screenshots/390x844/` (mockups).
3. The `.dc.html` files are design references. Do **not** port HTML/JS into the app — except the 3D figure renderer (step 4), which must be used as supplied.

## 1. Generate references for the target devices
```
cd design_handoff_epley_coach_v2/tools
npm i && npx playwright install chromium
npm run refs                     # all devices in screens.json
```
This writes `reference/<device>/<day|night>/<screen>.png` and a `<screen>.json` layout spec for every screen state. Use the JSON for exact positions, sizes, colours, fonts, radii, icons and text. If the user's phone is not in `screens.json`, add it (width/height in dp and density) and re-run.

## 2. Foundations (README §3, §12, P1, P4)
- `DesignFrame` root wrapper from **P1** (density × s, s = clamp(W/390, 0.85, 1.25)). Every screen uses the literal design numbers inside it. Use `Modifier.weight(1f)` only on the flex regions listed in P1.
- Tokens exactly as in §3 / §12. No dynamic colour, no tonal elevation, no ripples, no default shadows.
- Fonts: Bricolage Grotesque **variable** and Material Symbols Rounded **variable** (opsz 24, wght 500). `includeFontPadding = false`, `LineHeightStyle(Center, Trim.None)`, letter-spacing in em, `tnum` on live numbers.
- Copy `logo-kit/android/` into `app/src/main/res/`.

## 3. Screens (README §4, §4a, §4b, §8, §11)
Build the components in §8, then every screen for day and night, all five positions and both ears. Clinical copy is **verbatim**. Never add postural restrictions. The ear only comes from triage (Practice mode is the only side picker).

## 4. 3D figure (README §7)
Do not model or approximate the figure. Put `figure/` in `assets/`, vendor `three@0.184.0` (`build/three.module.js`, `build/three.core.js`) into `assets/figure/vendor/` and change the two `https://unpkg.com/three@0.184.0/build/three.module.js` imports to `./vendor/three.module.js`. Show it with a transparent `WebView` loading `figure-view.html#step=&ear=&theme=&playing=` through `WebViewAssetLoader`; drive it with `evaluateJavascript("setFigure({...})")`. Wrap it in `FigureView(step, ear, theme, playing, sizeDp)`. Sizes: Welcome 208 · Find 248 · Hold 144 · Result 136 (design-dp, scaled by P1).

## 5. Motion (README §6)
Implement M1–M14 and reduced motion exactly (durations, easings, stagger). No spinners, spirals, rotating icons, horizontal slides, parallax or overshoot.

## 6. Verification loop — mandatory
1. Add Roborazzi screenshot tests: one per `screens.json` entry × theme × device, animations off, figure in held frame (`playing=false`), sample live values from §4b. Output `<theme>/<id>.png`.
2. `node tools/compare.mjs --device <device> --actual <roborazzi output>/<device>`
3. Open every red diff image, fix the cause (size, spacing, colour, font, icon, radius, flex), re-run. Repeat until **every screen on every device passes (≤ 0.5 %)**.
4. Report the final table. Only then hand over to the user.

Ask before changing any clinical copy, colour token, size or motion value. If something in the design seems wrong, say so — do not "improve" it silently.
