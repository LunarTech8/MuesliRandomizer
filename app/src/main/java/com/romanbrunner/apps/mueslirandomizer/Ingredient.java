package com.romanbrunner.apps.mueslirandomizer;


public interface Ingredient
{
    String getName();
    String getBrand();
    int getSpoonCount();
    String getSpoonCountString();
    String getWeightString();
    String getSugarPercentageString();

    void markAsEmpty();
}