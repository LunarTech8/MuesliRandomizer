package com.romanbrunner.apps.mueslirandomizer;


import java.util.Objects;

public class IngredientEntity implements Ingredient
{
    // --------------------
    // Functional code
    // --------------------

    private Muesli muesli;
    private int spoonCount;

    @Override
    public String getName()
    {
        return muesli.getName();
    }

    @Override
    public int getSpoonCount()
    {
        return spoonCount;
    }

    @Override
    public float getWeight()
    {
        return spoonCount * muesli.getSpoonWeight();
    }

    @Override
    public void setMuesli(Muesli muesli)
    {
        this.muesli = muesli;
    }

    @Override
    public void setSpoonCount(int spoonCount)
    {
        this.spoonCount = spoonCount;
    }

    public IngredientEntity(Muesli muesli, int spoonCount)
    {
        this.muesli = muesli;
        this.spoonCount = spoonCount;
    }

    static boolean isContentTheSame(Ingredient ingredientA, Ingredient ingredientB)
    {
        return Objects.equals(ingredientA.getName(), ingredientB.getName())
                && ingredientA.getSpoonCount() == ingredientB.getSpoonCount();
    }
}