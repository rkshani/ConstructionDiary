# Construction Diary: Thekedar & Site Manager

An offline construction cost and project manager for house owners and thekedars (contractors).
Package: `com.diary.construction`.

It records every cost of a building project — material, labour, thekedar / sub-contractor
payments, suppliers and stages — and makes reports you can share as PDF or Excel.

## Features

- **First launch:** 3 intro screens, then "I am the Owner" / "I am a Thekedar" (labels and dashboard change by role).
- **Projects:** name, site address, owner/client + phone, start date, budget, area (marla / kanal / sq ft), status.
- **Project dashboard:** budget vs spent, spent by category (material / labour / contractor / other), pending dues, progress %, budget warning.
- **Stages:** foundation, structure, chhat, plaster, electric, plumbing, tiles, paint, finishing + custom; budget, status, dates, order.
- **Material:** eent, cement, sariya, ret, bajri + custom; quantity, unit, rate, total, supplier, stage, bill photo; stock received / used / remaining.
- **Supplier khata:** material taken, payments, running balance, next payment date, Call / WhatsApp.
- **Labour:** dihari, daily attendance (present / half / absent), advances, weekly settlement, payment history.
- **Contractor khata:** fixed or per sq ft agreement, work done %, payments, due now, remaining.
- **Other expenses:** transport, tools, naqsha, NOC, bijli / pani connection, chai-pani ... with stage and bill photo.
- **Site photos:** daily photos per stage on a date timeline.
- **Reports:** summary, stage-wise, material-wise, labour, supplier and contractor statements, expenses; date range filter;
  PDF (PdfDocument) shared through WhatsApp / share sheet (FileProvider); CSV / XLSX export through SAF.
- **Reminders (WorkManager):** labour weekly payment, supplier payment due, stage deadlines.
- **Estimator:** bricks / cement / sand for a wall, cement / sand / bajri / sariya for a roof slab.
- **Backup / restore:** one .zip with the database and photos, saved anywhere through SAF.
- **Settings:** English / Urdu (RTL), light / dark theme, currency sign, area unit, PIN + fingerprint app lock.

Fully offline: the app has **no INTERNET permission**.

## Tech

Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture (`core` / `data` / `domain` / `ui` / `di`),
Hilt, Room (KSP), Coroutines / Flow, Navigation Compose, DataStore, WorkManager. `minSdk 24`.

Money is stored as `Long` paisa, quantities / areas as `Long` with scale 100, dates as epoch millis.

## Build

Open the project in Android Studio and run the `app` configuration.

Release signing: copy `keystore.properties.example` to `keystore.properties`, put your keystore
next to it and fill in the values. Both files are ignored by git.

## Branches

Work was done one feature per branch (`feature/project-setup` ... `feature/polish-release`), each merged into `master`.
See `AGENTS.md` for the list.
