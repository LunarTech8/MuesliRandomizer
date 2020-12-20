package com.romanbrunner.apps.mueslirandomizer;

import static com.romanbrunner.apps.mueslirandomizer.ArticleEntity.*;


public interface Article
{
    String getName();
    String getBrand();
    Type getType();
    double getSpoonWeight();
    double getSugarPercentage();
    int getMultiplier();
    int getSelectionsLeft();
    boolean getIsFiller();

    void setName(String name);
    void setBrand(String brand);
    void setSpoonWeight(double spoonWeight);
    void setSugarPercentage(double sugarPercentage);
    void setMultiplier(int multiplier);
    void setSelectionsLeft(int selectionsLeft);
    void setIsFiller(boolean isFiller);

    boolean isAvailable();
    void incrementMultiplier();
    void decrementSelectionsLeft();
}