# MuesliRandomizer - AI Coding Guidelines

## Architecture Overview
This is a single-activity Android app (`MainActivity.java`) that generates randomized muesli mixtures using a sophisticated selection algorithm. The app uses Android Data Binding extensively and operates in three distinct modes controlled by radio buttons.

## Core Domain Model
- **Article/ArticleEntity**: Represents muesli ingredients with properties like `name`, `brand`, `type` (FILLER, CRUNCHY, TOPPING, TENDER, PUFFY, FLAKY), `spoonWeight`, `sugarPercentage`, and `multiplier` (0x-3x availability)
- **Ingredient/IngredientEntity**: Represents selected ingredients in a generated mix with spoon counts and weights
- **Type enum hierarchy**: `FILLER` and regular types (CRUNCHY, TENDER, PUFFY, FLAKY) use tablespoons; `TOPPING` uses teaspoons

## Three User Modes (UserMode enum)
1. **MIX_MUESLI**: Main randomization interface with size/sugar/topping controls and results list
2. **AVAILABILITY**: Adjust multipliers (0x-3x) for each ingredient's selection frequency
3. **EDIT_ITEMS**: Add/remove ingredients and import/export JSON data

## Randomization Algorithm (lines 1180-1250 in MainActivity)
The core algorithm has nested retry loops with specific constants:
- `MAX_RANDOMIZE_TRIES = 1024`: Attempts with current ingredient pool
- `MAX_FULL_RESET_RANDOMIZE_TRIES = 3`: Full resets when no valid mix found
- Uses exhaustible pools that track `selectionsLeft` and priority systems
- Automatically adds low-sugar filler ingredients to achieve target percentages
- Repopulates exhausted ingredients based on their multiplier values

## Data Binding Patterns
- Heavy use of `<data>` variables in `main_screen.xml` (30+ bound variables)
- Custom binding adapters in `BindingAdapters.java` for visibility (`isVisible`), focusability (`isFocusable`), and double-to-text conversion
- All UI state managed through data binding rather than manual view updates
- Always call `binding.executePendingBindings()` after data changes for Espresso compatibility

## JSON Persistence
- Versioned JSON format with version metadata as first array element
- Export: Creates `JSONArray` with version object followed by article data via `writeToJson()`
- Import: Uses `extractItemsJsonVersion()` to handle format migrations
- Articles serialize using custom byte serialization methods in `ArticleEntity`

## Key Development Patterns
- **State Management**: Complex ingredient pool management with selectable/used/chosen/priority lists
- **Logging**: Extensive `Log.i()` usage for debugging algorithm iterations and state changes
- **Error Handling**: Custom `createErrorAlertDialog()` for validation failures
- **RecyclerView Adapters**: `ArticlesAdapter` and `IngredientsAdapter` with data binding integration
- **Activity Results**: Uses modern `ActivityResultLauncher` for file import/export operations

## Build Configuration
- **Target SDK**: 36, **Min SDK**: 28, **Java 11** compatibility (Note: Gradle requires Java 17+ for newer versions)
- **Dependencies**: AndroidX, Apache Commons Lang3, Gson, Data Binding enabled
- **Signing**: Release builds use custom keystore (path in `build.gradle`)
- **Exclusions**: Kotlin stdlib excluded via `configurations.implementation`
- **Testing**: JUnit 4, Espresso for UI tests, extensive unit test coverage for domain logic

## Testing Approach
- **Unit Tests**: Comprehensive coverage of core domain logic (41 tests across 5 test classes)
  - `SmokeTest.java`: Basic smoke tests for fundamental functionality validation
  - `ArticleEntityTest.java`: Tests article creation, multiplier behavior, availability logic, string formatting
  - `IngredientEntityTest.java`: Tests ingredient calculations, weight/spoon formatting, similarity comparison
  - `ArticleIngredientIntegrationTest.java`: Integration tests for ArticleEntity and IngredientEntity interactions
  - `RandomizationAlgorithmTest.java`: Tests algorithm constants, article pool management, exhaustion logic
- **Instrumented Tests**: UI interactions and data binding
  - `MainActivityInstrumentedTest.java`: Tests user mode switching, data binding updates, RecyclerView setup
- **Key Testing Patterns**:
  - **German Locale Aware**: Tests expect comma decimal separators (`,` not `.`)
  - **Availability Logic**: `isAvailable()` checks `multiplier > 0`, not `selectionsLeft > 0`
  - **Multiplier Behavior**: `incrementMultiplier()` resets to 0 when reaching `MAX_MULTIPLIER` (3)
  - **Name Comparison**: `isNameTheSame()` requires both name AND brand to match
  - Data binding requires `binding.executePendingBindings()` for Espresso compatibility
  - Algorithm constants (`MAX_RANDOMIZE_TRIES = 1024`, `MAX_FULL_RESET_RANDOMIZE_TRIES = 3`) are tested
  - All tests properly initialize `selectionsLeft` to match `multiplier` values

## File Structure Conventions
- Single package: `com.romanbrunner.apps.mueslirandomizer`
- Interface/Implementation pattern: `Article`/`ArticleEntity`, `Ingredient`/`IngredientEntity`
- Custom views: `EditTextWithSuffix` for input controls
- Resources: Three layout files (`main_screen.xml`, `article.xml`, `ingredient.xml`)