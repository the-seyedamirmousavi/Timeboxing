# Timeboxing

An Android timeboxing app built around the way Elon Musk runs his day: the whole
day is cut into **5-minute blocks**, and every block gets a job. Nothing is left
to "whenever".

## How it works

- **Now**: a countdown ring for the box you should be working on right now,
  with its start, its end and what comes next. "Mark done" when it's finished.
  In unboxed time the ring counts down to your next box and offers "Box this slot".
- **Top 3**: the few outcomes that actually matter today. Keep it short.
- **Day Analysis**: hours boxed, number of 5-minute blocks, completion
  (weighted by duration) and boxes still open.
- **5-Minute Blocks**: the day as a grid, one row per hour and 12 cells per row,
  grouped into quarter hours. Tap a free cell to box it and tap a box to edit it.
  The current block is outlined in green.
- **Schedule**: the day as a table. Tap a row to edit it and long-press to mark it done.
- **One thing at a time**: boxes can't overlap. The editor rejects a clash and
  names the box it hits.
- Use the header arrows to plan tomorrow (or look back at yesterday). Tap the date
  to jump back to today.

Data stays on the device (SharedPreferences, one JSON entry per day).

## Theme

The palette is taken from the FitSho app:

| Token | Color | Use |
|---|---|---|
| `tb_background` | `#F7F7F7` | Screen background |
| `tb_surface` | `#FFFFFF` | Cards (16dp radius), header |
| `tb_ink` | `#000000` | Titles, values |
| `tb_text_secondary` | `#8A8A8A` | Labels, hints |
| `tb_button` | `#2B2B2B` | Pill buttons, boxed blocks, ring arc |
| `tb_field` | `#F4F4F4` | Inputs, ring track, free blocks |
| `tb_divider` | `#EEEEEE` | Card header dividers |
| `tb_accent` | `#22E022` | Done blocks, "now" marker |

## Build

Open the project in Android Studio, or run:

```bash
./gradlew assembleDebug testDebugUnitTest
```

Min SDK 24, target SDK 37. Written in Java with Android Views and Material Components.
