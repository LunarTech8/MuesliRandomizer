package com.romanbrunner.apps.mueslirandomizer;


public interface Item
{
    String getName();
    String getBrand();
    int getType();
    float getSpoonWeight();
    float getSugarPercentage();

    void setName(String name);
    void setBrand(String brand);
    void setType(int type);
    void setSpoonWeight(float spoonWeight);
    void setSugarPercentage(float sugarPercentage);
}