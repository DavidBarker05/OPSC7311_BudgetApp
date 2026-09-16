My Money Tree
---------------------------------------------------------------------------------------------------------------------------------------------

Part 1
-------------------------------------------------------------------------------------------------------------------------------------------------
Research - [Research_OPSC7311_Part1A_Tiyah_Singh_ST10453245 (1).pdf](https://github.com/user-attachments/files/32261596/Research_OPSC7311_Part1A_Tiyah_Singh_ST10453245.1.pdf)

Planning & Design - [Planning & Design_OPSC7311_Part1B_Tiyah_Singh_ST10453245 (1).pdf](https://github.com/user-attachments/files/32261606/Planning.Design_OPSC7311_Part1B_Tiyah_Singh_ST10453245.1.pdf)


Gantt Chart - [OPSC7311_P1_Gantt_Chart_ST10453245_Tiyah_Singh (1).xlsx](https://github.com/user-attachments/files/32261612/OPSC7311_P1_Gantt_Chart_ST10453245_Tiyah_Singh.1.xlsx)

Part 2
------------------------------------------------------------------------------------------------------------------------------------------------

Code References & Bibliography List - 

Gantt Chart Project Schedule - 

Video Demonstration - 

-----------------------------------------------------------------------------------------------------------------------------------------------

**Welcome to your financial sanctuary.**

My Money Tree is an Android personal budgeting and financial management application developed as the Part 2 prototype for a BCIS Software Development and Design module. The project builds on the Part 1B Planning and Design work, where the application was framed as a Financial Sanctuary: a calmer space for monitoring spending, managing savings goals and building healthier money habits.

This README documents the implemented prototype. It explains the concept behind the application, the functionality that has been built, the technical approach used, and how to run and evaluate the project.

Repository: https://github.com/DavidBarker05/OPSC7311_BudgetApp

## 1. Introduction

Budgeting applications are widely available, but many still feel dense, clinical or stressful to use. That matters in a student and young-adult context, where the problem is often not only knowing what to track, but being willing to open the app regularly.

My Money Tree was designed to keep the useful parts of modern budgeting software (categories, amounts, dates, history, progress and clear navigation) while presenting them in a quieter, nature-inspired interface. The Part 2 build turns that design direction into a working Android prototype with local persistence, validated input, optional receipt images, monthly min/max spending goals and user-created savings goals.

The prototype is intended for a single logged-in user on one device. All application data is stored locally. Cloud sync, social authentication and commercial deployment fall outside the Part 2 scope.

## 2. Design concept and Part 1B influence

### 2.1 Financial Sanctuary

The Financial Sanctuary concept treats money management as something that should feel approachable rather than punitive. In the interface this appears through greens and cream tones, garden-style category tiles, "Sow Expenses" wording, nature artwork on splash and welcome screens, and a watering-can area for savings goals.

Progress is communicated visually through budget bars, goal percentages and the watering-can fill graphic. The intention is that reviewing spending and savings feels more like returning to a personal space than reading a bank statement.

### 2.2 Research influence

Part 1B compared products such as YNAB, Wallet by BudgetBakers and Spendee. From that work, the prototype retained practical patterns that users already understand:

- structured expense categories
- transaction history with filtering
- clear amounts, dates and progress indicators
- simple navigation between tracking, review and goals

What changed was the presentation and emotional tone. My Money Tree keeps those functional expectations, but organises them around a nature and growth metaphor rather than a purely transactional layout.

### 2.3 Relationship to the wider vision

Part 1B also described a longer gamification loop in which savings feed a watering can, water supports Money Tree growth, and growth stages reward consistent behaviour. In Part 2, that idea is only partly realised. Users can create savings goals, add contributions and see watering-can progress. Tree leveling data already has its own database table, but the visible growth system, watering interaction and achievements are reserved for the final PoE so that Part 2 can focus on reliable budgeting behaviour.

## 3. Application functionality

### 3.1 Account management

Users can create an account with display name, email, phone number, date of birth, password and currency (ZAR). Login accepts either username or email with password. Profile information and profile photo can be edited after login.

A local password-recovery prototype is included. The user enters an email address, receives an on-screen security pin, confirms the pin and sets a new password. This is an in-app flow rather than true email delivery. The authenticated session is held in memory for the current process through `UserSession`.

### 3.2 Categories and transactions

On first use, the application seeds default garden categories such as Food, Transport, Medicine, Groceries, Rent, Gifts, Savings and Entertainment. Users can create additional categories and edit or delete existing ones, including name, icon and optional monthly budget.

Expenses and income can be recorded with:

- title
- amount
- category
- date
- start time and end time
- optional receipt or proof image from gallery or camera

Saved entries can later be edited or deleted, including their attached image. Amounts may be entered with currency symbols such as R or $; these are stripped before parsing. Receipt images are stored locally through `LocalImageStorageSystem` and linked by file path on the relevant database row.

Transaction review is available across Home, Transactions, Search and Category Detail, with support for period, category and income/expense filtering.

### 3.3 Monthly budgeting

The Monthly Goal allows the user to set a minimum and maximum total spend for the current month. This is the Part 2 min/max spending goal requirement and is treated as a budgeting control, separate from savings gamification.

Budget bars and progress indicators on Home, Categories, Category Detail, Watering Can and Analysis are scoped to the current month. By default they reflect the Monthly Goal. When a specific category is opened, progress can instead reflect that category's own monthly budget where one has been set.

On the Home screen, the Daily / Weekly / Monthly toggle updates both the balance figures and the transaction list below them, so the summary and detail views stay aligned.

### 3.4 Savings goals

Savings goals are user-created rather than hardcoded. From the Goals (Watering Can) area, a user can create a goal with a name, icon and optional target amount, then edit or delete that goal later. Contributions can be added or removed against each goal.

`WateringCanView` and Goal Detail present progress from actual contributions versus each goal target. Home and Quickly Analysis also use genuine savings-goal figures, rather than presenting expense or category-budget calculations under a savings label.

### 3.5 Analysis and review

The application includes:

- Home dashboard with period-based totals and recent activity
- Transactions history with month and type filters
- Analysis with period totals, charting and monthly budget progress
- Quickly Analysis as an additional snapshot view
- Search by text, category, type and date
- an in-app notifications list built from recent activity and reminder copy

Amounts are displayed with an R prefix using `MoneyFormatter`. Invalid or incomplete input is generally rejected with Toast feedback rather than allowed to fail silently.

## 4. Technical implementation

### 4.1 Platform and libraries

| Area | Details |
| --- | --- |
| Language | Kotlin |
| UI | XML layouts with Material 3 (`Theme.Material3.Light.NoActionBar`) |
| Architecture style | Activities with manager classes over Room DAOs |
| Persistence | Room over SQLite (`mybudgettree.db`, `AppDatabase`) |
| Images | Local private storage via `LocalImageStorageSystem` |
| Concurrency | Kotlin coroutines for database and image work |
| SDK | minSdk 26, targetSdk 36, compileSdk 37 |
| Build | Gradle 9.4.1 / Android Gradle Plugin 9.2.1 |
| CI | GitHub Actions workflow `.github/workflows/tests.yml` on `master` |

The project does not use Jetpack Compose, Firebase, Retrofit or a remote backend. The current prototype is fully offline for a single device.

### 4.2 Data model

Primary entities:

| Entity | Role |
| --- | --- |
| `User` | Account credentials and profile details |
| `Category` | Spending or savings category, icon and optional monthly budget |
| `Expense` | Outgoing transaction with date, times and optional image path |
| `Income` | Incoming transaction; also used for savings contributions |

Manager classes (`UserDatabaseSystem`, `CategoryDatabaseSystem`, `ExpenseDatabaseSystem`, `IncomeDatabaseSystem`) validate input and mediate Room access. Activities call these managers rather than talking to DAOs directly. `DateTimeConverter` stores `LocalDate`, `LocalTime` and `YearMonth` values as strings.

Tree and watering-can leveling information is stored in a separate table from `User`. The schema is present for later gamification work, but the growth logic itself is inactive in Part 2 so it does not interfere with category or budget behaviour.

Database construction occurs in `BudgetTreeApplication`. Schema upgrades currently use destructive migration, which is acceptable for a prototype but means local data can be wiped when the schema changes.

### 4.3 Validation and error handling

On-screen validation covers blank login fields, failed credentials, incomplete sign-up details, mismatched passwords, invalid recovery email or pin, missing expense fields, invalid amounts, duplicate category names, blank profile fields and denied camera permission.

At the manager layer, checks include required user fields, username format, email and phone format, duplicate identity fields, blank or duplicate category names, blank descriptions, negative amounts, end time before start time, and blank image paths when an image is expected.

### 4.4 Testing and continuous integration

Instrumented tests cover the database managers and local image storage behaviour. GitHub Actions builds the debug APK and runs unit and instrumented test tasks on push and pull request to `master`, using JDK 21 and an Android emulator for connected tests.

The local unit test suite is still limited, and Espresso UI tests have not been added yet. Testing therefore supports confidence in persistence and image handling more than full interface coverage.

## 5. How to run the application

Requirements:

- Android Studio with an SDK able to compile API 37
- Emulator or physical device on API 26 or higher
- A JDK suitable for the current Android Gradle Plugin (CI uses JDK 21)

Steps:

```bash
git clone https://github.com/DavidBarker05/OPSC7311_BudgetApp.git
```

1. Open the cloned project in Android Studio.
2. Allow Gradle to sync.
3. Select an emulator or device.
4. Run the debug configuration.

No API keys or Firebase setup are required. On first launch the application moves from splash to welcome, then to sign up or log in. Camera permission is requested only when the user chooses to take a photo.

Suggested walkthrough after login:

1. Review or edit categories in the Categories garden.
2. Add an expense through Sow Expenses, including date, times and optional receipt.
3. Set a Monthly Goal with minimum and maximum spend.
4. Create one or more savings goals and add contributions.
5. Review totals and progress on Home, Transactions and Analysis.

## 6. Project structure

```text
OPSC7311_BudgetApp/
├── .github/workflows/tests.yml
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/mybudgettree/
│       │   │   ├── *Activity.kt
│       │   │   ├── MainNavigation.kt
│       │   │   ├── BudgetTreeApplication.kt
│       │   │   ├── database/
│       │   │   └── imagestorage/
│       │   └── res/
│       ├── test/
│       └── androidTest/
├── gradle/libs.versions.toml
└── README.md
```

Main screens include authentication (welcome, login, sign up, password recovery), Home, Analysis, Quickly Analysis, Transactions, Search, Categories, Category Detail, Sow Expenses, Watering Can, Goal Detail, Fill Watering Can, Profile, Edit Profile, Help and Notifications.

## 7. Scope, limitations and future development

### 7.1 Part 2 scope

Part 2 delivers a working Android prototype that demonstrates:

- account creation and login
- editable categories and transactions
- optional receipt images
- user-selected start and end times
- monthly minimum and maximum spending goals
- user-created savings goals with contributions
- local Room persistence and image storage
- GitHub version control and CI

### 7.2 Current limitations

The prototype remains intentionally focused. Passwords are stored as plain text. The login session is not remembered after the process ends. Social login buttons are placeholders. The in-app notifications list is not a push-notification service. Destructive migration can clear local data on schema change. Automated testing does not yet cover the full UI surface.

### 7.3 Future development

Further work toward the final PoE can expand the Money Tree growth system (visible tree stages, watering interaction and achievements), strengthen password handling, persist the login session, improve financial reporting and broaden unit and UI testing. Cloud synchronisation and a finished commercial service are outside the current academic prototype.

## 8. Author and project information

| Field | Details |
| --- | --- |
| Project | My Money Tree |
| Student | Tiyah Singh | Jamie-Lee Davies | David Adam Barker
| Institution | Vega School / EMERIS |
| Programme | BCIS in Software Development and Design |
| Stage | Part 2 Prototype |
| Application ID | `com.example.mybudgettree` |
| Version | 1.0 |
| Repository | https://github.com/DavidBarker05/OPSC7311_BudgetApp |

## 9. References

Planning research for Part 1B included YNAB, Wallet by BudgetBakers and Spendee. Implementation work drew on Android Developers documentation, the Room persistence library, Kotlin language resources and Material Design 3 guidance. 


https://github.com/DavidBarker05/OPSC7311_BudgetApp
