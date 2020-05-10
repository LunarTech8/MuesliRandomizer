package com.romanbrunner.apps.mueslirandomizer;


public interface Muesli
{
    String getName();
    int getType();
    float getSpoonWeight();
    float getSugarPercentage();

    void setName(String name);
    void setType(int type);
    void setSpoonWeight(float spoonWeight);
    void setSugarPercentage(float sugarPercentage);
}