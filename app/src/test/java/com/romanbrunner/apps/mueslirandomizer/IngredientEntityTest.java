package com.romanbrunner.apps.mueslirandomizer;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;
import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.Type.*;

/**
 * Unit tests for IngredientEntity.
 * Tests the ingredient representation in generated muesli mixes.
 */
public class IngredientEntityTest {

    private ArticleEntity crunchyArticle;
    private ArticleEntity toppingArticle;
    private IngredientEntity crunchyIngredient;
    private IngredientEntity toppingIngredient;

    @Before
    public void setup() {
        crunchyArticle = new ArticleEntity("Granola", "Brand A", CRUNCHY, 15.0, 0.25);
        crunchyArticle.setMultiplier(2);
        crunchyArticle.setSelectionsLeft(2);

        toppingArticle = new ArticleEntity("Dried Berries", "Brand B", TOPPING, 5.0, 0.35);
        toppingArticle.setMultiplier(1);
        toppingArticle.setSelectionsLeft(1);

        crunchyIngredient = new IngredientEntity(crunchyArticle, 3);
        toppingIngredient = new IngredientEntity(toppingArticle, 2);
    }

    @Test
    public void testIngredientCreation() {
        assertEquals("Granola", crunchyIngredient.getName());
        assertEquals("Brand A", crunchyIngredient.getBrand());
        assertEquals(3, crunchyIngredient.getSpoonCount());
    }

    @Test
    public void testSpoonCountStrings() {
        // Single spoon
        IngredientEntity singleSpoonIngredient = new IngredientEntity(crunchyArticle, 1);
        assertEquals("1 tablespoon", singleSpoonIngredient.getSpoonCountString());

        // Multiple spoons
        assertEquals("3 tablespoons", crunchyIngredient.getSpoonCountString());
        assertEquals("2 teaspoons", toppingIngredient.getSpoonCountString());
    }

    @Test
    public void testWeightCalculation() {
        // Crunchy: 3 spoons * 15.0g per spoon = 45.0 (no unit in actual implementation)
        String expectedCrunchyWeight = String.format(java.util.Locale.getDefault(), "%.1f", 3 * 15.0);
        assertEquals(expectedCrunchyWeight, crunchyIngredient.getWeightString());

        // Topping: 2 spoons * 5.0g per spoon = 10.0 (no unit in actual implementation)
        String expectedToppingWeight = String.format(java.util.Locale.getDefault(), "%.1f", 2 * 5.0);
        assertEquals(expectedToppingWeight, toppingIngredient.getWeightString());
    }

    @Test
    public void testSugarPercentageString() {
        // Should format sugar percentage from article (no % unit in actual implementation)
        assertEquals("25,0", crunchyIngredient.getSugarPercentageString());
        assertEquals("35,0", toppingIngredient.getSugarPercentageString());
    }

    @Test
    public void testSimilarArticleComparison() {
        ArticleEntity similarArticle = new ArticleEntity("Granola", "Brand A", TENDER, 10.0, 0.15);
        similarArticle.setMultiplier(1);
        similarArticle.setSelectionsLeft(1);
        ArticleEntity differentArticle = new ArticleEntity("Nuts", "Brand A", CRUNCHY, 15.0, 0.25);
        differentArticle.setMultiplier(2);
        differentArticle.setSelectionsLeft(2);

        assertTrue(crunchyIngredient.isSimilarArticle(crunchyArticle)); // Same article
        assertTrue(crunchyIngredient.isSimilarArticle(similarArticle)); // Same name and brand
        assertFalse(crunchyIngredient.isSimilarArticle(differentArticle)); // Different name
    }

    @Test
    public void testMarkAsEmptyToggle() {
        // Initially not marked as empty, switchMarkAsEmpty returns new state
        assertTrue(crunchyIngredient.switchMarkAsEmpty());  // Returns new state (true)
        assertFalse(crunchyIngredient.switchMarkAsEmpty()); // Returns new state (false)
        assertTrue(crunchyIngredient.switchMarkAsEmpty());  // Returns new state (true)
    }

    @Test
    public void testZeroSpoonCount() {
        IngredientEntity zeroSpoonIngredient = new IngredientEntity(crunchyArticle, 0);
        assertEquals("0 tablespoons", zeroSpoonIngredient.getSpoonCountString());
        assertEquals("0,0", zeroSpoonIngredient.getWeightString());
    }

    @Test
    public void testNegativeSpoonCount() {
        // Edge case: negative spoon count (shouldn't normally occur)
        IngredientEntity negativeSpoonIngredient = new IngredientEntity(crunchyArticle, -1);
        assertEquals("-1 tablespoons", negativeSpoonIngredient.getSpoonCountString());
        String expectedWeight = String.format(java.util.Locale.getDefault(), "%.1f", -1 * 15.0);
        assertEquals(expectedWeight, negativeSpoonIngredient.getWeightString());
    }

    @Test
    public void testHighSpoonCount() {
        // Test with larger spoon counts
        IngredientEntity manySpoonIngredient = new IngredientEntity(crunchyArticle, 10);
        assertEquals("10 tablespoons", manySpoonIngredient.getSpoonCountString());
        String expectedWeight = String.format(java.util.Locale.getDefault(), "%.1f", 10 * 15.0);
        assertEquals(expectedWeight, manySpoonIngredient.getWeightString());
    }
}