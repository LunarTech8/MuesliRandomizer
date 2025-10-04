# MuesliRandomizer Test Suite

## Test Organization

This test suite provides comprehensive coverage of the MuesliRandomizer application's core domain logic.

### Test Classes

1. **SmokeTest.java**
   - Basic smoke tests to verify fundamental functionality
   - Quick validation that core operations work
   - Runs fast, good for catching major regressions

2. **ArticleEntityTest.java**
   - Comprehensive unit tests for ArticleEntity domain logic
   - Tests multiplier behavior, availability logic, string formatting
   - Validates Type enum behavior and article properties

3. **IngredientEntityTest.java**
   - Unit tests for IngredientEntity functionality
   - Tests ingredient calculations, similar article detection
   - Validates weight calculations and marking behavior

4. **ArticleIngredientIntegrationTest.java**
   - Integration tests showing ArticleEntity and IngredientEntity working together
   - Tests the interactions between articles and ingredients
   - Validates combined behavior and formatting

5. **RandomizationAlgorithmTest.java**
   - Tests for the complex randomization algorithm
   - Validates algorithm constants and article pool management
   - Tests exhaustion logic and availability patterns

### Key Test Patterns

- **German Locale Aware**: Tests expect comma decimal separators (`,` not `.`)
- **Availability Logic**: `isAvailable()` checks multiplier > 0, not selectionsLeft
- **Multiplier Behavior**: `incrementMultiplier()` resets to 0 when reaching MAX_MULTIPLIER (3)
- **Name Comparison**: `isNameTheSame()` requires both name AND brand to match
- **Proper Setup**: All tests properly initialize selectionsLeft to match multiplier values

### Running Tests

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*SmokeTest*"
```

Total Coverage: 41 tests across 5 test classes