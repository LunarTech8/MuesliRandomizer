package com.romanbrunner.apps.mueslirandomizer;


public interface Ingredient
{
    String getName();
    String getBrand();
    int getSpoonCount();
    String getWeightString();
    String getSugarPercentageString();

    void setSpoonCount(int spoonCount);
    void markAsEmpty();
}