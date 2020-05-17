package com.romanbrunner.apps.mueslirandomizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.romanbrunner.apps.mueslirandomizer.databinding.MainScreenBinding;

import java.util.ArrayList;
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
    private final static int MAX_RANDOMIZE_TRIES = 1000;

    private static void addDefaultFillerItemsToList(List<ItemEntity> muesliList)
    {
        // Very low sugar muesli:
        muesliList.add(new ItemEntity("Echte Kölln Kernige", "Kölln", 0, 8F, 0.012F));
    }

    private static void addDefaultRegularItemsToList(List<ItemEntity> muesliList)
    {
        // Low sugar muesli:
        muesliList.add(new ItemEntity("Nuss & Krokant", "Kölln", 1, 9.5F, 0.077F));
        // Medium sugar muesli:
        muesliList.add(new ItemEntity("Superfood Crunchy Müsli Cacao & Nuts", "Kellogg", 1, 10.5F, 0.14F));
        muesliList.add(new ItemEntity("Schokomüsli Feinherb", "Vitalis", 1, 9.5F, 0.15F));
        muesliList.add(new ItemEntity("Joghurtmüsli mit Erdbeer-Stücken", "Vitalis", 1, 9F, 0.13F));
        muesliList.add(new ItemEntity("Schoko 30% weniger Zucker", "Kölln", 1, 10F, 0.13F));
        // High sugar muesli:
        muesliList.add(new ItemEntity("Nesquik Knusper-Müsli", "Nestle", 1, 8F, 0.21F));
        muesliList.add(new ItemEntity("Crunchy Müsli Red Berries", "Kellogg", 1, 9.5F, 0.22F));
        muesliList.add(new ItemEntity("Knuspermüsli Nuss-Nougat", "Vitalis", 1, 11.5F, 0.25F));
        muesliList.add(new ItemEntity("Knuspermüsli Plus Nuss Mischung", "Vitalis", 1, 13.5F, 0.2F));
        muesliList.add(new ItemEntity("Knusper Beere & Schoko", "Kölln", 1, 11.5F, 0.24F));
        muesliList.add(new ItemEntity("Knusper Schoko-Krokant", "Kölln", 1, 11.5F, 0.22F));
        muesliList.add(new ItemEntity("Knusper Schoko & Kaffee", "Kölln", 1, 12F, 0.22F));
        muesliList.add(new ItemEntity("Knusper Schoko & Keks", "Kölln", 1, 11.5F, 0.21F));
        muesliList.add(new ItemEntity("Knusper Joghurt-Honig", "Kölln", 1, 11F, 0.2F));
        muesliList.add(new ItemEntity("Knusper Schoko Feinherb 30% weniger Fett", "Kölln", 1, 12F, 0.2F));
        muesliList.add(new ItemEntity("Porridge Dreierlei Beere", "3 Bears", 1, 12.5F, 0.22F));
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.main_screen);
        Random random = new Random();

        // Setup recycle view adapter:
        adapter = new RecyclerViewAdapter();
        binding.ingredients.setAdapter(adapter);
        binding.ingredients.setLayoutManager(new LinearLayoutManager(this));
        binding.setIsChosenMuesliUsed(true);

        List<ItemEntity> fillerItems = new LinkedList<>();
        List<ItemEntity> selectableItems = new LinkedList<>();
        List<ItemEntity> chosenItems = new ArrayList<>(binding.itemsSlider.getMax() + 1);
        List<ItemEntity> usedItems = new LinkedList<>();
        addDefaultFillerItemsToList(fillerItems);
        addDefaultRegularItemsToList(selectableItems);
        if (getLowestValue(selectableItems, ItemEntity::getSugarPercentage) <= getLowestValue(fillerItems, ItemEntity::getSugarPercentage)) throw new AssertionError("sugar percentage of all filler items has to be lower than that of regular items");

        sizeValue = binding.sizeSlider.getProgress();
        sugarValue = binding.sugarSlider.getProgress();
        itemsValue = binding.itemsSlider.getProgress();
        binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
        binding.setSugarPercentage(String.format(Locale.getDefault(), "%.1f", sugarValue2SugarPercentage(sugarValue) * 100));
        binding.setItemsCount(itemsValue2ItemsCount(itemsValue));
        binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync

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
                Log.d("onCreate", "selectableItems count: " + selectableItems.size());  // DEBUG:
                Log.d("onCreate", "chosenItems count: " + chosenItems.size());  // DEBUG:
                Log.d("onCreate", "usedItems count: " + usedItems.size());  // DEBUG:
                for (int tryCounter = 0; tryCounter < MAX_RANDOMIZE_TRIES; tryCounter++)
                {
                    // Return chosen items back to the selectable pool if necessary:
                    if (!chosenItems.isEmpty())
                    {
                        selectableItems.addAll(chosenItems);
                        chosenItems.clear();
                    }

                    // Chose items for muesli at random:
                    for (int i = 0; i < regularItemsCount; i++)
                    {
                        // FIXME: with many items and low size selectableItems.size() gets negative or zero, should not happen
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
                    binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                    return;
                }
                // Return used and chosen items back to the selectable pool:
                selectableItems.addAll(usedItems);
                chosenItems.addAll(usedItems);
                usedItems.clear();
                chosenItems.clear();
                Log.i("onCreate", "cannot find valid mix with selectable items, retrying with full reset");
                fullResetTryCounter += 1;
            }
            Log.e("onCreate", "cannot find valid mix with all items for given settings");
        });
        binding.useButton.setOnClickListener((View view) ->
        {
            if (chosenItems.isEmpty())
            {
                Log.e("onCreate", "chosenItems is empty");
            }

            // Move chosen items to used pool:
            usedItems.addAll(chosenItems);
            chosenItems.clear();

            // Adjust use button:
            binding.setIsChosenMuesliUsed(true);
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        });
    }
}
