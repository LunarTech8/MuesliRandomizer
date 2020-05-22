package com.romanbrunner.apps.mueslirandomizer;


import java.util.Objects;

public class IngredientEntity implements Ingredient
{
    // --------------------
    // Functional code
    // --------------------

    private Item item;
    private int spoonCount;

    @Override
    public String getName()
    {
        return item.getName();
    }

    @Override
    public String getBrand()
    {
        return item.getBrand();
    }

    @Override
    public int getSpoonCount()
    {
        return spoonCount;
    }

    @Override
    public float getWeight()
    {
        return spoonCount * item.getSpoonWeight();
    }

    @Override
    public float getSugarPercentage()
    {
        return 100 * item.getSugarPercentage();
    }

    @Override
    public void setItem(Item item)
    {
        this.item = item;
    }

    @Override
    public void setSpoonCount(int spoonCount)
    {
        this.spoonCount = spoonCount;
    }

    IngredientEntity(Item item, int spoonCount)
    {
        this.item = item;
        this.spoonCount = spoonCount;
    }

    static boolean isContentTheSame(Ingredient ingredientA, Ingredient ingredientB)
    {
        return Objects.equals(ingredientA.getName(), ingredientB.getName())
                && ingredientA.getSpoonCount() == ingredientB.getSpoonCount();
    }
}