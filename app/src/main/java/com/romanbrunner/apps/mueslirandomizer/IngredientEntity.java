package com.romanbrunner.apps.mueslirandomizer;

import java.util.Locale;
import java.util.Objects;


public class IngredientEntity implements Ingredient
{
    // --------------------
    // Functional code
    // --------------------

    private final Article article;
    private int spoonCount;

    @Override
    public String getName()
    {
        return article.getName();
    }

    @Override
    public String getBrand()
    {
        return article.getBrand();
    }

    @Override
    public int getSpoonCount()
    {
        return spoonCount;
    }

    @Override
    public String getWeightString()
    {
        return String.format(Locale.getDefault(), "%.1f", spoonCount * article.getSpoonWeight());
    }

    @Override
    public String getSugarPercentageString()
    {
        return String.format(Locale.getDefault(), "%.1f", article.getSugarPercentage() * 100);
    }

    @Override
    public void setSpoonCount(int spoonCount)
    {
        this.spoonCount = spoonCount;
    }

    @Override
    public void markAsEmpty()
    {
        this.article.setMultiplier(0);
    }

    IngredientEntity(Article article, int spoonCount)
    {
        this.article = article;
        this.spoonCount = spoonCount;
    }

    static boolean isContentTheSame(Ingredient ingredientA, Ingredient ingredientB)
    {
        return Objects.equals(ingredientA.getName(), ingredientB.getName())
            && ingredientA.getSpoonCount() == ingredientB.getSpoonCount();
    }
}