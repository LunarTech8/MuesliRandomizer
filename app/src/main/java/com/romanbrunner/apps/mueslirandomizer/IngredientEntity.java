package com.romanbrunner.apps.mueslirandomizer;

import android.util.Log;

import java.util.Locale;
import java.util.Objects;


public class IngredientEntity implements Ingredient
{
    // --------------------
    // Functional code
    // --------------------

    private final ArticleEntity article;
    private final int spoonCount;
    private boolean isMarkedAsEmpty;

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
    public String getSpoonCountString()
    {
        if (spoonCount == 1)
        {
            return "1 " + article.getSpoonName();
        }
        else
        {
            return String.format(Locale.getDefault(), "%d " + article.getSpoonName() + "s", spoonCount);
        }
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
    public boolean switchMarkAsEmpty()
    {
        return this.isMarkedAsEmpty = !this.isMarkedAsEmpty;
    }

    @Override
    public void onRemoval()
    {
        Log.d("onRemoval", this.article.getName());  // DEBUG:
        if (this.isMarkedAsEmpty)
        {
            this.article.setMultiplier(0);
        }
    }

    IngredientEntity(ArticleEntity article, int spoonCount)
    {
        this.article = article;
        this.spoonCount = spoonCount;
        this.isMarkedAsEmpty = false;
    }

    static boolean isContentTheSame(Ingredient ingredientA, Ingredient ingredientB)
    {
        return Objects.equals(ingredientA.getName(), ingredientB.getName())
            && ingredientA.getSpoonCount() == ingredientB.getSpoonCount();
    }
}