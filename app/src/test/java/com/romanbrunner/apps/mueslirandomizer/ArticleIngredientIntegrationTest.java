package com.romanbrunner.apps.mueslirandomizer;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;
import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.Type.*;

/**
 * Integration tests for ArticleEntity and IngredientEntity working together.
 * These tests focus on the interactions between articles and ingredients.
 */
public class ArticleIngredientIntegrationTest {

    private ArticleEntity article;
    private IngredientEntity ingredient;

    @Before
    public void setup() {
        article = new ArticleEntity("Granola", "Brand A", CRUNCHY, 15.0, 0.25);
        article.setMultiplier(2);
        article.setSelectionsLeft(2);

        ingredient = new IngredientEntity(article, 3);
    }

    @Test
    public void testArticleBasics() {
        assertEquals("Granola", article.getName());
        assertEquals("Brand A", article.getBrand());
        assertEquals(CRUNCHY, article.getType());
        assertEquals(15.0, article.getSpoonWeight(), 0.01);
        assertEquals(0.25, article.getSugarPercentage(), 0.01);
        assertEquals(2, article.getMultiplier());
        assertEquals(2, article.getSelectionsLeft());
    }

    @Test
    public void testTypeEnumBasics() {
        assertTrue(CRUNCHY.isRegular());
        assertTrue(TENDER.isRegular());
        assertTrue(PUFFY.isRegular());
        assertTrue(FLAKY.isRegular());
        assertFalse(FILLER.isRegular());
        assertFalse(TOPPING.isRegular());

        assertEquals("Crunchy", CRUNCHY.toString());
        assertEquals("Topping", TOPPING.toString());
    }

    @Test
    public void testSpoonNames() {
        assertEquals("tablespoon", article.getSpoonName());
        assertEquals("tablesp.", article.getSpoonNameShort());

        ArticleEntity toppingArticle = new ArticleEntity("Berries", "Brand", TOPPING, 5.0, 0.3);
        assertEquals("teaspoon", toppingArticle.getSpoonName());
        assertEquals("teasp.", toppingArticle.getSpoonNameShort());
    }

    @Test
    public void testAvailability() {
        assertTrue(article.isAvailable());

        article.decrementSelectionsLeft();
        assertTrue(article.isAvailable()); // Still available because multiplier > 0
        assertEquals(1, article.getSelectionsLeft());

        article.decrementSelectionsLeft();
        assertTrue(article.isAvailable()); // Still available because multiplier > 0
        assertEquals(0, article.getSelectionsLeft());

        // To make unavailable, need to set multiplier to 0
        article.setMultiplier(0);
        assertFalse(article.isAvailable());
    }

    @Test
    public void testIngredientBasics() {
        assertEquals("Granola", ingredient.getName());
        assertEquals("Brand A", ingredient.getBrand());
        assertEquals(3, ingredient.getSpoonCount());
        assertEquals("3 tablespoons", ingredient.getSpoonCountString());
    }

    @Test
    public void testIngredientFormatting() {
        assertEquals("45,0", ingredient.getWeightString());
        assertEquals("25,0", ingredient.getSugarPercentageString());
    }

    @Test
    public void testIngredientSingleSpoon() {
        IngredientEntity singleSpoon = new IngredientEntity(article, 1);
        assertEquals("1 tablespoon", singleSpoon.getSpoonCountString());
    }

    @Test
    public void testMarkAsEmpty() {
        // switchMarkAsEmpty returns new state
        assertTrue(ingredient.switchMarkAsEmpty());  // now marked as empty
        assertFalse(ingredient.switchMarkAsEmpty()); // now not marked as empty
        assertTrue(ingredient.switchMarkAsEmpty());  // now marked as empty again
    }

    @Test
    public void testMultiplierConstraints() {
        ArticleEntity testArticle = new ArticleEntity("Test", "Test", CRUNCHY, 10.0, 0.1);

        assertEquals(1, testArticle.getMultiplier());
        testArticle.incrementMultiplier();
        assertEquals(2, testArticle.getMultiplier());
        testArticle.incrementMultiplier();
        assertEquals(3, testArticle.getMultiplier());

        // Test that it resets to 0 when reaching MAX_MULTIPLIER (3)
        testArticle.incrementMultiplier();
        assertEquals(0, testArticle.getMultiplier());
    }

    @Test
    public void testStringFormatting() {
        assertEquals("15,0", article.getSpoonWeightString());
        assertEquals("25,0", article.getSugarPercentageString());
    }

    @Test
    public void testSimilarArticleDetection() {
        ArticleEntity similarArticle = new ArticleEntity("Granola", "Brand A", TENDER, 10.0, 0.15);
        ArticleEntity differentArticle = new ArticleEntity("Nuts", "Brand A", CRUNCHY, 15.0, 0.25);

        assertTrue(ingredient.isSimilarArticle(article));
        assertTrue(ingredient.isSimilarArticle(similarArticle)); // Same name and brand
        assertFalse(ingredient.isSimilarArticle(differentArticle)); // Different name
    }
}