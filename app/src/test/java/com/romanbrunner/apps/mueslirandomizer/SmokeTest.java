package com.romanbrunner.apps.mueslirandomizer;

import org.junit.Test;

import static org.junit.Assert.*;
import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.Type.*;

/**
 * Smoke tests to verify basic functionality works.
 * These tests validate the most fundamental operations.
 */
public class SmokeTest {

    @Test
    public void testBasicArticleCreation() {
        ArticleEntity article = new ArticleEntity("Test", "Brand", CRUNCHY, 15.0, 0.25);

        // Basic properties
        assertEquals("Test", article.getName());
        assertEquals("Brand", article.getBrand());
        assertEquals(CRUNCHY, article.getType());
        assertEquals(15.0, article.getSpoonWeight(), 0.01);
        assertEquals(0.25, article.getSugarPercentage(), 0.01);

        // Default values
        assertEquals(1, article.getMultiplier());
        assertEquals(1, article.getSelectionsLeft());
        assertTrue(article.isAvailable());
    }

    @Test
    public void testBasicIngredient() {
        ArticleEntity article = new ArticleEntity("Test", "Brand", CRUNCHY, 15.0, 0.25);
        IngredientEntity ingredient = new IngredientEntity(article, 2);

        assertEquals("Test", ingredient.getName());
        assertEquals("Brand", ingredient.getBrand());
        assertEquals(2, ingredient.getSpoonCount());
    }

    @Test
    public void testTypeEnum() {
        assertEquals("Crunchy", CRUNCHY.toString());
        assertTrue(CRUNCHY.isRegular());
        assertFalse(TOPPING.isRegular());
    }
}