# Demo video — production script

Target **1:55**. Screeners watch the first two minutes and score on four things; every beat below
earns its place against one of them. Numbers are quoted from `docs/RESEARCH.md` and are all
sourced — nothing here is rounded for effect.

**Voice:** unhurried, low, certain. Launch-film pace, not explainer pace — roughly 150 words a
minute with real pauses. Never sell. State the fact and stop talking.

**Grammar** (from the two reference films): dark field, one coloured glow taking its colour from
the screen, flat UI as rim-lit planes in 3D, camera movement instead of cuts, kinetic type on
black for act breaks, logo sting to close.

**Palette:** ground `#121311`, glow `#C4B5FF`, type `#F3F1EA`, alert `#FF8F7A`, confirm `#7AD9E0`.

---

## Act 1 — The gap (0:00–0:24)

> The hook is not the app. It is the fact that a free cure exists and almost nobody receives it.
> Nothing appears on screen for the first six seconds except words.

| Time | Visual | Voiceover | On-screen |
|---|---|---|---|
| 0:00–0:07 | Black. Type fades up centred, one line at a time, ~0.4s apart. No motion otherwise. | "There is a cure for the most common cause of vertigo. It is free. It takes five minutes. It involves no medicine at all." | **A cure exists.** |
| 0:07–0:13 | Type clears. A single number scales up slowly from 98% to 100% — almost still. | "Eight percent of the people who need it ever receive it." | **8%** |
| 0:13–0:24 | Five faint position diagrams trace in sequence, then one tilts wrong and a red arc opens between it and true. | "It is five head positions, and it only works if the angles are right. Done at home from a video, those angles are wrong by forty to fifty degrees." | **40–51° off** |

---

## Act 2 — The app (0:24–1:12)

> First real footage lands at 0:32 and never stops for long after. This act carries *"your app in
> use"*, which for Next Gen is what validates the submission.

| Time | Visual | Voiceover | On-screen |
|---|---|---|---|
| 0:24–0:30 | Glow blooms lilac. The home screen flies in as a rim-lit plane, rotating to face-on. | "This is Epley Coach. It measures your head while you treat yourself." | **Epley Coach** |
| 0:30–0:41 | **Real capture.** Safety screener, then the tap, then the stop screen. Hold on it. Let it sit. | "Before anything else it looks for the signs that mean this is not vertigo at all. If it finds one, it stops. It does not let you continue." | **Positional vertigo can be a stroke** |
| 0:41–0:55 | Camera dollies to a second plane: the triage questions. Two figures rise as bars beside it. | "Vertigo comes back in the other ear. Treating a recurrence from last time's diagnosis resolves forty-three percent. Six questions from a five-hundred-and-eighty-five-patient trial resolve seventy-two." | **42.9% → 72.4%**<br>JAMA Neurology, 2023 |
| 0:55–1:12 | **Real capture, the centrepiece.** Phone against the cheek, live angle readout, the band, the timer refusing to start, then locking. Hold the moment it turns. | "Then it watches. The timer will not start until your head is in position — and still." | **In position** |

---

## Act 3 — What is free (1:12–1:34)

> This act is the RevenueCat criterion. It is not a feature list; it is a position. Say it as one.

| Time | Visual | Voiceover | On-screen |
|---|---|---|---|
| 1:12–1:21 | The paywall's free-list ticks in, one line at a time, in confirm cyan. | "Every angle it measures is free. The safety check is free. Every guided run is free, forever." | ✓ safety check ✓ the six questions ✓ unlimited runs |
| 1:21–1:29 | **Real capture.** Tap unlock, the Test Store dialog, the entitlement granting, the report opening. | "One payment unlocks one thing — your history, as plain text, for the doctor you will see three weeks from now, when nobody remembers the detail." | **$2.49 once. No subscription.** |
| 1:29–1:34 | Back to black. Type only. | "A paywall that lets someone proceed unguided is not a business model. It is harm." | **Nothing safety-relevant is ever paid** |

---

## Act 4 — The care, and the limit (1:34–1:55)

> The honest limit is not an apology. Delivered flat and unhurried it reads as the most confident
> thing in the film — and it is what the repo is scored on.

| Time | Visual | Voiceover | On-screen |
|---|---|---|---|
| 1:34–1:41 | A wall of test names rushes past at an angle, then settles on one number. | "The angle mathematics was proven before the phone was ever plugged in. A hundred and eighty-seven tests, not one of which needs a device." | **187 tests** |
| 1:41–1:50 | Three lines, appearing and clearing on black. Slowest beat in the film. | "The manoeuvre is recommended by clinical guideline. The questionnaire is validated by trial. That this app improves outcomes is not — and it does not claim to be." | **Claims angle accuracy. Claims nothing else.** |
| 1:50–1:55 | Glow collapses to the "e" monogram on ground. Hold. Repo URL fades beneath. | "Built by one student, on one laptop, with no developer account." | **Epley Coach**<br>github.com/Anuj7411/epley-coach |

---

## Where each judging criterion is earned

| Criterion | Beat |
|---|---|
| App idea clarity and usefulness | Act 1 entire — the gap is the idea |
| Meaningful progress toward a working app | 0:30–1:12, all real capture |
| Thoughtful RevenueCat use | Act 3 — stated as a position, shown as a purchase |
| Technical choices and care | 1:34–1:50 — the tests, and the limit |
| "Your app in use" (validation) | 0:30–0:41, 0:55–1:12, 1:21–1:29 |
| Under two minutes | 1:55 |

---

## Shot list — what still has to be filmed

Everything below marked **real capture** must be genuine screen recording, not animation. Three
of the four are already recorded; one needs a head.

| Shot | Status |
|---|---|
| Home screen, slow scroll | recorded |
| Safety screener → stop screen | can be recorded over adb |
| Triage questions | can be recorded over adb |
| Paywall → Test Store purchase → export | can be recorded over adb, after clearing the entitlement |
| **Phone at the cheek, live angle, timer locking** | **needs you, a bed and a second pair of hands** |

That last one is the centrepiece and the only thing a screen recorder cannot fake. Film it in
landscape, well lit, phone screen clearly legible, and let the timer actually refuse before it
starts — the refusal is the product.

---

## Voiceover, in full

Read straight through, no music under the first seven seconds.

> There is a cure for the most common cause of vertigo.
> It is free. It takes five minutes. It involves no medicine at all.
>
> Eight percent of the people who need it ever receive it.
>
> It is five head positions, and it only works if the angles are right.
> Done at home from a video, those angles are wrong by forty to fifty degrees.
>
> This is Epley Coach. It measures your head while you treat yourself.
>
> Before anything else it looks for the signs that mean this is not vertigo at all.
> If it finds one, it stops. It does not let you continue.
>
> Vertigo comes back in the other ear. Treating a recurrence from last time's diagnosis
> resolves forty-three percent. Six questions from a five-hundred-and-eighty-five-patient
> trial resolve seventy-two.
>
> Then it watches. The timer will not start until your head is in position — and still.
>
> Every angle it measures is free. The safety check is free. Every guided run is free, forever.
>
> One payment unlocks one thing — your history, as plain text, for the doctor you will see
> three weeks from now, when nobody remembers the detail.
>
> A paywall that lets someone proceed unguided is not a business model. It is harm.
>
> The angle mathematics was proven before the phone was ever plugged in.
> A hundred and eighty-seven tests, not one of which needs a device.
>
> The manoeuvre is recommended by clinical guideline. The questionnaire is validated by trial.
> That this app improves outcomes is not — and it does not claim to be.
>
> Built by one student, on one laptop, with no developer account.
>
> Epley Coach.

**229 words.** At launch pace with the pauses written in, 1:50–1:58.
