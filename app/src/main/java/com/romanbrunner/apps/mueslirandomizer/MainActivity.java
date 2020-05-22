package com.romanbrunner.apps.mueslirandomizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.romanbrunner.apps.mueslirandomizer.databinding.MainScreenBinding;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;


public class MainActivity extends AppCompatActivity
{
    // --------------------
    // Data code
    // --------------------

    private final static float FILLER_INGREDIENT_RATIO = 0.5F;
    private final static int MAX_RANDOMIZE_TRIES = 1024;
    private final static String ITEMS_FILENAME = "AllItems";
    private final static int ITEM_TYPE_FILLER = 0;
    private final static int ITEM_TYPE_SELECTABLE_REGULAR = 1;
    private final static int ITEM_TYPE_USED_REGULAR = 2;

    private static void addDefaultItemsToList(List<ItemEntity> itemList)
    {
        // Very low sugar filler muesli:
        itemList.add(new ItemEntity("Echte Kölln Kernige", "Kölln", ITEM_TYPE_FILLER, 8F, 0.012F));

        // Low sugar regular muesli:
        itemList.add(new ItemEntity("Nuss & Krokant", "Kölln", ITEM_TYPE_SELECTABLE_REGULAR, 9.5F, 0.077F));
        // Medium sugar regular muesli:
        itemList.add(new ItemEntity("Superfood Crunchy Müsli Cacao & Nuts", "Kellogg", ITEM_TYPE_SELECTABLE_REGULAR, 10.5F, 0.14F));
        itemList.add(new ItemEntity("Schokomüsli Feinherb", "Vitalis", ITEM_TYPE_SELECTABLE_REGULAR, 9.5F, 0.15F));
        itemList.add(new ItemEntity("Joghurtmüsli mit Erdbeer-Stücken", "Vitalis", ITEM_TYPE_SELECTABLE_REGULAR, 9F, 0.13F));
        itemList.add(new ItemEntity("Schoko 30% weniger Zucker", "Kölln", ITEM_TYPE_SELECTABLE_REGULAR, 10F, 0.13F));
        // High sugar regular muesli:
        itemList.add(new ItemEntity("Nesquik Knusper-Müsli", "Nestle", ITEM_TYPE_SELECTABLE_REGULAR, 8F, 0.21F));
        itemList.add(new ItemEntity("Crunchy Müsli Red Berries", "Kellogg", ITEM_TYPE_SELECTABLE_REGULAR, 9.5F, 0.22F));
        itemList.add(new ItemEntity("Knuspermüsli Nuss-Nougat", "Vitalis", ITEM_TYPE_SELECTABLE_REGULAR, 11.5F, 0.25F));
        itemList.add(new ItemEntity("Knuspermüsli Plus Nuss Mischung", "Vitalis", ITEM_TYPE_SELECTABLE_REGULAR, 13.5F, 0.2F));
        itemList.add(new ItemEntity("Knusper Beere & Schoko", "Kölln", ITEM_TYPE_SELECTABLE_REGULAR, 11.5F, 0.24F));
        itemList.add(new ItemEntity("Knusper Schoko-Krokant", "Kölln", ITEM_TYPE_SELECTABLE_REGULAR, 11.5F, 0.22F));
        itemList.add(new ItemEntity("Knusper Schoko & Kaffee", "Kölln", ITEM_TYPE_SELECTABLE_REGULAR, 12F, 0.22F));
        itemList.add(new ItemEntity("Knusper Schoko & Keks", "Kölln", ITEM_TYPE_SELECTABLE_REGULAR, 11.5F, 0.21F));
        itemList.add(new ItemEntity("Knusper Joghurt-Honig", "Kölln", ITEM_TYPE_SELECTABLE_REGULAR, 11F, 0.2F));
        itemList.add(new ItemEntity("Knusprige Haferkissen Zimt", "Kölln", ITEM_TYPE_SELECTABLE_REGULAR, 4.5F, 0.2F));
        itemList.add(new ItemEntity("Knusper Schoko Feinherb 30% weniger Fett", "Kölln", ITEM_TYPE_SELECTABLE_REGULAR, 12F, 0.2F));
        itemList.add(new ItemEntity("Porridge Dreierlei Beere", "3 Bears", ITEM_TYPE_SELECTABLE_REGULAR, 12.5F, 0.22F));
        itemList.add(new ItemEntity("Porridge Zimtiger Apfel", "3 Bears", ITEM_TYPE_SELECTABLE_REGULAR, 11F, 0.2F));
    }

    private static float sizeValue2SizeWeight(int sizeValue)
    {
        return 35F + 15F * sizeValue;
    }

    private static float sugarValue2SugarPercentage(int sugarValue)
    {
        return 0.1F + 0.025F * sugarValue;
    }

    private static int itemsValue2ItemsCount(int itemsValue)
    {
        return itemsValue + 1;
    }


    // --------------------
    // Functional code
    // --------------------

    private RecyclerViewAdapter adapter;
    private MainScreenBinding binding;
    private List<ItemEntity> allItems = new LinkedList<>();
    private int sizeValue;
    private int sugarValue;
    private int itemsValue;

    private <T> float getLowestValue(List<T> list, Function<T, Float> getter)
    {
        float lowestValue = getter.apply(list.get(0));
        for (int i = 1; i < list.size(); i++)
        {
            lowestValue = Math.min(lowestValue, getter.apply(list.get(i)));
        }
        return lowestValue;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.main_screen);
        Random random = new Random();
        List<ItemEntity> fillerItems = new LinkedList<>();
        List<ItemEntity> selectableItems = new LinkedList<>();
        List<ItemEntity> chosenItems = new ArrayList<>(binding.itemsSlider.getMax() + 1);
        List<ItemEntity> usedItems = new LinkedList<>();
        List<ItemEntity> priorityChoosing = new ArrayList<>(binding.itemsSlider.getMax() + 1);

        // Setup recycle view adapter:
        adapter = new RecyclerViewAdapter();
        binding.ingredients.setAdapter(adapter);
        binding.ingredients.setLayoutManager(new LinearLayoutManager(this));

        // Load/create items and add them to the appropriate lists:
        try
        {
            Context context = getApplicationContext();
            List<String> fileNames = new ArrayList<>(Arrays.asList(context.fileList()));
            if (fileNames.contains(ITEMS_FILENAME))
            {
                byte[] bytes;
                FileInputStream fileInputStream = context.openFileInput(ITEMS_FILENAME);
                int length;
                while ((length = fileInputStream.read()) != -1)
                {
                    bytes = new byte[length];
                    fileInputStream.read(bytes);
                    allItems.add(new ItemEntity(bytes));
                }
            }
            else
            {
                addDefaultItemsToList(allItems);
            }
            for (ItemEntity item: allItems)
            {
                switch (item.getType())
                {
                    case ITEM_TYPE_FILLER:
                        fillerItems.add(item);
                        break;
                    case ITEM_TYPE_SELECTABLE_REGULAR:
                        selectableItems.add(item);
                        break;
                    case ITEM_TYPE_USED_REGULAR:
                        usedItems.add(item);
                        break;
                    default:
                        Log.e("onDestroy", "unrecognized item type");
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        if (getLowestValue(selectableItems, ItemEntity::getSugarPercentage) <= getLowestValue(fillerItems, ItemEntity::getSugarPercentage)) throw new AssertionError("sugar percentage of all filler items has to be lower than that of regular items");

        // Init layout variables:
        sizeValue = binding.sizeSlider.getProgress();
        sugarValue = binding.sugarSlider.getProgress();
        itemsValue = binding.itemsSlider.getProgress();
        binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
        binding.setSugarPercentage(String.format(Locale.getDefault(), "%.1f", sugarValue2SugarPercentage(sugarValue) * 100));
        binding.setItemsCount(itemsValue2ItemsCount(itemsValue));
        binding.setIsAvailabilityBoxMinimized(true);
        binding.setIsChosenMuesliUsed(true);
        binding.setIsInvalidSettings(false);
        binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync

        // Create slider and button listeners:
        binding.sizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                sizeValue = progress;
                binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            }
        });
        binding.sugarSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                sugarValue = progress;
                binding.setSugarPercentage(String.format(Locale.getDefault(), "%.1f", sugarValue2SugarPercentage(sugarValue) * 100));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            }
        });
        binding.itemsSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                itemsValue = progress;
                binding.setItemsCount(itemsValue2ItemsCount(itemsValue));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            }
        });
        binding.randomizeButton.setOnClickListener((View view) ->
        {
            final int regularItemsCount = itemsValue2ItemsCount(itemsValue);
            final float targetWeight = sizeValue2SizeWeight(sizeValue);
            final float targetSugar = sugarValue2SugarPercentage(sugarValue) * targetWeight;

            // Return used items back to the selectable pool if necessary:
            if (selectableItems.size() + chosenItems.size() < regularItemsCount)
            {
                priorityChoosing.addAll(selectableItems);
                priorityChoosing.addAll(chosenItems);
                usedItems.forEach((ItemEntity item) -> item.setType(ITEM_TYPE_SELECTABLE_REGULAR));
                selectableItems.addAll(usedItems);
                usedItems.clear();
            }

            if (selectableItems.size() + chosenItems.size() < regularItemsCount)
            {
                Log.e("onCreate", "regularItemsCount cannot be smaller than count of all regular items");
            }

            // Retry with randomized ingredients until a valid mix is found:
            final List<IngredientEntity> ingredients = new ArrayList<>(regularItemsCount + 1);
            int fullResetTryCounter = 0;
            while (fullResetTryCounter <= 1)
            {
                Log.d("onCreate", "------------------------");  // DEBUG:
                Log.d("onCreate", "selectableItems count: " + selectableItems.size());  // DEBUG:
                Log.d("onCreate", "chosenItems count: " + chosenItems.size());  // DEBUG:
                Log.d("onCreate", "usedItems count: " + usedItems.size());  // DEBUG:
                Log.d("onCreate", "priorityChoosing count: " + priorityChoosing.size());  // DEBUG:
                for (int tryCounter = 0; tryCounter < MAX_RANDOMIZE_TRIES; tryCounter++)
                {
                    // Return chosen items back to the selectable pool if necessary:
                    if (!chosenItems.isEmpty())
                    {
                        selectableItems.addAll(chosenItems);
                        chosenItems.clear();
                    }

                    // Chose items for muesli:
                    chosenItems.addAll(priorityChoosing);
                    selectableItems.removeAll(priorityChoosing);
                    for (int i = 0; i < regularItemsCount - priorityChoosing.size(); i++)
                    {
                        chosenItems.add(selectableItems.remove(random.nextInt(selectableItems.size())));
                    }
                    ItemEntity fillerItem = fillerItems.get(random.nextInt(fillerItems.size()));

                    // Determine ingredients:
                    float totalWeight = 0F;
                    float totalSugar = 0F;
                    ingredients.clear();
                    ItemEntity item;
                    int spoonCount;
                    // Calculate and add spoons for first regular items based on number of ingredients:
                    for (int i = 0; i < regularItemsCount - 1; i++)
                    {
                        item = chosenItems.get(i);
                        spoonCount = Math.round(targetWeight / (item.getSpoonWeight() * (regularItemsCount + FILLER_INGREDIENT_RATIO)));
                        spoonCount = Math.max(spoonCount, 1);
                        totalWeight += spoonCount * item.getSpoonWeight();
                        totalSugar += spoonCount * item.getSpoonWeight() * (item.getSugarPercentage() - fillerItem.getSugarPercentage());
                        ingredients.add(new IngredientEntity(item, spoonCount));
                    }
                    // Calculate and add spoons for last regular item based on sugar percentage:
                    item = chosenItems.get(regularItemsCount - 1);
                    spoonCount = Math.round((targetSugar - totalSugar) / (item.getSpoonWeight() * (item.getSugarPercentage() - fillerItem.getSugarPercentage())));
                    if (spoonCount <= 0) continue;
                    totalWeight += item.getSpoonWeight() * spoonCount;
                    ingredients.add(new IngredientEntity(item, spoonCount));
                    // Calculate and add spoons for filler item based on total size:
                    spoonCount = Math.round((targetWeight - totalWeight) / fillerItem.getSpoonWeight());
                    if (spoonCount < 0) continue;
                    if (spoonCount > 0)
                    {
                        ingredients.add(new IngredientEntity(fillerItem, spoonCount));
                    }

                    // Display ingredients and adjust use button:
                    Log.d("onCreate", "totalWeight: " + (totalWeight + fillerItem.getSpoonWeight() * spoonCount));  // DEBUG:
                    Log.d("onCreate", "tryCounter: " + tryCounter);  // DEBUG:
                    adapter.setIngredients(ingredients);
                    binding.setIsChosenMuesliUsed(false);
                    binding.setIsInvalidSettings(false);
                    binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                    return;
                }
                // Return used and chosen items back to the selectable pool and reset priority choosing:
                usedItems.forEach((ItemEntity item) -> item.setType(ITEM_TYPE_SELECTABLE_REGULAR));
                selectableItems.addAll(usedItems);
                usedItems.clear();
                selectableItems.addAll(chosenItems);
                chosenItems.clear();
                priorityChoosing.clear();
                Log.i("onCreate", "cannot find valid mix with selectable items, retrying with full reset");
                fullResetTryCounter += 1;
            }
            binding.setIsInvalidSettings(true);
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        });
        binding.useButton.setOnClickListener((View view) ->
        {
            if (chosenItems.isEmpty())
            {
                Log.e("onCreate", "chosenItems is empty");
            }

            // Move chosen items to used pool and reset priority choosing:
            chosenItems.forEach((ItemEntity item) -> item.setType(ITEM_TYPE_USED_REGULAR));
            usedItems.addAll(chosenItems);
            chosenItems.clear();
            priorityChoosing.clear();

            // Adjust use button:
            binding.setIsChosenMuesliUsed(true);
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // Store items:
        try
        {
            byte[] bytes;
            ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
            for (ItemEntity item: allItems)
            {
                bytes = item.toByteArray();
                dataOutputStream.write(bytes.length);
                dataOutputStream.write(bytes);

                if (bytes.length > 255)
                {
                    Log.e("onDestroy", "data size of an Item is too big, consider limiting allowed string sizes or use two bytes for data size");
                }
            }
            FileOutputStream fileOutputStream = getApplicationContext().openFileOutput(ITEMS_FILENAME, Context.MODE_PRIVATE);
            fileOutputStream.write(dataOutputStream.toByteArray());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
