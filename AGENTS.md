# AGENTS.md — Construction Diary (for Codex)

**App:** Construction Diary: Thekedar & Site Manager
**Package:** `com.diary.construction`
**What it does:** An offline construction project manager for house owners and thekedars (contractors). It records all costs and expenses: material, labour, contractor payments, suppliers and stages, and makes reports you can share.
**Stack:** Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture, Hilt, Room (KSP), Coroutines/Flow, Navigation Compose, DataStore, WorkManager.

## Rules
1. **Only write code. Do NOT run Gradle or build the app** (no `./gradlew`, no assemble, no tests run). Write the Gradle files (version catalog, wrapper config, build scripts), but do not run them. The user will build in Android Studio.
2. **Every screen and every feature gets its own new branch.** For each item in the list below:
   - `git checkout main && git pull`
   - `git checkout -b feature/<name>` (branch names are given below)
   - write the code → `git add . && git commit -m "feat(<name>): <short summary>"`
   - `git push -u origin feature/<name>`
   - merge into `main` and push `main`, so the next branch starts from the latest code.
3. **If anything is unclear, ask the user first.** Use short, simple English and give options. Always ask before adding a library that is not listed here, and if the git remote is missing.
4. **Fully offline.** Never add the INTERNET permission.
5. Money is stored as a `Long` in paisa, quantities as a `Long` in the smallest unit (or with a fixed scale), and dates as epoch millis.
6. All text goes in `strings.xml`: English in `values/`, Urdu in `values-ur/`. Support RTL, light theme and dark theme.
7. `minSdk 24`, and use the latest stable versions for the rest. Never commit keystores or `local.properties`.

## Branches (do them in this order)

| # | Branch | What to build |
|---|--------|---------------|
| 1 | `feature/project-setup` | Gradle files, version catalog, Hilt app class, package structure (core/data/domain/ui/di) |
| 2 | `feature/theme-navigation` | M3 theme (light/dark, construction orange/grey), Navigation Compose routes, bottom bar (Projects, Expenses, Reports, Settings) |
| 3 | `feature/database` | Room: Project, Stage, MaterialType, MaterialEntry, Supplier, SupplierPayment, Labour, Attendance, LabourPayment, Contractor, ContractorPayment, Expense, Photo. DAOs, repositories, use cases |
| 4 | `feature/user-role` | On first launch, ask: "I am the Owner" or "I am a Thekedar". Labels and the dashboard change by role |
| 5 | `feature/projects` | Project list + add/edit: name, site address, owner/client name + phone, start date, total budget, area (marla/sq ft), status |
| 6 | `feature/project-dashboard` | Per-project view: budget vs spent, spent by category (material/labour/contractor/other), pending dues, progress %, and a budget warning |
| 7 | `feature/stages` | Stages (foundation, structure, chhat/roof, plaster, electric, plumbing, tiles, paint, finishing): budget, status, start/end dates, editable list |
| 8 | `feature/material` | Material entries (eent, cement, sariya, ret, bajri, custom): quantity, unit, rate, total, supplier, stage, bill photo. Stock used/remaining |
| 9 | `feature/suppliers` | Supplier khata: material taken, payments made, balance due, and a call/WhatsApp button |
| 10 | `feature/labour` | Labour/mistri list: daily wage (dihari), daily attendance (present/half/absent), advance, weekly settlement, payment history |
| 11 | `feature/contractor` | Contractor (thekedar) khata: agreed amount (fixed or per sq ft), payments given, work done %, balance |
| 12 | `feature/expenses` | Other expenses (transport, tools, naqsha, NOC, bijli/pani connection, chai-pani): category, amount, stage, bill photo |
| 13 | `feature/site-photos` | Daily site photos per stage, and a progress timeline by date |
| 14 | `feature/reports-pdf` | Reports: full project summary, stage-wise, material-wise, labour, supplier/contractor statements, with date-range filters. PDF via PdfDocument, shared via WhatsApp/share sheet (FileProvider) |
| 15 | `feature/excel-export` | Export to CSV/XLSX through SAF |
| 16 | `feature/reminders` | Reminders: labour weekly payment, supplier payment due, stage deadlines (WorkManager notifications) |
| 17 | `feature/calculator` | Quick estimator: eent/cement/sariya needed for a wall/slab size (a simple tool inside the app) |
| 18 | `feature/backup-restore` | Offline backup file (DB + photos zip) through SAF, and restore |
| 19 | `feature/settings-onboarding` | Language (EN/UR), theme, currency, units (marla/kanal/sq ft), app lock (PIN/biometric), 3-screen onboarding |
| 20 | `feature/polish-release` | App icon, splash, empty states, R8 rules, signing config read from `keystore.properties` |

After each branch, tell the user in 2–3 lines what was done and the branch name.
