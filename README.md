[Planning & Design_OPSC7311_Part1B_Tiyah_Singh_ST10453245 (1).pdf](https://github.com/user-attachments/files/32261600/Planning.Design_OPSC7311_Part1B_Tiyah_Singh_ST10453245.1.pdf)# 🌱 My Money Tree

Part 1
-------------------------------------------------------------------------------------------------------------------------------------------------
Research - [Research_OPSC7311_Part1A_Tiyah_Singh_ST10453245 (1).pdf](https://github.com/user-attachments/files/32261596/Research_OPSC7311_Part1A_Tiyah_Singh_ST10453245.1.pdf)

Planning & Design - [Planning & Design_OPSC7311_Part1B_Tiyah_Singh_ST10453245 (1).pdf](https://github.com/user-attachments/files/32261606/Planning.Design_OPSC7311_Part1B_Tiyah_Singh_ST10453245.1.pdf)


Gantt Chart - [OPSC7311_P1_Gantt_Chart_ST10453245_Tiyah_Singh (1).xlsx](https://github.com/user-attachments/files/32261612/OPSC7311_P1_Gantt_Chart_ST10453245_Tiyah_Singh.1.xlsx)

Part 2
------------------------------------------------------------------------------------------------------------------------------------------------

Code References & Bibliography - 

Gantt Chart Project Schedule - 

Video Demonstration - 


### Welcome to your financial sanctuary.

My Money Tree is a personal financial management and budgeting application for Android. It is intended to make tracking money feel more approachable, organised and easier to live with day to day.

This README documents the **Part 2 prototype**. It connects the original Part 1B Planning and Design concept to the functionality that is actually present in the current codebase. Features that were planned in Part 1B but are not finished here are marked as planned or future work, not as completed work.

The launcher label in `strings.xml` is `MyMoneyTree`. This document uses the project name **My Money Tree**.

---

## Project overview

My Money Tree is a local Android prototype that lets a user create an account, log in, record spending against categories, attach an optional receipt image, save money towards named goals, and review balances and transactions in the app.

The problem it tries to address is that budgeting tools can feel cold, busy or stressful, especially for people who are still building a habit of looking at their money. Part 1B framed this as a **Financial Sanctuary**: a personal space where someone can monitor spending, keep an eye on goals and practise healthier financial habits in a calmer visual environment.

The original planning work described students, young adults, beginner-to-intermediate budgeters and more experienced users as possible audiences. The Part 2 build focuses on a single logged-in user on one device, with data stored locally rather than in the cloud.

Part 2 exists to turn that concept into a working prototype: XML screens, Room persistence, input checks, optional photographs, period-based lists, and GitHub-based version control and CI. It does not claim to be the full Final POE product described in Part 1B.

---

## Design concept: the Financial Sanctuary

Financial management can feel overwhelming. My Money Tree uses nature and growth as a visual metaphor for financial wellbeing: greens, cream backgrounds, garden-style category tiles, a watering-can savings screen and a tree badge as the app identity.

The idea is that if the interface feels calmer, it is easier to come back to it. Gamification was part of the Part 1B research (progress, rewards, a growing tree). In this prototype, that idea shows up as:

- a watering-can fill graphic based on savings versus goal targets
- progress bars and donut-style target rings
- water-drop indicators on Home
- nature illustrations on splash and welcome

It does **not** yet include a growing Money Tree on screen, achievements, or a full watering-the-tree interaction. Those remain later-stage ideas.

---

## Part 1B research and design influence

Part 1B compared existing budgeting products, including [YNAB](https://www.ynab.com/), [Wallet by BudgetBakers](https://budgetbakers.com/) and [Spendee](https://www.spendee.com/). Useful themes from that research were structured budgeting, reporting, readable visuals, simple navigation, spending and savings tracking, and ways to keep people engaged over time.

My Money Tree tries to keep the useful parts of those products (categories, amounts, dates, history, a sense of progress) while presenting them in a simpler, more nature-led interface. The Financial Sanctuary idea came from that research: money tools often cover the numbers well, but they do not always address how uncomfortable it can feel to look at spending.

This README is not a literature review. The full comparison, audience work and Figma direction sit in the separate Part 1B Planning and Design document.

---

## Part 2 prototype features

The table below is based on the current source, not on the Part 1B wish list.

| Feature | Description | Status |
| ------- | ----------- | ------ |
| Account creation | `SignupActivity` creates a `User` in Room (display name, email, phone, date of birth, password, currency `ZAR`) | Implemented |
| Login | `LoginActivity` checks username or email plus password through `UserDatabaseSystem.login` | Implemented |
| Session | `UserSession` holds the logged-in user in memory for the current process | Implemented (not persisted after the app process ends) |
| Password reset (prototype) | Email, on-screen 6-digit pin, new password screens | Implemented as a local flow (the pin is shown in a dialog, not emailed) |
| Expense categories | Default garden categories, plus a dialog to add a named category | Implemented |
| Expense entries | Sow Expenses saves description, amount, category, date and optional image | Implemented |
| Date | User picks the expense date with `DatePickerDialog` | Implemented |
| Start and end time | Stored on `Expense` / `Income` | Partial: saved as `LocalTime.now()`, not chosen by the user. Lists show the stored start time |
| Expense description | Title plus optional message, combined when saving | Implemented |
| Optional photograph | Gallery or camera, stored under app files via `LocalImageStorageSystem` | Implemented |
| Viewing stored photographs | Receipt preview on Sow Expenses; thumbnails in transaction lists if `imagePath` exists | Implemented (thumbnail / preview, not a separate full-screen gallery) |
| Viewing entries over a period | Home (daily / weekly / monthly), Transactions (month picker), category and goal lists (month picker), Search (one date) | Implemented |
| Category totals over a selected period | Analysis shows overall income/expense for daily, weekly, monthly or yearly ranges. Category Detail lists that category’s expenses, filterable by month | Partial: there is no dedicated screen that lists every category’s period total together |
| Minimum and maximum spending goals | `Category.budgetAmount` is a single optional amount | Not implemented as min + max. There is no SeekBar |
| Savings goals | Travel, Wedding and Car categories with default targets; savings stored as `Income` | Implemented (single target per goal, not min/max spend goals) |
| Watering can | `WateringCanView` fill percent from savings versus goal targets | Implemented as a progress graphic |
| Money Tree growth | `User.treeLevel` / `treeLevelPeriod` exist on the user record | Not used in the UI |
| Local / offline storage | Room database `mybudgettree.db` and local image files | Implemented |
| Input validation | Empty fields, invalid amounts, login failures, duplicate category names, and similar checks with Toasts | Implemented |
| Simple analysis charts | Custom `AnalysisChartView` bars and `DonutTargetView` rings | Implemented as prototype charts, not a full analytics product |
| Logging | `android.util.Log` in login, signup, expenses, savings, categories and database managers | Implemented |
| Firebase / cloud sync | Not present | Not implemented |
| Social login | Facebook / Google buttons show “coming soon” | Not implemented |

---

## Part 2 requirements checklist

Statuses were checked against the project, not against what the assignment asks for.

- [x] Login with username or email and password
- [x] Expense categories (create, list, open a category)
- [x] Expense entries (amount, description, category, date)
- [~] Date and time (date is user-selected; start and end time are stored automatically)
- [x] Expense description
- [x] Optional photograph / receipt
- [ ] Minimum and maximum spending goals (SeekBar or equivalent)
- [x] Viewing expense entries over a selected period
- [x] Accessing stored photographs from saved entries (as list thumbnails / add-screen preview)
- [~] Category totals over a selected period (overall analysis totals and per-category month lists exist; no all-categories period-total screen)
- [x] Local / offline Room persistence
- [x] User-facing screens with a shared green / cream layout and bottom navigation
- [x] Invalid input handling with Toast messages
- [~] Testing of main functionality (instrumented database and image tests exist; the local unit test is still the example `2 + 2`; no Espresso UI tests were found)
- [x] Git / GitHub version control
- [x] GitHub Actions workflow (`.github/workflows/tests.yml`)

---

## My Money Tree's unique concept

Part 1B described this loop:

**Savings → Watering Can → Water → Money Tree Growth**

In the current prototype, that idea is only partly built.

**What is in the app now**

- Goals are the named categories Travel, Wedding and Car (`CategoryGoals`).
- If they do not exist yet, they are created with default targets (R25 000, R34 700 and R20 000).
- “Fill watering can” saves an `Income` row against the chosen goal.
- `WateringCanActivity` fills `WateringCanView` using savings total ÷ sum of goal targets.
- Goal Detail shows amount saved, target, a progress bar and a donut ring.
- Home shows a savings-style card, a donut ring and water-drop icons driven by `GoalSnapshot` (those drops currently follow spending versus the sum of spending-category `budgetAmount` values, which are usually unset in the UI).

**What is still planned**

- Using the watering can to water a visible Money Tree
- Tree growth stages on screen
- Achievements and milestones

Other things that are actually in this project (not invented):

- Garden language: “Sow Expenses”, category garden tiles, sapling-style cards
- Local receipt images without a backend
- Custom chart and watering-can views drawn in code
- A notification list built from recent transactions plus reminder copy (`NotificationFeed`), not a push-notification service

---

## User interface and design

The UI is XML layouts with Material 3 (`Theme.Material3.Light.NoActionBar`). There is no Jetpack Compose in this project. No custom font files were found; text uses the platform / Material defaults.

**Colour (from `res/values/colors.xml`)**

| Token | Hex | Typical use |
| ----- | --- | ----------- |
| `home_header` | `#1B5E3B` | Home / analysis headers, dark green page tops |
| `green_primary` | `#1C6D3C` | Theme primary |
| `green_text` | `#1A3328` | Body and labels |
| `home_sheet` | `#F3F8F0` | Light content cards |
| `cream` | `#F8F7F2` | Splash / window background |
| `sage_header` | `#A0B8A3` | Auth screen headers |
| `mint_button` / `card_fill` / `sow_field` | light greens | Buttons, cards, form fields |
| `nav_bar` | `#EFF6EC` | Bottom navigation |

**Layout habits**

- Rounded cards and filled input fields
- Material buttons for primary actions
- Bottom navigation: Home, Analytics, Transactions, Categories, Goals, Profile (`include_bottom_nav.xml`)
- Forest / tree artwork on splash and welcome (`bg_splash`, `bg_welcome`, `img_splash_logo`, `img_logo_badge`)
- Category and goal icons (food, transport, plane, car, wedding rings, and others)
- Toast messages and empty-state text for feedback

The look is meant to feel like a quiet garden rather than a bank statement. The Part 2 screens follow that direction, but they can still differ from the original Figma frames.

There are no separate screenshot files in the repository (only drawable assets). Placeholders:

<!-- Add screenshot: Splash Screen -->
<!-- Add screenshot: Welcome Screen -->
<!-- Add screenshot: Login Screen -->
<!-- Add screenshot: Home Screen -->
<!-- Add screenshot: Sow Expenses Screen -->
<!-- Add screenshot: Categories Screen -->
<!-- Add screenshot: Watering Can / Goals Screen -->
<!-- Add screenshot: Analysis Screen -->
<!-- Add screenshot: Profile Screen -->

---

## Application screens

These activities are registered in `AndroidManifest.xml`.

| Screen | Class | Purpose |
| ------ | ----- | ------- |
| Splash | `MainActivity` | Shows the splash layout, then opens Welcome after 2 seconds |
| Welcome | `WelcomeActivity` | Log In, Sign Up, Forgot Password |
| Login | `LoginActivity` | Username or email and password |
| Sign up | `SignupActivity` | New account fields and date of birth picker |
| Forgot password | `ForgotPasswordActivity` | Email, then a displayed security pin |
| Security pin | `SecurityPinActivity` | 6-digit pin entry |
| New password | `NewPasswordActivity` | New password + confirm |
| Password changed | `PasswordChangedActivity` | Success message |
| Home | `HomeActivity` | Balance, expenses, period toggle, recent transactions, savings card |
| Quickly analysis | `QuicklyAnalysisActivity` | Extra home-style snapshot with a weekly expense chart |
| Analysis | `AnalysisActivity` | Daily / weekly / monthly / yearly totals, bar chart, target rings |
| Search | `SearchActivity` | Filter by text, category, income/expense and a single date |
| Transactions | `TransactionActivity` | Full history, income/expense filter, month grouping |
| Categories | `CategoriesActivity` | Garden grid, add category, budget overview header |
| Category detail | `CategoryDetailActivity` | Expenses for one category, month filter, Sow Expenses |
| Sow expenses | `SowExpensesActivity` | Add an expense, optional receipt |
| Watering can (Goals) | `WateringCanActivity` | Goal tiles and can fill level |
| Goal detail | `GoalDetailActivity` | One goal’s progress and savings list |
| Fill watering can | `FillWateringCanActivity` | Add a savings deposit |
| Profile | `ProfileActivity` | Name, photo, edit, help, logout |
| Edit profile | `EditProfileActivity` | Name, phone, email, photo, push toggle |
| Help | `HelpActivity` | FAQ search and contact rows |
| Notifications | `NotificationsActivity` | In-app list from `NotificationFeed` |

The Part 2 screens are an implementation stage. Where they differ from Figma, the running app is the source of truth for this submission.

---

## User flow

Typical path through the **implemented** navigation:

```text
Launch (splash)
    ↓
Welcome
    ↓
Sign up  or  Log in  (forgot password is optional)
    ↓
Home
    ↓
Add/manage information:
    Categories → Category detail → Sow expenses
    Goals → Goal detail → Fill watering can
    ↓
Review:
    Home period list
    Transactions
    Analysis / Search
    ↓
Profile (edit details, help, log out)
```

Bottom navigation from Home and related screens:

Home · Analytics · Transactions · Categories · Goals · Profile

If there is no `UserSession`, protected screens send the user back to Welcome.

---

## Technologies used

Taken from `app/build.gradle.kts`, `gradle/libs.versions.toml` and the source.

| Technology | Purpose |
| ---------- | ------- |
| Kotlin | Application language (KSP / compiler line `2.2.10`) |
| Android SDK | `minSdk` 26, `targetSdk` 36, `compileSdk` 37 |
| XML layouts | All screens (`res/layout`) |
| AndroidX AppCompat / Activity KTX | Activities and window / result APIs |
| Material Components 1.14.0 | Buttons, text fields, switches, theme |
| ConstraintLayout / RecyclerView | Screen layout and lists |
| Room 2.7.2 | Local SQLite persistence |
| KSP | Room annotation processing |
| Kotlin coroutines | Database and image work off the main thread (`lifecycleScope`) |
| JUnit 4 | Local unit tests |
| AndroidX JUnit + Espresso | Instrumented test runner (Espresso is a dependency; no Espresso UI test classes were found) |
| Gradle 9.4.1 / AGP 9.2.1 | Build |

Not used in this project: Jetpack Compose, Firebase, Retrofit, a remote API.

---

## Database and data persistence

**Technology:** Room over SQLite. Database name: `mybudgettree.db`. Class: `AppDatabase` (version 2, `exportSchema = false`). Built in `BudgetTreeApplication` with `fallbackToDestructiveMigration(true)`.

**Type conversion:** `DateTimeConverter` stores `LocalDate`, `LocalTime` and `YearMonth` as strings.

**Entities**

| Entity | Table | Main fields |
| ------ | ----- | ----------- |
| `User` | `users` | username (PK), password, email, phone, display name, date of birth, currency, profile photo path, `treeLevel`, `treeLevelPeriod` |
| `Category` | `categories` | id, username, category name (unique per user), optional `budgetAmount` |
| `Expense` | `expenses` | id, category id, description, amount, date, start time, end time, optional `imagePath` |
| `Income` | `incomes` | same shape as expenses (used for savings deposits) |

**DAOs:** `UserDao`, `CategoryDao`, `ExpenseDao`, `IncomeDao`  
**Managers:** `UserDatabaseSystem`, `CategoryDatabaseSystem`, `ExpenseDatabaseSystem`, `IncomeDatabaseSystem`

Managers wrap validation and Room calls and return `wasSuccessful` / `errMsg` style results. DAOs can query by date, date range, description and category. The UI often loads all rows for the user and then filters in Kotlin (for example Home and Analysis).

**Images:** `LocalImageStorageSystem` writes files under `filesDir/images` and stores the path on the expense, income or user row. The app does not need a network connection for this.

**Offline:** yes, for the current device. There is no cloud backup in code. The login session is in memory only.

---

## Input validation and error handling

**On screen (Toasts)**

- Login: blank identifier or password; failed credentials
- Sign up: empty fields, passwords that do not match, invalid date of birth; Room errors such as duplicate email are shown from `errMsg`
- Forgot password: blank or invalid email; incomplete or wrong pin
- New password: blank or mismatched passwords
- Sow expenses / fill watering can: missing title or amount, non-numeric or negative amount, missing category, no categories yet
- New category: blank name; duplicate name comes back from the manager
- Edit profile: blank name, phone or email
- Camera permission denied when taking a photo

**In managers**

- Empty username, password, email, phone, display name or currency on create user
- Username limited to English letters and numbers
- Email and phone format checks
- Duplicate username, email or phone
- Blank category name; duplicate category name for the same user
- Blank expense/income description, negative amount, end time before start time, blank image path if one is supplied
- Negative category budget amount (database layer; the UI does not currently offer a budget editor)

Invalid input is meant to fail with a message rather than crash. Limitations: passwords are stored as plain text in Room; sign-up password rules are “not blank” and “must match”; expense times are not validated by the user because they cannot pick them.

---

## Testing

| Location | Class | What it covers |
| -------- | ----- | -------------- |
| `app/src/test` | `ExampleUnitTest` | Example `assertEquals(4, 2 + 2)` only |
| `app/src/androidTest` | `ExampleInstrumentedTest` | Package name `com.example.mybudgettree` |
| | `UserDatabaseSystemTest` | Create user, duplicates, login, updates including tree level fields |
| | `CategoryDatabaseSystemTest` | Create category, budget update, validation |
| | `ExpenseDatabaseSystemTest` | Create expense, negative amount, end before start, retrieve |
| | `IncomeDatabaseSystemTest` | Create / retrieve income (smaller than the user tests) |
| | `ExpenseImageIntegrationTest` | Expense + saved image path |
| | `IncomeImageIntegrationTest` | Income + saved image path |
| | `LocalImageStorageSystemTest` | Save, load, delete, null bytes |
| | `DatabaseTestBase` | In-memory Room setup for the manager tests |

There are no Activity / Espresso UI tests in the repo. This README does not claim that CI is currently green, only that the workflow and test classes exist.

---

## GitHub and version control

The project is a Git repository. The configured remote is:

[https://github.com/DavidBarker05/OPSC7311_BudgetApp](https://github.com/DavidBarker05/OPSC7311_BudgetApp)

Default branch on the remote is `master`. Other remote branches seen from this clone include `jamie`, `david`, `tiyah` and patch branches. Commits cover login/sign-up, home, analytics, categories, goals, UI adjustments, logging and README updates.

This README is the project documentation file at the repository root.

---

## GitHub Actions / continuous integration

File: `.github/workflows/tests.yml`  
Name: `Tests`  
Runs on: push and pull request to `master`

**Job `build-and-unit-test`** (Ubuntu, Temurin JDK 21)

1. Checkout
2. `./gradlew assembleDebug`
3. `./gradlew testDebugUnitTest`

**Job `instrumented-test`**

- Uses `reactivecircus/android-emulator-runner@v2`
- Emulator: API 34, `google_apis`, x86_64, Pixel 6 profile
- Command: `./gradlew connectedDebugAndroidTest`

The workflow builds the debug APK and runs the unit and instrumented test tasks described above. Pass/fail of a specific run is not recorded in this README.

---

## Project structure

Simplified view of the parts that matter for Part 2:

```text
OPSC7311_BudgetApp/
├── .github/workflows/tests.yml
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/mybudgettree/
│       │   │   ├── *Activity.kt          # screens
│       │   │   ├── MainNavigation.kt
│       │   │   ├── BudgetTreeApplication.kt
│       │   │   ├── database/             # Room + managers
│       │   │   └── imagestorage/         # local files
│       │   └── res/
│       │       ├── layout/
│       │       ├── drawable/ and drawable-nodpi/
│       │       ├── values/               # colours, strings, themes
│       │       └── mipmap-*/             # launcher icons
│       ├── test/                         # ExampleUnitTest
│       └── androidTest/                  # Room and image tests
├── gradle/libs.versions.toml
└── README.md
```

`BudgetTreeApplication` constructs Room and the manager objects once. Screens talk to those managers instead of calling DAOs directly.

---

## How to run the application

Requirements taken from Gradle: Android Studio with an SDK that can compile API 37, a device or emulator on **API 26 or higher**, and a JDK suitable for this Android Gradle Plugin (CI uses **JDK 21**).

1. Clone the repository:  
   `git clone https://github.com/DavidBarker05/OPSC7311_BudgetApp.git`
2. Open the folder in Android Studio.
3. Let Gradle sync (wrapper uses Gradle **9.4.1**).
4. Select an emulator or a physical device.
5. Run the `app` configuration (debug).

There is no extra API key or Firebase setup. First launch: splash → welcome → sign up or log in.

Camera permission is requested only if the user takes a photo. The camera hardware is optional in the manifest (`required="false"`).

---

## How to use the application

### Login

On Welcome, open Log In. Enter the **username** (created from the email local-part at sign-up) **or** the email, plus the password. Empty fields and failed logins show a Toast. Facebook and Google buttons are placeholders.

### Add an expense

Open Categories, tap a category (or add one with the extra tile), then **Sow Expenses**. Enter a title, amount and category, pick a date, optionally add a gallery or camera image, and save. Amounts may include `R` or `$`; they are stripped before parsing.

### Categories

If the user has no categories yet, the app seeds Food, Transport, Medicine, Groceries, Rent, Gifts, Savings and Entertainment. New names can be added from the dialog. Goal names (Travel, Wedding, Car) are kept off this garden list.

### Goals

Open Goals (watering can). Tap Travel, Wedding or Car, or **Save more** to add a deposit. Deposits are income records against that goal. The can fill and Goal Detail percentages use saved amount versus the goal’s `budgetAmount`.

### Review spending

- **Home:** daily / weekly / monthly lists and totals  
- **Transactions:** all entries, income or expense filter, calendar month  
- **Analysis:** period totals and a simple bar chart; calendar changes the anchor date  
- **Search:** one date, optional category and text, income or expense  

Amounts are shown with an `R` prefix (`MoneyFormatter`).

### Profile

View name and photo, edit details, open Help (FAQ / contact), or log out back to Welcome.

---

## Part 1B vs Part 2

**Part 1B:** planning, research (YNAB, Wallet, Spendee), requirements, UI direction and the broader Financial Sanctuary / Money Tree vision.

**Part 2:** this Android prototype: accounts, categories, expenses with optional photos, local Room storage, period views, a first version of savings goals and the watering-can graphic, logging, tests that exist in the repo, and GitHub Actions.

Part 1B is the wider design. Part 2 is what currently runs.

---

## Current limitations

- No user-facing minimum and maximum spending goals, and no SeekBar.
- Spending-category `budgetAmount` is not edited in the UI, so Home / Analysis “budget” figures are often zero unless a value was set in code or tests.
- Start and end times are stored but not picked by the user.
- No combined “all categories, this period, totals” report screen.
- `treeLevel` is not shown as a growing tree.
- Login session is not remembered after the process is killed.
- Passwords are stored in plain text in Room.
- Social login is not implemented.
- Push-notification switch is a local preference only.
- Help contact URLs in `strings.xml` (for example `https://mymoneytree.app`) are app strings, not a verified live product site.
- Database version upgrades use destructive migration, which can wipe local data on schema change.
- Local unit tests do not cover app logic; UI tests are missing.
- Layouts may still differ from the Part 1B Figma file.

---

## Future development

Appropriate follow-on work from Part 1B and the gaps above:

- Minimum and maximum monthly spend goals (for example two SeekBars)
- User-chosen start and end times
- A clear category-totals-by-period screen
- UI to set spending budgets
- Money Tree growth stages and watering the tree
- Achievements and stronger gamification
- Richer analysis (without claiming it is already here)
- Real notifications if that remains in scope
- Stronger password handling and a remembered session
- Broader unit and UI tests
- Help / FAQ content closer to the finished product

Firebase, cloud sync and a finished commercial service are not part of this Part 2 build.

---

## Development process

Work on this prototype followed a normal student project path:

1. Part 1B planning and visual design  
2. Android XML implementation of the screens  
3. Room entities, DAOs and manager classes  
4. Wiring screens to those managers (login, expenses, categories, goals)  
5. Local images for receipts and profile photos  
6. Instrumented tests around managers and image storage  
7. Logging at key success and failure points  
8. Git branches and pull requests, with GitHub Actions on `master`  
9. UI refinement against the sanctuary look  

---

## References and credits

**Planning research (from Part 1B)**

- [YNAB](https://www.ynab.com/)
- [Wallet by BudgetBakers](https://budgetbakers.com/)
- [Spendee](https://www.spendee.com/)

**Platform documentation used for the implementation**

- [Android developers](https://developer.android.com/)
- [Room persistence library](https://developer.android.com/training/data-storage/room)
- [Kotlin](https://kotlinlang.org/)
- [Material Design 3](https://m3.material.io/)

The full academic bibliography, Gantt chart and video demonstration belong with the Part 1B / submission pack rather than as invented links here.

- Gantt chart / project schedule: add when available  
- Video demonstration: add when available  
- Code references and bibliography: see Part 1B and the links above  

---

## Author / project information

**Project:** My Money Tree  
**Student:** Tiyah Singh  
**Institution:** Vega School / EMERIS  
**Programme:** BCIS in Software Development and Design  
**Project stage:** Part 2 Prototype  
**Application ID:** `com.example.mybudgettree`  
**Version:** `1.0` (`versionCode` 1)  
**Repository:** [https://github.com/DavidBarker05/OPSC7311_BudgetApp](https://github.com/DavidBarker05/OPSC7311_BudgetApp)
