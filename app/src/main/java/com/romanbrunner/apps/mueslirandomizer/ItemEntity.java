package com.romanbrunner.apps.mueslirandomizer;


public class ItemEntity implements Item
{
    // --------------------
    // Functional code
    // --------------------

    private String name;
    private String brand;
    private int type;
    private float spoonWeight;
    private float sugarPercentage;

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getBrand()
    {
        return brand;
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
    public void setBrand(String brand)
    {
        this.brand = brand;
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

    ItemEntity(String name, String brand, int type, float spoonWeight, float sugarPercentage)
    {
        this.name = name;
        this.brand = brand;
        this.type = type;
        this.spoonWeight = spoonWeight;
        this.sugarPercentage = sugarPercentage;
    }
}