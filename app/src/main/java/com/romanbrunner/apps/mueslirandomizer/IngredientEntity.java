package com.romanbrunner.apps.mueslirandomizer;

import java.util.Objects;


public class IngredientEntity implements Ingredient
{
    // --------------------
    // Functional code
    // --------------------

    private Article article;
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
    public float getWeight()
    {
        return spoonCount * article.getSpoonWeight();
    }

    @Override
    public float getSugarPercentage()
    {
        return 100 * article.getSugarPercentage();
    }

    @Override
    public void setArticle(Article article)
    {
        this.article = article;
    }

    @Override
    public void setSpoonCount(int spoonCount)
    {
        this.spoonCount = spoonCount;
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