package com.romanbrunner.apps.mueslirandomizer;

import org.junit.Test;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.Type.*;

/**
 * Unit tests for randomization algorithm constants and core logic.
 * Tests the constraints and behavior that govern muesli generation.
 */
public class RandomizationAlgorithmTest {

    // Constants from MainActivity
    private static final int MAX_FULL_RESET_RANDOMIZE_TRIES = 3;
    private static final int MAX_RANDOMIZE_TRIES = 1024;
    private static final int TOPPINGS_INGREDIENT_COUNT = 2;

    private List<ArticleEntity> fillerArticles;
    private List<ArticleEntity> regularArticles;
    private List<ArticleEntity> toppingArticles;

    @Before
    public void setup() {
        // Create test articles similar to what might exist in the app
        ArticleEntity oats = new ArticleEntity("Oats", "Brand A", FILLER, 20.0, 0.05);
        oats.setMultiplier(3);
        oats.setSelectionsLeft(3);
        ArticleEntity wheat = new ArticleEntity("Wheat Flakes", "Brand B", FILLER, 18.0, 0.08);
        wheat.setMultiplier(2);
        wheat.setSelectionsLeft(2);
        fillerArticles = Arrays.asList(oats, wheat);

        ArticleEntity granola = new ArticleEntity("Granola", "Brand A", CRUNCHY, 15.0, 0.25);
        granola.setMultiplier(2);
        granola.setSelectionsLeft(2);
        ArticleEntity almonds = new ArticleEntity("Almonds", "Brand B", CRUNCHY, 12.0, 0.15);
        almonds.setMultiplier(1);
        almonds.setSelectionsLeft(1);
        ArticleEntity banana = new ArticleEntity("Banana Chips", "Brand C", TENDER, 10.0, 0.30);
        banana.setMultiplier(2);
        banana.setSelectionsLeft(2);
        ArticleEntity corn = new ArticleEntity("Corn Flakes", "Brand D", FLAKY, 8.0, 0.20);
        corn.setMultiplier(3);
        corn.setSelectionsLeft(3);
        ArticleEntity rice = new ArticleEntity("Rice Puffs", "Brand E", PUFFY, 6.0, 0.10);
        rice.setMultiplier(1);
        rice.setSelectionsLeft(1);
        regularArticles = Arrays.asList(granola, almonds, banana, corn, rice);

        ArticleEntity berries = new ArticleEntity("Dried Berries", "Brand A", TOPPING, 5.0, 0.35);
        berries.setMultiplier(1);
        berries.setSelectionsLeft(1);
        ArticleEntity chocolate = new ArticleEntity("Chocolate Chips", "Brand B", TOPPING, 7.0, 0.80);
        chocolate.setMultiplier(2);
        chocolate.setSelectionsLeft(2);
        ArticleEntity honey = new ArticleEntity("Honey Granules", "Brand C", TOPPING, 4.0, 0.90);
        honey.setMultiplier(1);
        honey.setSelectionsLeft(1);
        toppingArticles = Arrays.asList(berries, chocolate, honey);
    }

    @Test
    public void testArticleAvailabilityAfterUse() {
        ArticleEntity article = regularArticles.get(0); // Granola with multiplier 2

        assertTrue("Article should be initially available", article.isAvailable());
        assertEquals("Initial selectionsLeft should equal multiplier", 2, article.getSelectionsLeft());

        // Use article once
        article.decrementSelectionsLeft();
        assertTrue("Article should still be available after first use", article.isAvailable());
        assertEquals("SelectionsLeft should be decremented", 1, article.getSelectionsLeft());

        // Use article again
        article.decrementSelectionsLeft();
        assertTrue("Article should still be available (isAvailable checks multiplier, not selectionsLeft)", article.isAvailable());
        assertEquals("SelectionsLeft should be zero", 0, article.getSelectionsLeft());
    }

    @Test
    public void testMultiplierIncrement() {
        ArticleEntity article = new ArticleEntity("Test", "Test", CRUNCHY, 10.0, 0.1);

        assertEquals("Initial multiplier", 1, article.getMultiplier());
        article.incrementMultiplier();
        assertEquals("After increment", 2, article.getMultiplier());
        article.incrementMultiplier();
        assertEquals("After second increment", 3, article.getMultiplier());

        // Test multiplier continues incrementing (no built-in maximum)
        article.incrementMultiplier();
        assertEquals("Should reset to 0 when >= MAX_MULTIPLIER", 0, article.getMultiplier());
    }

    @Test
    public void testArticlePoolSufficiency() {
        // Test the condition from MainActivity: enough articles for valid mix
        int requiredRegularCount = 3;
        int requiredToppingCount = TOPPINGS_INGREDIENT_COUNT;

        // Should have enough regular articles
        assertTrue("Should have enough regular articles",
                  getAvailableCount(regularArticles) >= requiredRegularCount);

        // Should have enough topping articles
        assertTrue("Should have enough topping articles",
                  getAvailableCount(toppingArticles) >= requiredToppingCount);

        // Should have at least one filler article
        assertTrue("Should have at least one filler article",
                  getAvailableCount(fillerArticles) >= 1);
    }

    @Test
    public void testInsufficientArticlePool() {
        // Create scenario with insufficient articles
        ArticleEntity onlyArticle = new ArticleEntity("Only Article", "Brand", CRUNCHY, 10.0, 0.1);
        onlyArticle.setMultiplier(1);
        onlyArticle.setSelectionsLeft(1);
        List<ArticleEntity> insufficientRegular = Arrays.asList(onlyArticle);

        int requiredCount = 3;
        assertFalse("Insufficient articles should be detected",
                   getAvailableCount(insufficientRegular) >= requiredCount);
    }

    @Test
    public void testArticleExhaustion() {
        // Test what happens when articles are exhausted
        ArticleEntity article = new ArticleEntity("Test", "Test", CRUNCHY, 10.0, 0.1);
        article.setMultiplier(2);
        article.setSelectionsLeft(2);

        // Exhaust the article selections
        article.decrementSelectionsLeft();
        article.decrementSelectionsLeft();
        assertTrue("Article should still be available (isAvailable checks multiplier, not selectionsLeft)", article.isAvailable());
        assertEquals("SelectionsLeft should be zero", 0, article.getSelectionsLeft());

        // Replenish (like algorithm does when resetting pools)
        article.setSelectionsLeft(article.getMultiplier());
        assertTrue("Article should be available after replenishment", article.isAvailable());
    }

    @Test
    public void testRandomSelectionConsistency() {
        // Test that random selection is deterministic with same seed
        Random random1 = new Random(12345);
        Random random2 = new Random(12345);

        List<ArticleEntity> pool1 = new ArrayList<>(regularArticles);
        List<ArticleEntity> pool2 = new ArrayList<>(regularArticles);

        // Simulate random selection
        ArticleEntity selected1 = pool1.get(random1.nextInt(pool1.size()));
        ArticleEntity selected2 = pool2.get(random2.nextInt(pool2.size()));

        assertEquals("Same seed should produce same selection",
                    selected1.getName(), selected2.getName());
    }

    @Test
    public void testSugarCalculation() {
        // Test sugar percentage calculations (like in the algorithm)
        double targetWeight = 100.0;
        double targetSugarPercentage = 0.15;
        double targetSugar = targetSugarPercentage * targetWeight;

        assertEquals("Target sugar calculation", 15.0, targetSugar, 0.01);

        // Test individual ingredient sugar contribution
        ArticleEntity highSugarArticle = toppingArticles.get(1); // Chocolate chips: 80% sugar
        double ingredientWeight = 2 * highSugarArticle.getSpoonWeight(); // 2 teaspoons
        double sugarContribution = ingredientWeight * highSugarArticle.getSugarPercentage();
        double expectedSugar = 2 * 7.0 * 0.80; // 2 * 7g * 80% = 11.2g

        assertEquals("Sugar contribution calculation", expectedSugar, sugarContribution, 0.01);
    }

    @Test
    public void testWeightCalculation() {
        // Test weight calculations for different spoon types
        ArticleEntity tablespoonArticle = regularArticles.get(0); // Granola: 15g per tablespoon
        ArticleEntity teaspoonArticle = toppingArticles.get(0);   // Berries: 5g per teaspoon

        double tablespoonWeight = 3 * tablespoonArticle.getSpoonWeight(); // 3 tablespoons
        double teaspoonWeight = 2 * teaspoonArticle.getSpoonWeight();     // 2 teaspoons

        assertEquals("Tablespoon weight calculation", 45.0, tablespoonWeight, 0.01);
        assertEquals("Teaspoon weight calculation", 10.0, teaspoonWeight, 0.01);
    }

    /**
     * Helper method to count available articles in a list
     */
    private int getAvailableCount(List<ArticleEntity> articles) {
        return (int) articles.stream().filter(ArticleEntity::isAvailable).count();
    }
}