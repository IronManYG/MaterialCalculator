# Greenfield Design — MaterialCalculator Phase 2

**Date:** 2026-05-09
**Status:** Draft for review (brainstormed via `superpowers:brainstorming`, awaiting user approval before plan generation)
**Phase:** Phase 2 (post-dormancy-clearance)
**Companion artifact:** `docs/explore/greenfield-vs-current.html` (visual side-by-side)

---

## 1. Context & Goals

This spec is the output of a "pretend the app doesn't exist" greenfield design exercise. The goal is to produce an ideal architecture for the calculator using current best practices, then diff that against the live MaterialCalculator codebase to surface concrete improvements.

**Reference template:** A freshly-created Android Studio project at `E:\AndroidStudioProjects\HabitTracker` using AS's latest default template (Gradle 9.4.1, AGP 9.2.0, Kotlin 2.2.10, KTS, version catalog, package `dev.gaddal.habittracker`).

**Decisions locked before brainstorming:**

| Decision | Value |
|---|---|
| Greenfield ambition | Modernized single-module — MVI + DI + Nav3-ready |
| Feature scope | Pragmatic Phase 2 — calculator + settings + history (3 screens) |
| Architecture pattern | Feature-folder (`feature/<name>/` + `core/`) |
| Namespace target | `dev.gaddal.<brand>` — `<brand>` is a placeholder for the rebrand decision |
| applicationId | **Stays `com.gaddal.materialcalculator`** — preserves Play Store listing, installs, ratings |
| KMP | Domain layer kept KMP-reachable (pure Kotlin, no Android deps); no commitment to KMP yet |

**Brand name** is still open; downstream of this spec.

---

## 2. Architecture

### 2.1 Package layout

```
dev.gaddal.<brand>/
├── core/
│   ├── data/
│   │   ├── database/         # AppDatabase (Room) — aggregates all feature DAOs
│   │   ├── datastore/        # AppDataStore (DataStore<Preferences>)
│   │   ├── history/          # HistoryRepository interface (cross-feature contract)
│   │   ├── settings/         # SettingsRepository interface
│   │   └── di/               # coreDataModule (Koin)
│   ├── domain/
│   │   ├── history/          # HistoryEntry (cross-feature model)
│   │   ├── settings/         # AppSettings, ThemeMode (cross-feature model)
│   │   └── util/             # Result<D, E>, Error marker
│   └── ui/
│       ├── theme/            # Color, Type, Shape, Theme
│       ├── component/        # Reusable composables (empty at start; promoted on demand)
│       └── util/             # UiText, ObserveAsEvents, modifier extensions
│
├── feature/
│   ├── calculator/
│   │   ├── domain/           # ExpressionEvaluator, ExpressionParser, ExpressionWriter, Operation, CalcError
│   │   ├── ui/
│   │   │   ├── components/   # CalculatorButton, CalculatorDisplay, CalculatorButtonGrid
│   │   │   ├── CalculatorScreen.kt
│   │   │   ├── CalculatorRoot.kt
│   │   │   ├── CalculatorState.kt
│   │   │   ├── CalculatorAction.kt
│   │   │   ├── CalculatorEvent.kt
│   │   │   ├── CalculatorViewModel.kt
│   │   │   └── CalcErrorMapper.kt
│   │   └── di/               # calculatorModule
│   │
│   ├── settings/
│   │   ├── data/             # SettingsRepositoryImpl
│   │   ├── ui/               # Screen / Root / State / Action / Event / ViewModel
│   │   └── di/               # settingsModule
│   │
│   └── history/
│       ├── data/             # HistoryRepositoryImpl, HistoryEntity, HistoryDao
│       ├── ui/               # Screen / Root / State / Action / Event / ViewModel + components/HistoryItem
│       └── di/               # historyModule
│
├── navigation/
│   ├── NavRoot.kt            # NavDisplay + entryProvider wiring
│   └── Routes.kt             # data object CalculatorRoute, SettingsRoute, HistoryRoute (NavKey-based)
│
├── di/
│   └── appModule.kt          # assembles core + all feature modules
│
├── App.kt                    # Application class, startKoin { modules(appModule) }
└── MainActivity.kt           # ComponentActivity, enableEdgeToEdge, hosts NavRoot, reads themeMode
```

### 2.2 Boundary rules

1. `feature/*` cannot import from another `feature/*`. Cross-feature data flows go through navigation arguments or via `core/`.
2. `feature/*` can import from `core/*`.
3. `core/*` cannot import from `feature/*`. **Exception:** `core/data/database/AppDatabase` references feature DAOs (Room limitation — DAOs cannot live in a different module without subprojects).
4. Domain layers (`feature/*/domain/`, `core/domain/`) are pure Kotlin — no `androidx.*`, no `android.*`, no `Context`. Keeps them KMP-reachable and trivially unit-testable.
5. MVI shape per feature: `Screen` (pure) → `Root` (state collector + event handler) → `ViewModel` (state owner) → `Repository` (data) → `Domain` (logic). Per the `android-presentation-mvi` skill convention.
6. **Promotion rule:** a model or interface is promoted to `core/` only when **two or more features** touch it. Otherwise it stays in `feature/<name>/`. YAGNI applied to package boundaries.

### 2.3 Build & toolchain

| Concern | Choice |
|---|---|
| Build DSL | Kotlin DSL (`.kts`) |
| Version catalog | `gradle/libs.versions.toml` |
| Gradle | 9.4.1+ |
| AGP | 9.x (latest stable at implementation time; verify via `dependency-version-lookup`) |
| Kotlin | 2.3.x (current MaterialCalculator pin or newer) |
| Compose plugin | `org.jetbrains.kotlin.plugin.compose` |
| JDK toolchain | `gradle/gradle-daemon-jvm.properties` declarative, JDK 21 |
| Foojay resolver | 1.0.0 |
| compileSdk | 36 (with `minorApiLevel = 1` if AGP 9 syntax is in use) |
| minSdk | 24 (unchanged from Phase 1; raising further drops too many devices) |
| Java compatibility | 17 (kotlinOptions jvmTarget '17') |

---

## 3. Components (per-screen)

### 3.1 Calculator screen — `feature/calculator/`

```kotlin
data class CalculatorState(
    val expression: String = "",
    val errorMessage: UiText? = null,   // null = no error
)

sealed interface CalculatorAction {
    data class Number(val n: Int) : CalculatorAction
    data object Decimal : CalculatorAction
    data class Op(val op: Operation) : CalculatorAction
    data object OpenParen : CalculatorAction
    data object CloseParen : CalculatorAction
    data object Delete : CalculatorAction
    data object Clear : CalculatorAction
    data object Calculate : CalculatorAction
    data object HistoryClicked : CalculatorAction
    data object SettingsClicked : CalculatorAction
}

sealed interface CalculatorEvent {
    data object NavigateToHistory : CalculatorEvent
    data object NavigateToSettings : CalculatorEvent
}
```

**Composable hierarchy:** `CalculatorRoot(viewModel = koinViewModel(), onNavigate)` → `CalculatorScreen(state, onAction)` → uses `components/CalculatorDisplay` + `components/CalculatorButtonGrid` + `components/CalculatorButton`.

**Side effects:** `HapticController` and `SoundController` are injected, hold their own `Flow<AppSettings>` internally. UI calls `haptics.performClick()`; controller decides whether to fire based on current settings. Keeps screen state lean.

**On Calculate:**
1. Evaluate via `ExpressionEvaluator` — returns `Result<Double, CalcError>`.
2. On `Success`: format result, update state, side-write to history via `HistoryRepository.add(...)`.
3. On `Failure`: map `CalcError → UiText`, update `state.errorMessage`. Expression is NOT cleared — user can edit and retry.
4. Next non-Calculate input clears `errorMessage` automatically.

### 3.2 Settings screen — `feature/settings/`

```kotlin
data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
)

enum class ThemeMode { System, Light, Dark }

sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
    data object ToggleHaptics : SettingsAction
    data object ToggleSound : SettingsAction
    data object BackClicked : SettingsAction
}

sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent
}
```

**ViewModel:** observes `SettingsRepository.observe()` into state on init. Mutations call `repo.update { copy(...) }`.

### 3.3 History screen — `feature/history/`

```kotlin
data class HistoryState(
    val entries: List<HistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
)

data class HistoryEntry(
    val id: Long,
    val expression: String,
    val result: String,
    val timestamp: Instant,
)

sealed interface HistoryAction {
    data class DeleteEntry(val id: Long) : HistoryAction
    data object ClearAll : HistoryAction
    data class EntryClicked(val entry: HistoryEntry) : HistoryAction
    data object BackClicked : HistoryAction
}

sealed interface HistoryEvent {
    data class NavigateBackWithExpression(val expression: String) : HistoryEvent
    data object NavigateBack : HistoryEvent
}
```

**UX:** `LazyColumn` of `HistoryItem` composables (expression on top, result on bottom, timestamp small). Swipe-to-delete via M3 `SwipeToDismissBox`. Empty state when list is empty. Tapping an entry navigates back to Calculator with that entry's `result` populated as the starting expression.

### 3.4 Theme system — `core/ui/theme/`

```kotlin
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme, Typography, Shapes, content)
}
```

`MainActivity` collects `themeMode` from `SettingsRepository` once and passes to `AppTheme`. Phase 2.5+ theme directions (Cardboard / Minimalist / Retro / Glassmorphism per the brainstorm doc) extend `ThemeMode`; not built now, architecture allows it.

### 3.5 Reusable components

- Feature-local: `CalculatorButton`, `CalculatorDisplay`, `CalculatorButtonGrid`, `HistoryItem` stay in `feature/<name>/ui/components/`.
- `core/ui/component/` is empty at start. First time something gets duplicated across features, it gets promoted.

---

## 4. Data Flow

### 4.1 MVI cycle inside a feature

```
Screen ──onAction(Action)──► ViewModel ──state: StateFlow──► Root ──renders──► Screen
                                  │
                                  └──events: Channel/Flow──► Root ──onNavigate──► NavRoot
```

`Root` collects state with `collectAsStateWithLifecycle()` and events with `ObserveAsEvents` (a small lifecycle-aware extension in `core/ui/util/`).

### 4.2 Cross-feature contract — where shared models live

| Type | Lives in | Rationale |
|---|---|---|
| `HistoryEntry` | `core/domain/history/` | Calculator writes, History reads |
| `HistoryRepository` interface | `core/data/history/` | Calculator depends on the interface |
| `HistoryRepositoryImpl`, `HistoryEntity`, `HistoryDao` | `feature/history/data/` | Room infra stays with consumer feature |
| `AppSettings`, `ThemeMode` | `core/domain/settings/` | MainActivity, Calculator, Settings UI all read |
| `SettingsRepository` interface | `core/data/settings/` | Same reasoning |
| `SettingsRepositoryImpl` | `feature/settings/data/` | DataStore wiring stays with consumer feature |
| `ExpressionEvaluator`, `ExpressionParser`, `ExpressionWriter`, `Operation` | `feature/calculator/domain/` | Used only by Calculator |

### 4.3 Settings flow (DataStore Preferences)

```kotlin
// core/data/settings/SettingsRepository.kt
interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun update(transform: AppSettings.() -> AppSettings)
}

// feature/settings/data/SettingsRepositoryImpl.kt
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override fun observe(): Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toAppSettings() }
    override suspend fun update(transform: AppSettings.() -> AppSettings) {
        dataStore.edit { prefs -> prefs.fromAppSettings(prefs.toAppSettings().transform()) }
    }
}
```

### 4.4 History flow (Room)

```kotlin
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long,   // Instant.toEpochMilliseconds(), mapped at the boundary
)

@Dao interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun observe(): Flow<List<HistoryEntity>>
    @Insert suspend fun insert(entry: HistoryEntity): Long
    @Query("DELETE FROM history WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM history") suspend fun clear()
}

@Database(entities = [HistoryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
```

### 4.5 Cross-feature seam — Calculator writes to history

`CalculatorViewModel` injects `HistoryRepository` (interface from `core/`). On successful Calculate, calls `historyRepo.add(entry)` inside `viewModelScope.launch`. Because `historyRepo.observe()` is a Flow backed by Room, `HistoryViewModel` (when active) gets the new entry pushed automatically. **Reactive persistence handles the cross-feature wiring for free** — no event-bus, no manual refresh.

### 4.6 History → Calculator return path

1. `HistoryViewModel.onAction(EntryClicked(entry))` → emits `HistoryEvent.NavigateBackWithExpression(entry.result)`.
2. `HistoryRoot` calls a `Navigator` lambda passed in from `NavRoot`, which:
   - Pops history off the back stack.
   - Sets a "pending expression" in Calculator's `SavedStateHandle` via Nav3's `NavKey` saved-state slot.
3. `CalculatorViewModel` reads pending expression from `SavedStateHandle` on init:

```kotlin
init {
    savedStateHandle.get<String>("pending_expression")?.let { pending ->
        _state.update { it.copy(expression = pending) }
        savedStateHandle["pending_expression"] = null
    }
}
```

### 4.7 Coroutine / dispatcher discipline

- `viewModelScope` for all VM coroutines.
- DAO suspend functions run on `Dispatchers.IO` automatically (Room handles it).
- `ExpressionEvaluator` is pure CPU work — runs synchronously on the calling dispatcher.
- No `GlobalScope`, no `runBlocking` outside tests.

---

## 5. Error Handling

### 5.1 Generic Result wrapper (in `core/domain/util/`)

```kotlin
interface Error  // marker

sealed interface Result<out D, out E : Error> {
    data class Success<D>(val data: D) : Result<D, Nothing>
    data class Failure<E : Error>(val error: E) : Result<Nothing, E>

    inline fun <R> map(transform: (D) -> R): Result<R, E> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
}
```

Tiny helpers (`onSuccess`, `onFailure`, `getOrNull`) added as needed. Custom name (not `kotlin.Result`) — import from own package.

### 5.2 Domain-specific errors

```kotlin
// feature/calculator/domain/CalcError.kt
sealed interface CalcError : Error {
    data object DivideByZero : CalcError
    data object Malformed : CalcError
    data object Overflow : CalcError
}
```

`ExpressionEvaluator.evaluate(...)` returns `Result<Double, CalcError>`. Replaces the current single-string `"Error"` sentinel with three distinct typed errors.

### 5.3 UiText for localized error messages (in `core/ui/util/`)

```kotlin
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class FromRes(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is FromRes -> stringResource(id, *args.toTypedArray())
    }
}

// feature/calculator/ui/CalcErrorMapper.kt
fun CalcError.toUiText(): UiText = when (this) {
    CalcError.DivideByZero -> UiText.FromRes(R.string.error_divide_by_zero)
    CalcError.Malformed    -> UiText.FromRes(R.string.error_malformed_expression)
    CalcError.Overflow     -> UiText.FromRes(R.string.error_overflow)
}
```

### 5.4 String resources (en + ar)

```xml
<!-- res/values/strings.xml -->
<string name="error_divide_by_zero">Cannot divide by zero</string>
<string name="error_malformed_expression">Invalid expression</string>
<string name="error_overflow">Result too large</string>

<!-- res/values-ar/strings.xml -->
<string name="error_divide_by_zero">لا يمكن القسمة على صفر</string>
<string name="error_malformed_expression">تعبير غير صالح</string>
<string name="error_overflow">النتيجة كبيرة جدًا</string>
```

Retires the hard-coded `"Error"` sentinel currently spread across three files in MaterialCalculator.

### 5.5 Settings & History — silent best-effort

For non-domain errors (DataStore IO, Room SQLite), no user-facing error state. Use defaults on read failure; log on write failure.

```kotlin
override suspend fun add(entry: HistoryEntry) {
    runCatching { dao.insert(entry.toEntity()) }
        .onFailure { Log.w("HistoryRepo", "Failed to write history entry", it) }
}
```

Trade-off: silent data loss is acceptable for a calculator history feature; would NOT be acceptable for transaction logs etc.

---

## 6. Testing Strategy

### 6.1 Test pyramid

| Layer | Coverage target | Tools |
|---|---|---|
| Domain (pure Kotlin) | 90%+ | JUnit5, AssertK |
| Data (Room, DataStore) | Integration smoke | `Room.inMemoryDatabaseBuilder`, test DataStore via `tempFile`, runTest |
| ViewModels (MVI) | 80%+ | JUnit5, Turbine, Fakes, runTest, `MainDispatcherRule` |
| Compose UI | Spot tests for key flows | `createComposeRule()`, Robot pattern |
| Manual smoke | 3 screens | Hand-test before each release |

### 6.2 Tooling

- **JUnit5** replaces JUnit4. Vintage engine kept for AndroidX runner compat on instrumented if needed.
- **AssertK** replaces Truth. Multiplatform, soft assertions, more idiomatic Kotlin. Aligns with `android-testing` skill convention and the KMP-reachable domain decision. One commit, one find/replace — small switching cost upfront.
- **Turbine** for Flow/StateFlow assertions: `state.test { … }`, `awaitItem()`, exhaustive matching.
- **kotlinx-coroutines-test** for `runTest`, `UnconfinedTestDispatcher`, virtual time.
- **MockK** sparingly — only when fakes don't fit.

### 6.3 Fakes-first philosophy

Default to fakes. `FakeHistoryRepository` and `FakeSettingsRepository` live in `app/src/test/java/.../testfakes/`. Each provides observable state + test-only `seed(...)` helpers.

Fixture builders (defaults for every field):

```kotlin
fun historyEntry(
    id: Long = 1L,
    expression: String = "1+1",
    result: String = "2",
    timestamp: Instant = Instant.fromEpochMilliseconds(0),
) = HistoryEntry(id, expression, result, timestamp)
```

### 6.4 What is NOT tested

- `Theme.kt` (wiring, no logic)
- Koin DI modules — verify graph once via `verifyAll()` in a single `KoinModulesTest`, move on
- Generated Room code
- Pure data classes (auto-generated equals/hashCode)

### 6.5 CI integration (Phase 2 GitHub Actions)

| Trigger | Workflow |
|---|---|
| `pull_request` | Lint + `./gradlew test` (domain + data + VM) |
| `push: development` | Above + Compose UI tests on KVM-accelerated Linux runner |
| `push: staging` | Above + `connectedDebugAndroidTest` via Firebase Test Lab |
| `push: master` | Signed release + Play upload |

---

## 7. Greenfield vs. Current — what changes

This section is the practical output of the exercise: improvements to land in MaterialCalculator. The companion HTML artifact (`docs/explore/greenfield-vs-current.html`) shows these visually side-by-side.

### 7.1 Build / toolchain (mechanical)

| Concern | Current | Greenfield | Effort |
|---|---|---|---|
| Build DSL | Groovy | Kotlin DSL | M (mechanical migration) |
| Version catalog | None | `gradle/libs.versions.toml` | M |
| Gradle | 8.13 | 9.4.1+ | S |
| AGP | 8.13.2 | 9.x | M (breaking changes — `android-agp-kmp-migration` skill) |
| JDK toolchain | foojay only | `gradle-daemon-jvm.properties` declarative | S |
| `compileSdk` syntax | `compileSdk 36` | `compileSdk { version = release(36) { minorApiLevel = 1 } }` | S (AGP 9 feature) |
| Repo scoping | None | `includeGroupByRegex` for performance | S |
| `local.properties` read | Throws on missing | Wraps in `runCatching`, falls back to dummy values for non-release | S |

### 7.2 Architecture (deeper)

| Concern | Current | Greenfield | Effort |
|---|---|---|---|
| Package structure | `domain/` + `presentation/` flat | `core/` + `feature/<name>/` | L |
| Namespace | `com.example.materialcalculator` | `dev.gaddal.<brand>` | M (mechanical rename) |
| State model | Single `expression: String` in VM | Per-feature `State` data class with explicit `errorMessage: UiText?` | M |
| Action surface | Single `CalculatorAction` sealed interface | Same shape, renamed if needed for clarity | S |
| Event surface | None (no nav) | Per-feature `Event` sealed interface for one-shot effects | M |
| DI | None | Koin modules per layer | M |
| Navigation | None | Navigation 3 with NavKey routes | M |

### 7.3 Data layer (new)

| Concern | Current | Greenfield | Effort |
|---|---|---|---|
| Settings persistence | None | DataStore Preferences | M |
| History persistence | None | Room | M |
| Settings UI | None | Settings screen with theme/haptics/sound toggles | M |
| History UI | None | History screen with swipe-to-delete + tap-to-restore | M |

### 7.4 Errors

| Concern | Current | Greenfield | Effort |
|---|---|---|---|
| Calculator error type | `"Error"` string sentinel | `Result<Double, CalcError>` with three typed errors | M |
| Error UI | Hard-coded `"Error"` text | Localized `UiText` with `values-ar/` resources | S (after types are in place) |
| Sentinel scattering | Three files | One `CalcErrorMapper` | S |

### 7.5 Testing

| Concern | Current | Greenfield | Effort |
|---|---|---|---|
| Assertion lib | Truth | AssertK | S (find/replace) |
| ViewModel tests | None | Turbine + Fakes per VM | M |
| Compose UI tests | None | Spot tests via `createComposeRule()` | M |
| Test framework | JUnit4 | JUnit5 | S |

### 7.6 What stays the same

- Material 3 with dynamic color
- Edge-to-edge with `Scaffold` insets
- `enableEdgeToEdge()` in MainActivity
- Three build types (debug/staging/release)
- Signing config + Play App Signing
- minSdk 24, compileSdk/targetSdk 36
- Calculator domain logic (`ExpressionEvaluator`, `ExpressionParser`, `ExpressionWriter`, `Operation`) — moves to `feature/calculator/domain/` but logic stays. Recursive-descent grammar stays. Operator precedence stays. `formatResult` with `Locale.ROOT` stays.

---

## 8. Implementation Phases

The detailed step-by-step plan comes from `superpowers:writing-plans`. Rough phasing for sequencing decisions:

1. **Phase 2.0 — Build modernization.** Migrate Groovy → KTS, add version catalog, bump Gradle 9 + AGP 9, declarative JDK toolchain, fix `local.properties` footgun. Mostly mechanical; no behavior change. Ship in isolation to de-risk the toolchain bump.
2. **Phase 2.1 — Namespace rename.** `com.example.materialcalculator` → `dev.gaddal.<brand>`. Brand decision lands here. Single mechanical pass; no architectural change.
3. **Phase 2.2 — Architecture migration.** Reorganize into `core/` + `feature/calculator/`. Calculator is the only feature initially; settings & history are stubs. Introduce MVI, Koin, Navigation 3. ViewModelTests added as we go.
4. **Phase 2.3 — Settings feature.** DataStore + Settings screen + theme/haptics/sound toggles. `MainActivity` reads `themeMode` for `AppTheme`.
5. **Phase 2.4 — History feature.** Room + History screen + cross-feature seam (Calculator writes, History reads). Tap-to-restore via Nav3 saved-state.
6. **Phase 2.5 — Error typing + i18n.** Replace `"Error"` sentinel with `Result<Double, CalcError>` + `UiText` + `values-ar/` resources.
7. **Phase 2.6 — GitHub Actions CI** + new logo + screenshots (deferred to a separate spec).

Each phase is an independent merge to `development` → `staging` → `master`. No phase blocks the next architecturally; they're sequenced for risk management.

---

## 9. Open Questions

- **Brand name** — `<brand>` placeholder remains until user picks from {Prism Calc, Quantum Compute, Tactile Calc, …other}. Mechanical rename across codebase once chosen.
- **Logo design** — separate visual workstream; out of scope for this spec.
- **Screenshot generation tool** — user has built a custom tool; will brief in Phase 2.6 when screenshots are needed.
- **Theme directions** (Cardboard / Minimalist / Retro / Glassmorphism) — Phase 2.5+ extension; architecture allows it but not built now.

---

## 10. Acceptance criteria for this spec

This spec is "done" (ready to hand to `writing-plans`) when:

1. ✅ Architecture, Components, Data Flow, Error Handling, Testing sections each approved by the user during brainstorming.
2. ⏳ User reviews this written spec and either approves or requests changes.
3. ⏳ Self-review pass — placeholder scan, internal consistency, scope check, ambiguity check (performed by Claude before user review).
