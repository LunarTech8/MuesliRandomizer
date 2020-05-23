package com.romanbrunner.apps.mueslirandomizer;


public interface Ingredient
{
    String getName();
    String getBrand();
    int getSpoonCount();
    float getWeight();
    float getSugarPercentage();

    void setArticle(Article article);
    void setSpoonCount(int spoonCount);
}