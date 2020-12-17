package com.romanbrunner.apps.mueslirandomizer;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class ArticleEntity implements Article
{
    // --------------------
    // Data code
    // --------------------

    private final static int MAX_MULTIPLIER = 3;
    private final static Charset STRING_CHARSET = StandardCharsets.UTF_8;
    private final static int BYTE_BUFFER_LENGTH = 4;

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


    // --------------------
    // Functional code
    // --------------------

    private String name;
    private String brand;
    private Type type;
    private float spoonWeight;
    private float sugarPercentage;
    private int multiplier;  // Quantifier for how often the article has to be chosen before it is used, 0 means unavailable
    private int selectionsLeft;  // Counter for how often the article can still be chosen, 0 means it is used

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
    public int getMultiplier()
    {
        return multiplier;
    }

    @Override
    public int getSelectionsLeft()
    {
        return selectionsLeft;
    }

    @Override
    public boolean getIsFiller()
    {
        return type == Type.FILLER;
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
    public void setMultiplier(int multiplier)
    {
        this.multiplier = multiplier;
    }

    @Override
    public void setSelectionsLeft(int selectionsLeft)
    {
        this.selectionsLeft = selectionsLeft;
    }

    @Override
    public void setIsFiller(boolean isFiller)
    {
        type = (isFiller ? Type.FILLER : Type.REGULAR);
    }

    @Override
    public boolean isAvailable()
    {
        return multiplier > 0;
    }

    @Override
    public void incrementMultiplier()
    {
        if (multiplier >= MAX_MULTIPLIER)
        {
            multiplier = 0;
            selectionsLeft = 0;
        }
        else
        {
            multiplier += 1;
            selectionsLeft += 1;
        }
    }

    @Override
    public void decrementSelectionsLeft()
    {
        if (selectionsLeft > 0)
        {
            selectionsLeft -= 1;
        }
    }

    ArticleEntity(String name, String brand, Type type, float spoonWeight, float sugarPercentage)
    {
        this.name = name;
        this.brand = brand;
        this.type = type;
        this.spoonWeight = spoonWeight;
        this.sugarPercentage = sugarPercentage;
        this.multiplier = 0;
        this.selectionsLeft = 0;
    }
    ArticleEntity(byte[] dataBytes) throws IOException
    {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(dataBytes);
        name = new String(readEntry(inputStream, inputStream.read()), STRING_CHARSET);
        brand = new String(readEntry(inputStream, inputStream.read()), STRING_CHARSET);
        type = Type.fromInt(ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH)).getInt());
        spoonWeight = ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH)).getFloat();
        sugarPercentage = ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH)).getFloat();
        multiplier = ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH)).getInt();
        selectionsLeft = ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH)).getInt();
    }

    private static byte[] readEntry(final ByteArrayInputStream inputStream, final int entryBytesLength) throws IOException
    {
        byte[] entryBytes = new byte[entryBytesLength];
        //noinspection ResultOfMethodCallIgnored
        inputStream.read(entryBytes);
        return entryBytes;
    }

    private static void writeEntry(final ByteArrayOutputStream outputStream, final byte[] entryBytes, final boolean storeLength) throws IOException
    {
        if (storeLength)
        {
            outputStream.write(entryBytes.length);
        }
        outputStream.write(entryBytes);
    }

    static boolean isNameTheSame(Article articleA, Article articleB)
    {
        return Objects.equals(articleA.getName(), articleB.getName())
            && Objects.equals(articleA.getBrand(), articleB.getBrand());
    }

    static boolean isContentTheSame(Article articleA, Article articleB)
    {
        return Objects.equals(articleA.getName(), articleB.getName())
            && Objects.equals(articleA.getBrand(), articleB.getBrand())
            && articleA.getType() == articleB.getType()
            && Float.compare(articleA.getSpoonWeight(), articleB.getSpoonWeight()) == 0
            && Float.compare(articleA.getSugarPercentage(), articleB.getSugarPercentage()) == 0
            && articleA.getSelectionsLeft() == articleB.getSelectionsLeft()
            && articleA.getMultiplier() == articleB.getMultiplier();
    }

    byte[] toByteArray() throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeEntry(outputStream, name.getBytes(STRING_CHARSET), true);
        writeEntry(outputStream, brand.getBytes(STRING_CHARSET), true);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH).putInt(type.ordinal()).array(), false);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH).putFloat(spoonWeight).array(), false);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH).putFloat(sugarPercentage).array(), false);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH).putInt(multiplier).array(), false);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH).putInt(selectionsLeft).array(), false);
        return outputStream.toByteArray();
    }

    JSONObject writeToJson() throws JSONException
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("brand", brand);
        jsonObject.put("type", type);
        jsonObject.put("spoonWeight", spoonWeight);  // TEST: check if this is transformed correctly
        jsonObject.put("sugarPercentage", sugarPercentage);  // TEST: check if this is transformed correctly
        return jsonObject;
    }

    void readFromJson(JSONObject jsonObject) throws JSONException
    {
        this.name = jsonObject.getString("name");
        this.brand = jsonObject.getString("brand");
        this.type = Type.fromInt(jsonObject.getInt("brand"));
        this.spoonWeight = jsonObject.getLong("brand");  // TEST: check if this is transformed correctly
        this.sugarPercentage = jsonObject.getLong("brand");  // TEST: check if this is transformed correctly
        this.multiplier = 0;
        this.selectionsLeft = 0;
    }
}