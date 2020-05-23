package com.romanbrunner.apps.mueslirandomizer;


public interface Article
{
    String getName();
    String getBrand();
    int getType();
    float getSpoonWeight();
    float getSugarPercentage();
    boolean isAvailable();

    void setName(String name);
    void setBrand(String brand);
    void setType(int type);
    void setSpoonWeight(float spoonWeight);
    void setSugarPercentage(float sugarPercentage);
    void setAvailable(boolean available);
}