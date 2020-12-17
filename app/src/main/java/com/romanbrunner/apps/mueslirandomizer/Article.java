package com.romanbrunner.apps.mueslirandomizer;

import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.*;


public interface Article
{
    String getName();
    String getBrand();
    Type getType();
    float getSpoonWeight();
    float getSugarPercentage();
    int getMultiplier();
    int getSelectionsLeft();
    boolean getIsFiller();

    void setName(String name);
    void setBrand(String brand);
    void setSpoonWeight(float spoonWeight);
    void setSugarPercentage(float sugarPercentage);
    void setMultiplier(int multiplier);
    void setSelectionsLeft(int selectionsLeft);
    void setIsFiller(boolean isFiller);

    boolean isAvailable();
    void incrementMultiplier();
    void decrementSelectionsLeft();
}