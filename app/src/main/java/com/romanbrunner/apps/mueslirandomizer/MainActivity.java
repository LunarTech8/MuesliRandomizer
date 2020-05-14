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
import java.util.Random;


public class MainActivity extends AppCompatActivity
{
    // --------------------
    // Data code
    // --------------------

    private static final int REGULAR_INGREDIENTS_AMOUNT = 2;

    private static void addDefaultFillerMuesliToList(List<MuesliEntity> muesliList)
    {
        // Very low sugar filler muesli:
        muesliList.add(new MuesliEntity("FillerMuesliNameA", 0, 5F, 0F));
        muesliList.add(new MuesliEntity("FillerMuesliNameB", 1, 6F, 0.05F));
    }

    private static void addDefaultRegularMuesliToList(List<MuesliEntity> muesliList)
    {
        // Low sugar muesli:
        muesliList.add(new MuesliEntity("MuesliNameC", 0, 5F, 0.12F));
        muesliList.add(new MuesliEntity("MuesliNameD", 1, 6F, 0.08F));
        // Medium sugar muesli:
        muesliList.add(new MuesliEntity("MuesliNameB", 1, 4F, 0.13F));
        muesliList.add(new MuesliEntity("MuesliNameE", 2, 5.5F, 0.18F));
        muesliList.add(new MuesliEntity("MuesliNameG", 2, 5.5F, 0.16F));
        // High sugar muesli:
        muesliList.add(new MuesliEntity("MuesliNameA", 2, 3F, 0.2F));
        muesliList.add(new MuesliEntity("MuesliNameF", 1, 3F, 0.22F));
    }

    private static float sizeValue2SizeWeight(int sizeValue)
    {
        return 10F * 2 * (sizeValue + 1);
    }

    private static float sugarValue2SugarPercentage(int sugarValue)
    {
        return 0.05F * (sugarValue + 1);
    }


    // --------------------
    // Functional code
    // --------------------

    private RecyclerViewAdapter adapter;
    private MainScreenBinding binding;
    private int sizeValue;
    private int sugarValue;

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

        List<MuesliEntity> fillerMuesliList = new LinkedList<>();
        List<MuesliEntity> selectableMuesliList = new LinkedList<>();
        List<MuesliEntity> chosenMuesliList = new ArrayList<>(REGULAR_INGREDIENTS_AMOUNT);
        List<MuesliEntity> usedMuesliList = new LinkedList<>();
        addDefaultFillerMuesliToList(fillerMuesliList);
        addDefaultRegularMuesliToList(selectableMuesliList);

        sizeValue = binding.sizeSlider.getProgress();
        sugarValue = binding.sugarSlider.getProgress();
        binding.setSizeWeight(sizeValue2SizeWeight(sizeValue));
        binding.setSugarPercentage(sugarValue2SugarPercentage(sugarValue));
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
                binding.setSizeWeight(sizeValue2SizeWeight(sizeValue));
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
                binding.setSugarPercentage(sugarValue2SugarPercentage(sugarValue));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            }
        });
        binding.randomizeButton.setOnClickListener((View view) ->
        {
            // Return chosen muesli back to the selectable pool if necessary:
            if (!chosenMuesliList.isEmpty())
            {
                selectableMuesliList.addAll(chosenMuesliList);
                chosenMuesliList.clear();
            }

            // Return used muesli back to the selectable pool if necessary:
            if (selectableMuesliList.size() < REGULAR_INGREDIENTS_AMOUNT)
            {
                selectableMuesliList.addAll(usedMuesliList);
                usedMuesliList.clear();
            }

            // Chose muesli at random:
            for (int i = 0; i < REGULAR_INGREDIENTS_AMOUNT; i++)
            {
                chosenMuesliList.add(selectableMuesliList.remove(random.nextInt(selectableMuesliList.size())));
            }

            // Display ingredients:
            List<IngredientEntity> ingredients = new ArrayList<>(REGULAR_INGREDIENTS_AMOUNT + 1);
            MuesliEntity muesli;
            int spoonCount;
            // Add regular muesli based on target sugar percentage:
            float totalWeight = 0F;
            for (int i = 0; i < REGULAR_INGREDIENTS_AMOUNT; i++)
            {
                muesli = chosenMuesliList.get(i);
                // TODO: calculate and use correct spoonCounts
                spoonCount = 3;
                totalWeight += muesli.getSpoonWeight() * spoonCount;
                ingredients.add(new IngredientEntity(muesli, spoonCount));
            }
            // Add filler muesli to reach target size weight:
            muesli = fillerMuesliList.get(random.nextInt(fillerMuesliList.size()));
            spoonCount = Math.round((sizeValue2SizeWeight(sizeValue) - totalWeight) / muesli.getSpoonWeight());
            totalWeight += muesli.getSpoonWeight() * spoonCount;  // DEBUG:
            Log.d("onCreate", "totalWeight: " + totalWeight);  // DEBUG:
            ingredients.add(new IngredientEntity(muesli, spoonCount));
            adapter.setIngredients(ingredients);

            // Adjust use button:
            binding.setIsChosenMuesliUsed(false);
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        });
        binding.useButton.setOnClickListener((View view) ->
        {
            if (chosenMuesliList.isEmpty())
            {
                Log.e("onCreate", "chosenMuesliList is empty");
            }

            // Move chosen muesli to used pool:
            usedMuesliList.addAll(chosenMuesliList);
            chosenMuesliList.clear();

            // Adjust use button:
            binding.setIsChosenMuesliUsed(true);
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        });
    }
}
