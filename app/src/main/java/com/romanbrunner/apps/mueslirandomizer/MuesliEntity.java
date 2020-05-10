package com.romanbrunner.apps.mueslirandomizer;


public class MuesliEntity implements Muesli
{
    // --------------------
    // Functional code
    // --------------------

    private String name;
    private int type;
    private float spoonWeight;
    private float sugarPercentage;

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int getType()
    {
        return type;
    }

    @Override
    public float getSpoonWeight()
    {
        return spoonWeight;
    }

    @Override
    public float getSugarPercentage()
    {
        return sugarPercentage;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public void setType(int type)
    {
        this.type = type;
    }

    @Override
    public void setSpoonWeight(float spoonWeight)
    {
        this.spoonWeight = spoonWeight;
    }

    @Override
    public void setSugarPercentage(float sugarPercentage)
    {
        this.sugarPercentage = sugarPercentage;
    }

    public MuesliEntity(String name, int type, float spoonWeight, float sugarPercentage)
    {
        this.name = name;
        this.type = type;
        this.spoonWeight = spoonWeight;
        this.sugarPercentage = sugarPercentage;
    }
}