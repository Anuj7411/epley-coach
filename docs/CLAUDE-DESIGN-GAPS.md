Design these screens for Epley Coach in the existing v2 system (design_handoff_epley_coach_v2). They are live in the app but were never in the handoff, so I built them from the existing components. Please design each one properly.

Rules. They are the same as the rest of the handoff:
- 390 × 844 design frame. Every screen needs day and night versions. Mark the flex region the same way the README's ★P1 does.
- Use only the existing tokens, Bricolage Grotesque, Material Symbols Rounded, and the §8 components.
- No spirals, spinners, slides, parallax or overshoot.
- The copy below is what ships today. Keep the clinical copy word for word. You may tighten the rest.
- 3D figure: use only the supplied renderer (figure/). Never draw a new figure.

---

## A. Treatment setup: steps 3–6 of 6

The handoff's run starts at Find, but a real phone first has to be told where the head is. These four steps sit between the ear result ("Start treatment") and Find. Header: back circle, the caption "Step N of 6", and 6 progress segments. This is the same header as the questions.

1. **How will you hold the phone?** (Step 3 of 6)
   Sub-text: "It has to move with your head, so it goes against the side of your face."
   There are two option tiles. Tapping one goes to the next step.
   - "Against your cheek" / "Needs nothing. Flat on your cheekbone, screen facing out, top of the phone toward the top of your head."
   - "In a headband or cap" / "Most accurate, and both hands stay free. Tuck it against the side of your head."
   Note: "Either way, the voice guides you. You can keep your eyes shut."

2. **Calibrate** (Step 4 of 6)
   Title: "Sit up straight and look ahead". Practice title: "Hold the phone upright".
   Card: the 3D figure, 208 dp, held frame of **position 5's end pose** (seated, facing forward, head level, phone on the cheek; mirror it for a left ear). Put the mount instruction under it. Practice shows no figure.
   Note: "Tap while you can still see the screen. The app counts you down out loud, then captures the position once you are still."
   Button: "Start, then get into position"
   States:
   - **Countdown** replaces the button: a big number 3…2…1 with "Get into position", then "Hold still" / "Capturing when you stop moving".
   - **Failed** (a warning card): "No sensor reading yet. Wait a second and try again." or "The phone is lying too flat to tell which way you're facing. Hold it on its edge, as described, and try again."

3. **Turn to learn direction** (Step 5 of 6)
   Title: "Turn your head to your right". Practice title: "Turn the phone to your right". The side comes from triage.
   Sub-text: "About halfway to your shoulder, and hold it there."
   Card: the 3D figure, 208 dp, position 1, playing, mirrored for left.
   Note: "This teaches the app which direction is which. It can't work that out on its own."
   Button: "Start, then turn to my right"
   States: the same countdown and failure states as Calibrate.

4. **Ready** (Step 6 of 6)
   Flex hero card with the chip "All set", the word "Ready" and "The voice will guide you through five positions. You can keep your eyes shut."
   Confirmed list, each line with a check: "Right ear · posterior canal", "Phone against your cheek" (or "Phone in a headband"), "Direction learned".
   Notes: "Sit on the bed so that when you lie back, your head can hang over the end." and "Hold either volume button for a moment to stop. A quick press still changes the volume."
   Button: "Start"

## B. Practice mode

The flow is side picker (§4.12) → Calibrate → Direction → Ready → Find/Hold ×5 → Done. The caption reads "Practice · step N of 4". Please design:
- The picker's **nothing-selected state**. It opens with no side chosen, on purpose, so "Start practice" needs a disabled look.
- A **practice banner or treatment** on Find/Hold, so nobody mistakes a rehearsal for treatment. Practice sub-text is "The phone stands in for your head."
- **Practice Done:** "All five positions held." plus the list, **without** the after-care card, because nothing was treated.

## C. Safety check: question 2 and its stop

The app asks a second question because it carries the trial's exclusion criteria. Please design it as the second screen of the safety check.
- **Question 2**, captioned "One more": "Do any of these apply to you?" / "This step can't be skipped."
  Rows (these wrap to two lines):
  - "A doctor has never diagnosed you with BPPV"
  - "A neck or back problem that makes moving your head hard"
  - "A recent head injury"
  - "This attack feels different from the ones you were diagnosed with"
  Buttons: "Yes, one applies" (coral) / "No, none apply".
- **See a doctor** (answered yes). This is a stop in the same family as "Can't treat". Chip "See a doctor" (stethoscope). Headline "See a doctor before using this". Body "The head positions aren't safe to do on your own in this situation. A doctor can check what's causing the dizziness and show you what to do." Button "Back to home".

## D. Settings and About (the Home gear goes here)
- Title "Settings".
- Rows: "Your runs" / "Every run, and the PDF for your doctor", and "Check sensors" / "See the angles the app measures".
- **About** card:
  - Wordmark lockup and "Guides the Epley manoeuvre for posterior canal BPPV, one position at a time, measuring each head angle with the phone's sensor."
  - Credits: "Triage questions — Kim HJ et al., Neurology 2020." / "After-care — AAO-HNS Clinical Practice Guideline: BPPV, 2017." / "Head model — "Human head" by ADAMA on Sketchfab, CC BY 4.0." (required by the licence) / "Type — Bricolage Grotesque and Material Symbols."
- The NOT A MEDICAL DEVICE card.

## E. Check sensors (the Home tile "Check sensors" opens this)
- Title "What the phone sees" / "Hold it the way you would for a run, calibrate, then move. These are the angles the app guides by."
- **Sensor card:** "Orientation sensor", its rate in °/s, and a Still/Moving chip.
- **Mount choice** (radio list with the tolerance of each): Against your cheek ±7°, In a headband or cap ±5°, In your hand (practice) ±10°. Practice shows the note "Practice only. This reads the phone, not your head."
- **Live readings:** two big tiles, "Tip" and "Turn" (°, or "--" before calibrating).
- **Warnings:** "TURN UNRELIABLE — swung N° from where it was calibrated" and "THE PHONE MOVED — it turned faster than a neck can".
- **Buttons:** "Calibrate" / "Calibrate again", then "Back upright — check drift" with the result note "Drift: last 1.2°, worst 2.0° over 3 checks. Tolerance ±7°."
- **Rows:** "Accuracy self-check", "Record a log" / "Stop recording", and "Raw readings", which expands a small monospace block.

## F. Accuracy self-check
- Title "How accurate is this phone?" / "Put it flat on any surface — it doesn't need to be level. Take a reading, spin the phone 180° on the spot, and take another."
- Live card: a big "+0.42°" with Still/Moving, then Reading 1 and Reading 2.
- States:
  - After reading 1: "Now spin the phone 180° flat on the surface, like turning a plate…"
  - Warnings: "TURNED OVER" / "NOT TURNED".
  - Result card, **Passed** (mint) or **Outside tolerance** (coral), showing sensor error and surface tilt.
- Buttons: "Take reading 1", "Take reading 2", "Start again", "Correct this phone by −0.84°" (disabled while the phone is moving).

## G. Paywall: the RevenueCat purchase (important for the hackathon)
The design's "Share with your doctor" card on Your runs leads here when the PDF isn't unlocked.
- Hero: "Take your history to your doctor" / "A PDF of every run: when, which ear, how long, and the angle and hold time of each position."
- "Free forever, with or without this": the safety check, the six questions, unlimited guided runs, practice mode and after-care advice, your run history on this phone, the sensor check.
- Note: "One payment. No subscription, no account, and nothing leaves your phone unless you send it." In test builds, add "Test build: the store is not connected…"
- Buttons: "Unlock the PDF · ₹…" (price from the store), "Restore", "Not now".
- Error states: "That didn't go through. Nothing was charged." and "No previous purchase found on this account."

## H. The doctor's PDF
This is the A4 document that gets shared, not a screen. It lists each run (date, ear, outcome, duration). Under each run, each held position shows its hold time and measured turn and tip. A closing paragraph states the method and that this is not a medical device. It is currently plain typography; please design it.

## I. Edge and empty states from README §14 that the handoff never drew
- **Sensor unavailable:** "This phone can't measure head angles" / "Your phone's motion sensor isn't responding… Restart the app, or try another phone."
- **Your runs empty:** "No runs yet."
- **Emergency call fails:** "Call your local emergency number now." under the button.
- **Signal lost mid-hold** (pause card with Stop), and **figure fails to load** (area hidden).
- **Find, the phone moved:** "THE PHONE MOVED — It turned faster than a head can. Recalibrate before trusting the reading."

## J. Design-file issues to fix
- Night Home: the Start card's ring is typed inside its `onClick` attribute, so it never renders. Decide whether the ring should be there.
- Your runs row data: the app shows real dates. Groups are "This week", then months, with "Tonight, HH:MM" / "Today, HH:MM" for today.
