package com.romanbrunner.apps.mueslirandomizer;


public interface Ingredient
{
    String getName();
    int getSpoonCount();
    float getWeight();

    void setMuesli(Muesli muesli);
    void setSpoonCount(int spoonCount);
}