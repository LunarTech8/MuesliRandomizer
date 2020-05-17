package com.romanbrunner.apps.mueslirandomizer;


public interface Ingredient
{
    String getName();
    int getSpoonCount();
    float getWeight();
    float getSugarPercentage();

    void setItem(Item item);
    void setSpoonCount(int spoonCount);
}