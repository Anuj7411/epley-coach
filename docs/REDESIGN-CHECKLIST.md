# Redesign checklist — Epley Coach v2

Source: `design_handoff_epley_coach_v2/` (Claude Design, 2026-10-01) and
`design_handoff_epley_figure/`. The handoff README is the source of truth for **look, copy and
motion**. The app's existing engine is the source of truth for **measurement**: thresholds,
calibration, polarity learning, dwell gate, sensor path.

Commit after every numbered step so the app is always submittable.

## Decisions that differ from the handoff (and why)

| Handoff | Kept instead | Why |
|---|---|---|
| No paywall screen | RevenueCat paywall kept, restyled, gating "Share with your doctor" | RevenueCat purchase is a hard Shipaton requirement |
| Ear result → Find directly | Mount choice, calibration and direction-learning kept, restyled | Without them the phone cannot measure the head (FR-3, FR-14, polarity) |
| §4b target ranges | App thresholds from `Maneuver.kt` (Kwon et al.) | Handoff says its ranges are placeholders |
| Material Symbols Rounded font | Material Icons **Rounded** (already in `material-icons-core`) + drawn glyphs where missing | Visually near-identical; avoids a ~10 MB icon font |

## Build order (handoff §13) — tick with the check that proves it

- [ ] 1. Tokens (day/night), Bricolage Grotesque, icons, theme switch — night after 21:00 even with system light
- [ ] 2. Components: Card, Chip, PrimaryButton, Stop, IconCircle, Row, ProgressSegments, HoldBlocks, RangeMeter, NoteCard, WarningCard, press scale M4, enter M1 — targets ≥ 48 dp, buttons 64 dp
- [ ] 3. Logo resources, system splash, in-app splash (M6) — no spiral, no spinner
- [ ] 4. Welcome (first run only), Home, Practice (side), History
- [ ] 5. Safety → Emergency — no Back, no skip, Call emergency dials
- [ ] 6. Triage — Q1–3 always, Q6 only after Q4 = turning, precedence NotBppv > Horizontal > Ear, undo from Q2 and both stop results, "question N of 6"
- [ ] 7. Results a/b/c — horizontal shows no side; ear headline ≥ 96 sp
- [ ] 7b. Setup screens kept from the app, restyled: mount, calibrate, direction, ready
- [ ] 8. Find / Hold, all five positions, both ears — left mirrors words and figure; holds 3/45/45/45/5; 1 s blocks for short holds, 5 s otherwise
- [ ] 9. 3D figure — pre-rendered held frames + moves, transparent, labels ≥ 14 dp, held frames never move
- [ ] 10. Done + after-care — AAO-HNS copy verbatim, no "stay upright", no "sleep propped up"
- [ ] 10b. Paywall restyled in the new system; purchase still completes through the Test Store
- [ ] 11. Motion M1–M14, reduced motion, haptics, voice
- [ ] 12. Accessibility at 160% font scale; TalkBack labels

## Must survive the redesign (regression list)

- [ ] Safety screener blocks every run
- [ ] Triage decides the ear; L/R only in Practice
- [ ] NOT A MEDICAL DEVICE on Home, Welcome, Splash
- [ ] Hold timer only starts when in range **and** still
- [ ] Volume-key abort still works on run screens
- [ ] Mount-moved warning still surfaces during a run
- [ ] Export gated by entitlement; Test Store purchase completes
- [ ] 187 unit tests still pass
- [ ] Head model credit in About: "Head model: “Human head” by ADAMA on Sketchfab, CC BY 4.0."
