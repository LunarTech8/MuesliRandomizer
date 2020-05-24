package com.romanbrunner.apps.mueslirandomizer;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class ArticleEntity implements Article
{
    // --------------------
    // Data code
    // --------------------

    public enum Type
    {
        FILLER, REGULAR;

        private static final Type[] values = Type.values();

        public static Type fromInt(int intValue)
        {
            if (intValue < 0 || intValue >= values.length)
            {
                Log.e("Type", "Invalid intValue (" + intValue + " has to be at least 0 and smaller than " + values.length + ")");
            }

            return values[intValue];
        }
    }

    public enum State
    {
        UNAVAILABLE, SELECTABLE, USED;

        private static final State[] values = State.values();

        public static State fromInt(int intValue)
        {
            if (intValue < 0 || intValue >= values.length)
            {
                Log.e("State", "Invalid intValue (" + intValue + " has to be at least 0 and smaller than " + values.length + ")");
            }

            return values[intValue];
        }
    }


    // --------------------
    // Functional code
    // --------------------

    private String name;
    private String brand;
    private Type type;
    private float spoonWeight;
    private float sugarPercentage;
    private State state;

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
    public Type getType()
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
    public State getState()
    {
        return state;
    }

    @Override
    public boolean isAvailable()
    {
        return (state != State.UNAVAILABLE);
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
    public void setType(Type type)
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
    public void setState(State state)
    {
        this.state = state;
    }

    @Override
    public void setAvailable(boolean available)
    {
        if (available && this.state == State.UNAVAILABLE)
        {
            this.state = State.SELECTABLE;
        }
        else
        {
            this.state = State.UNAVAILABLE;
        }
    }

    ArticleEntity(String name, String brand, Type type, float spoonWeight, float sugarPercentage)
    {
        this.name = name;
        this.brand = brand;
        this.type = type;
        this.spoonWeight = spoonWeight;
        this.sugarPercentage = sugarPercentage;
        this.state = State.UNAVAILABLE;
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

        type = Type.fromInt(dataInputStream.read());

        bytes = new byte[4];
        dataInputStream.read(bytes);
        spoonWeight = ByteBuffer.wrap(bytes).getFloat();

        bytes = new byte[4];
        dataInputStream.read(bytes);
        sugarPercentage = ByteBuffer.wrap(bytes).getFloat();

        state = State.fromInt(dataInputStream.read());
    }

    static boolean isContentTheSame(Article articleA, Article articleB)
    {
        return Objects.equals(articleA.getName(), articleB.getName())
            && Objects.equals(articleA.getBrand(), articleB.getBrand())
            && articleA.getType() == articleB.getType()
            && Float.compare(articleA.getSpoonWeight(), articleB.getSpoonWeight()) == 0
            && Float.compare(articleA.getSugarPercentage(), articleB.getSugarPercentage()) == 0
            && articleA.getState() == articleB.getState();
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

        dataOutputStream.write(type.ordinal());

        dataOutputStream.write(ByteBuffer.allocate(4).putFloat(spoonWeight).array());

        dataOutputStream.write(ByteBuffer.allocate(4).putFloat(sugarPercentage).array());

        dataOutputStream.write(state.ordinal());

        return dataOutputStream.toByteArray();
    }
}