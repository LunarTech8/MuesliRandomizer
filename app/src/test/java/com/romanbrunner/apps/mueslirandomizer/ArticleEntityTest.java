package com.romanbrunner.apps.mueslirandomizer;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;
import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.Type.*;

/**
 * Unit tests for core domain logic of MuesliRandomizer.
 * Tests focus on ArticleEntity business logic, JSON serialization, and Type enum behavior.
 */
public class ArticleEntityTest {

    private ArticleEntity crunchyArticle;
    private ArticleEntity toppingArticle;
    private ArticleEntity fillerArticle;

    @Before
    public void setup() {
        crunchyArticle = new ArticleEntity("Granola", "Brand A", CRUNCHY, 15.0, 0.25);
        crunchyArticle.setMultiplier(2);
        crunchyArticle.setSelectionsLeft(2);

        toppingArticle = new ArticleEntity("Dried Berries", "Brand B", TOPPING, 5.0, 0.35);
        toppingArticle.setMultiplier(1);
        toppingArticle.setSelectionsLeft(1);

        fillerArticle = new ArticleEntity("Oats", "Brand C", FILLER, 20.0, 0.05);
        fillerArticle.setMultiplier(3);
        fillerArticle.setSelectionsLeft(3);
    }

    @Test
    public void testArticleCreation() {
        assertEquals("Granola", crunchyArticle.getName());
        assertEquals("Brand A", crunchyArticle.getBrand());
        assertEquals(CRUNCHY, crunchyArticle.getType());
        assertEquals(15.0, crunchyArticle.getSpoonWeight(), 0.01);
        assertEquals(0.25, crunchyArticle.getSugarPercentage(), 0.01);
        assertEquals(2, crunchyArticle.getMultiplier());
        assertEquals(2, crunchyArticle.getSelectionsLeft()); // Should match multiplier initially
    }

    @Test
    public void testSpoonNameForTypes() {
        assertEquals("tablespoon", crunchyArticle.getSpoonName());
        assertEquals("tablesp.", crunchyArticle.getSpoonNameShort());
        assertEquals("Tablespoon", crunchyArticle.getSpoonNameCapitalized());

        assertEquals("teaspoon", toppingArticle.getSpoonName());
        assertEquals("teasp.", toppingArticle.getSpoonNameShort());
        assertEquals("Teaspoon", toppingArticle.getSpoonNameCapitalized());

        assertEquals("tablespoon", fillerArticle.getSpoonName());
        assertEquals("tablesp.", fillerArticle.getSpoonNameShort());
    }

    @Test
    public void testTypeEnumBehavior() {
        assertTrue(CRUNCHY.isRegular());
        assertTrue(TENDER.isRegular());
        assertTrue(PUFFY.isRegular());
        assertTrue(FLAKY.isRegular());
        assertFalse(FILLER.isRegular());
        assertFalse(TOPPING.isRegular());

        assertEquals("Crunchy", CRUNCHY.toString());
        assertEquals("Topping", TOPPING.toString());
        assertEquals("Filler", FILLER.toString());
    }

    @Test
    public void testAvailabilityLogic() {
        assertTrue(crunchyArticle.isAvailable()); // multiplier = 2

        crunchyArticle.decrementSelectionsLeft();
        assertTrue(crunchyArticle.isAvailable()); // Still available - multiplier still > 0
        assertEquals(1, crunchyArticle.getSelectionsLeft());

        crunchyArticle.decrementSelectionsLeft();
        assertTrue(crunchyArticle.isAvailable()); // Still available - multiplier still > 0
        assertEquals(0, crunchyArticle.getSelectionsLeft());

        // To make unavailable, need to set multiplier to 0
        crunchyArticle.setMultiplier(0);
        assertFalse(crunchyArticle.isAvailable());
    }

    @Test
    public void testMultiplierIncrement() {
        assertEquals(2, crunchyArticle.getMultiplier());
        crunchyArticle.incrementMultiplier();
        assertEquals(3, crunchyArticle.getMultiplier());

        // Test max multiplier constraint
        ArticleEntity maxMultiplierArticle = new ArticleEntity("Test", "Test", CRUNCHY, 10.0, 0.1);
        maxMultiplierArticle.setMultiplier(3);
        maxMultiplierArticle.setSelectionsLeft(3);
        maxMultiplierArticle.incrementMultiplier();
        assertEquals(0, maxMultiplierArticle.getMultiplier()); // Resets to 0 when >= MAX_MULTIPLIER
    }

    @Test
    public void testStringFormatting() {
        assertEquals("15,0", crunchyArticle.getSpoonWeightString());
        assertEquals("25,0", crunchyArticle.getSugarPercentageString());
        assertEquals("35,0", toppingArticle.getSugarPercentageString());
        assertEquals("5,0", fillerArticle.getSugarPercentageString());
    }

    @Test
    public void testNameComparison() {
        ArticleEntity article1 = new ArticleEntity("Granola", "Brand A", CRUNCHY, 15.0, 0.25);
        article1.setMultiplier(2);
        article1.setSelectionsLeft(2);
        ArticleEntity article2 = new ArticleEntity("Granola", "Brand A", TENDER, 10.0, 0.15);
        article2.setMultiplier(1);
        article2.setSelectionsLeft(1);
        ArticleEntity article3 = new ArticleEntity("Nuts", "Brand A", CRUNCHY, 15.0, 0.25);
        article3.setMultiplier(2);
        article3.setSelectionsLeft(2);

        assertTrue(ArticleEntity.isNameTheSame(article1, article2)); // Same name and brand
        assertFalse(ArticleEntity.isNameTheSame(article1, article3)); // Different name
    }

    @Test
    public void testPriorityBehavior() {
        assertFalse(crunchyArticle.getHasPriority());
        crunchyArticle.setHasPriority(true);
        assertTrue(crunchyArticle.getHasPriority());
    }
}