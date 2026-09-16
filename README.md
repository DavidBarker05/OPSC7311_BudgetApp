My Money Tree
---------------------------------------------------------------------------------------------------------------------------------------------

Part 1
-------------------------------------------------------------------------------------------------------------------------------------------------
Research - [Research_OPSC7311_Part1A_Tiyah_Singh_ST10453245 (1).pdf](https://github.com/user-attachments/files/32261596/Research_OPSC7311_Part1A_Tiyah_Singh_ST10453245.1.pdf)

Planning & Design - [Planning & Design_OPSC7311_Part1B_Tiyah_Singh_ST10453245 (1).pdf](https://github.com/user-attachments/files/32261606/Planning.Design_OPSC7311_Part1B_Tiyah_Singh_ST10453245.1.pdf)


Gantt Chart - [OPSC7311_P1_Gantt_Chart_ST10453245_Tiyah_Singh (1).xlsx](https://github.com/user-attachments/files/32261612/OPSC7311_P1_Gantt_Chart_ST10453245_Tiyah_Singh.1.xlsx)

Part 2
------------------------------------------------------------------------------------------------------------------------------------------------

Code References & Bibliography List - [ST10453245_Code_References_Bibliography.pdf](https://github.com/user-attachments/files/32294733/ST10453245_Code_References_Bibliography.pdf)

Gantt Chart Project Schedule - [OPSC7311_P1_Gantt_Chart_ST10453245_Tiyah_Singh.xlsx](https://github.com/user-attachments/files/32295127/OPSC7311_P1_Gantt_Chart_ST10453245_Tiyah_Singh.xlsx)

Video Demonstration - 

-----------------------------------------------------------------------------------------------------------------------------------------------

**Welcome to your financial sanctuary.**

My Money Tree is an Android personal budgeting and financial management application developed as the Part 2 prototype for a BCIS Software Development and Design module. The project builds on the Part 1B Planning and Design work, where the application was framed as a Financial Sanctuary: a calmer space for monitoring spending, managing savings goals and building healthier money habits.

This README documents the implemented prototype. It explains the concept behind the application, the functionality that has been built, the technical approach used, and how to run and evaluate the project.

Repository: https://github.com/DavidBarker05/OPSC7311_BudgetApp

## 1. Introduction

Budgeting applications are widely available, but many still feel dense, clinical or stressful to use. That matters in a student and young-adult context, where the problem is often not only knowing what to track, but being willing to open the app regularly.

My Money Tree was designed to keep the useful parts of modern budgeting software (categories, amounts, dates, history, progress and clear navigation) while presenting them in a quieter, nature-inspired interface. Beyond basic logging, the Part 2 prototype supports full management of categories and transactions, user-selected start and end times, optional receipt images, a Monthly Goal with minimum and maximum spend, and fully customisable savings goals with contributions.

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

### 2.3 Part 2 focus and later gamification

Part 1B described a longer nature and growth theme, including a Money Tree that could grow as users save. Full gamification (tree growth stages, watering interaction and achievements) is required for Part 3, not Part 2. This prototype therefore concentrates on the budgeting core: accounts, categories, transactions, monthly min/max goals and savings goal tracking. The watering-can screen and progress graphics support savings goals in Part 2, while tree leveling data is already stored separately so Part 3 can build on it later.

## 3. Application functionality

The sections below describe the budgeting features that form the core of this Part 2 submission. Particular attention was given to making categories, transactions, monthly spend limits and savings goals fully usable rather than display-only.

### 3.1 Account management

Users can create an account with display name, email, phone number, date of birth, password and currency (ZAR). Login accepts either username or email with password. Profile information and profile photo can be edited after login, and the account can be deleted from Edit Profile with a confirmation prompt.

Password recovery (Forgot Password, security pin and new-password screens) is currently a placeholder: the screens are reachable but only show a "coming soon" message rather than performing a real reset. This is intended to return as an implemented feature alongside Part 3. The authenticated session is held in memory for the current process through `UserSession`.

### 3.2 Categories and transactions

Categories and transactions are fully manageable after creation.

On first use, the application seeds default garden categories such as Food, Transport, Medicine, Groceries, Rent, Gifts and Entertainment. Savings is deliberately not one of the defaults, since it is tracked through the Watering Can savings goals instead. Users can create additional categories and, importantly, edit or delete existing ones. Editing covers the category name, icon and optional monthly budget, so budgets can be adjusted as spending habits change.

Expenses and income can be recorded with:

- title
- amount
- category
- date
- start time and end time chosen by the user
- optional receipt or proof image from gallery or camera

Start and end times are part of the logging flow rather than silent defaults. Saved expenses and income can later be edited or deleted, including their attached receipt image. Amounts may be entered with currency symbols such as R or $; these are stripped before parsing. Receipt images are stored locally through `LocalImageStorageSystem` and linked by file path on the relevant database row.

Transaction review is available across Home, Transactions, Search and Category Detail, with support for period, category and income/expense filtering.

### 3.3 Monthly budgeting

A central Part 2 requirement is the Monthly Goal. This lets the user set both a minimum and a maximum total spend for the current month. It is a real budgeting control for overall monthly spending and is separate from the Part 3 Money Tree growth features.

Budget bars and progress indicators on Home, Categories, Category Detail, Watering Can and Analysis are scoped to the current month only, so progress reflects the active month rather than mixed all-time totals. By default they show the Monthly Goal. When a specific category is opened, progress can instead reflect that category's own monthly budget where one has been set.

On the Home screen, the Daily / Weekly / Monthly toggle updates both the balance figures and the transaction list below them, so the summary and detail views stay aligned.

### 3.4 Savings goals

Savings goals are created by the user. There are no fixed Travel, Wedding or Car goals. From the Goals (Watering Can) area, a user can create a goal with a name, icon and optional target amount, then edit or delete that goal later. Contributions can be added or removed against each goal, so savings progress can be maintained over time.

`WateringCanView` and Goal Detail present progress from actual contributions versus each goal target. Home and Quickly Analysis also use genuine savings-goal figures for the Savings On Goals style summaries, rather than mixing expense or category-budget calculations into those views.

### 3.5 Analysis and review

The application includes:

- Home dashboard with period-based totals and recent activity
- Transactions history with month and type filters, including edit and delete
- Analysis with period totals, charting and monthly budget progress
- Quickly Analysis as an additional snapshot view with savings progress
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

Tree and watering-can leveling information is stored in a separate table from `User`. The schema is in place for Part 3 gamification, but the growth logic is not active in this Part 2 build.

Database construction occurs in `BudgetTreeApplication`. Schema upgrades currently use destructive migration, which is acceptable for a prototype but means local data can be wiped when the schema changes.

### 4.3 Validation and error handling

On-screen validation covers blank login fields, failed credentials, incomplete sign-up details, mismatched passwords, invalid recovery email or pin, missing expense fields, invalid amounts, duplicate category names, blank profile fields and denied camera permission.

At the manager layer, checks include required user fields, username format, email and phone format, duplicate identity fields, blank or duplicate category names, blank descriptions, negative amounts, end time before start time, and blank image paths when an image is expected.

### 4.4 Testing and continuous integration

Testing is split across two source sets. Local JVM unit tests (`app/src/test`) cover pure logic that does not need a device: currency formatting (`MoneyFormatter`), budget status thresholds and text-contrast colour selection (`BudgetStatusHelper`), default category ordering and icon fallback (`CategoryGarden`, `IconCatalog`), Monthly Goal progress calculation (`GoalSnapshot`) and Analysis period bucketing (`AnalysisCalculator`). These run in seconds with `./gradlew testDebugUnitTest` and need no emulator.

Instrumented tests (`app/src/androidTest`) cover the database managers, local image storage behaviour, and Espresso UI tests that drive the real Login, Edit Profile (delete account), Home, Analysis, Transactions, Categories, Watering Can and Profile screens, plus a full bottom-navigation walkthrough across all six main screens. UI tests run against an isolated in-memory database swapped into `BudgetTreeApplication` for the duration of each test, so they never touch real on-device data. Run them with `./gradlew connectedDebugAndroidTest` against a device or emulator.

GitHub Actions builds the debug APK and runs both test tasks on push and pull request to `master`, using JDK 21 and an Android emulator for connected tests.

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

1. Review, edit or delete categories in the Categories garden, including monthly budgets.
2. Add an expense through Sow Expenses with date, start and end times, and an optional receipt.
3. Open an existing expense or income entry to edit or delete it if needed.
4. Set a Monthly Goal with both minimum and maximum spend.
5. Create custom savings goals, add or remove contributions, and check watering-can progress.
6. Use Home, Transactions and Analysis to review period totals and current-month budget progress.

## 6. Scope, limitations and future development

### 6.1 Part 2 scope

Part 2 delivers a working Android prototype with a complete budgeting workflow. In particular, the build demonstrates:

- account creation and login
- create, edit and delete for categories
- create, edit and delete for expenses and income, including receipt images
- user-selected start and end times on transaction entry
- Monthly Goal with minimum and maximum total spend
- current-month budget progress across the main review screens
- user-created savings goals with add and remove contributions
- local Room persistence and image storage
- GitHub version control and CI

### 6.2 Current limitations

The prototype remains intentionally focused. Passwords are stored as plain text. The login session is not remembered after the process ends. Social login buttons are placeholders, and password recovery is a non-functional placeholder pending Part 3. The in-app notifications list is not a push-notification service. Destructive migration can clear local data on schema change. Automated UI testing covers the six main screens and key flows (login, delete account, navigation) rather than every screen and dialog in the app.

### 6.3 Future development

Part 3 will introduce the required gamification layer, including Money Tree growth stages, watering interaction and achievements. Other later improvements may include stronger password handling, a remembered login session, clearer financial reporting and broader unit and UI testing. Cloud synchronisation and a finished commercial service remain outside the academic prototype.

## 7. Authors and project information

| Field | Details |
| --- | --- |
| Project | My Money Tree |
| Team | David Adam Barker, Jamie-Lee Davies, Tiyah Singh |
| Institution | Vega School / EMERIS |
| Programme | BCIS in Software Development and Design |
| Stage | Part 2 Prototype |
| Application ID | `com.example.mybudgettree` |
| Version | 1.0 |
| Repository | https://github.com/DavidBarker05/OPSC7311_BudgetApp |

## 8. References

Planning research for Part 1B included YNAB, Wallet by BudgetBakers and Spendee. Implementation work drew on Android Developers documentation, the Room persistence library, Kotlin language resources and Material Design 3 guidance. 
