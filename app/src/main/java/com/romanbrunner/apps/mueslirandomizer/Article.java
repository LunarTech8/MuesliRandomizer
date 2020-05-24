package com.romanbrunner.apps.mueslirandomizer;

import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.*;


public interface Article
{
    String getName();
    String getBrand();
    Type getType();
    float getSpoonWeight();
    float getSugarPercentage();
    State getState();
    boolean isAvailable();

    void setName(String name);
    void setBrand(String brand);
    void setType(Type type);
    void setSpoonWeight(float spoonWeight);
    void setSugarPercentage(float sugarPercentage);
    void setState(State state);
    void setAvailable(boolean available);
}