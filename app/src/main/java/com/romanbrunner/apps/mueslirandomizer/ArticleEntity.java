package com.romanbrunner.apps.mueslirandomizer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class ArticleEntity implements Article
{
    // --------------------
    // Functional code
    // --------------------

    private String name;
    private String brand;
    private int type;
    private float spoonWeight;
    private float sugarPercentage;
    private boolean available;

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
    public boolean isAvailable()
    {
        return available;
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

    @Override
    public void setAvailable(boolean available)
    {
        this.available = available;
    }

    ArticleEntity(String name, String brand, int type, float spoonWeight, float sugarPercentage)
    {
        this.name = name;
        this.brand = brand;
        this.type = type;
        this.spoonWeight = spoonWeight;
        this.sugarPercentage = sugarPercentage;
        this.available = true;  // DEBUG: make default true as long as storing doesn't work on phone
    }
    @SuppressWarnings("ResultOfMethodCallIgnored")
    ArticleEntity(byte[] dataBytes) throws IOException
    {
        byte[] bytes;
        ByteArrayInputStream dataInputStream = new ByteArrayInputStream(dataBytes);

        bytes = new byte[dataInputStream.read()];
        dataInputStream.read(bytes);
        name = new String(bytes, StandardCharsets.UTF_8);

        bytes = new byte[dataInputStream.read()];
        dataInputStream.read(bytes);
        brand = new String(bytes, StandardCharsets.UTF_8);

        type = dataInputStream.read();

        bytes = new byte[4];
        dataInputStream.read(bytes);
        spoonWeight = ByteBuffer.wrap(bytes).getFloat();

        bytes = new byte[4];
        dataInputStream.read(bytes);
        sugarPercentage = ByteBuffer.wrap(bytes).getFloat();

        available = (dataInputStream.read() != 0);
    }

    static boolean isContentTheSame(Article articleA, Article articleB)
    {
        return Objects.equals(articleA.getName(), articleB.getName())
            && Objects.equals(articleA.getBrand(), articleB.getBrand())
            && articleA.getType() == articleB.getType()
            && Float.compare(articleA.getSpoonWeight(), articleB.getSpoonWeight()) == 0
            && Float.compare(articleA.getSugarPercentage(), articleB.getSugarPercentage()) == 0
            && articleA.isAvailable() == articleB.isAvailable();
    }

    byte[] toByteArray() throws IOException
    {
        byte[] bytes;
        ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();

        bytes = name.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.write(bytes.length);
        dataOutputStream.write(bytes);

        bytes = brand.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.write(bytes.length);
        dataOutputStream.write(bytes);

        dataOutputStream.write(type);

        dataOutputStream.write(ByteBuffer.allocate(4).putFloat(spoonWeight).array());

        dataOutputStream.write(ByteBuffer.allocate(4).putFloat(sugarPercentage).array());

        dataOutputStream.write(available ? 1 : 0);

        return dataOutputStream.toByteArray();
    }
}