package com.romanbrunner.apps.mueslirandomizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

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

    private static final int INGREDIENTS_AMOUNT = 2;


    // --------------------
    // Functional code
    // --------------------

    private RecyclerViewAdapter adapter;
    private MainScreenBinding binding;

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

        List<MuesliEntity> selectableMuesliList = new LinkedList<>();
        List<MuesliEntity> chosenMuesliList = new ArrayList<>(INGREDIENTS_AMOUNT);
        List<MuesliEntity> usedMuesliList = new LinkedList<>();
        // DEBUG: add test muesli
        selectableMuesliList.add(new MuesliEntity("MuesliNameA", 1, 3F, 0.2F));
        selectableMuesliList.add(new MuesliEntity("MuesliNameB", 1, 4F, 0.15F));
        selectableMuesliList.add(new MuesliEntity("MuesliNameC", 2, 5F, 0F));
        selectableMuesliList.add(new MuesliEntity("MuesliNameD", 2, 6F, 0F));
        selectableMuesliList.add(new MuesliEntity("MuesliNameE", 1, 5.5F, 0.18F));

        binding.randomizeButton.setOnClickListener((View view) ->
        {
            // Return chosen muesli back to the selectable pool if necessary:
            if (!chosenMuesliList.isEmpty())
            {
                selectableMuesliList.addAll(chosenMuesliList);
                chosenMuesliList.clear();
            }

            // Return used muesli back to the selectable pool if necessary:
            if (selectableMuesliList.size() < INGREDIENTS_AMOUNT)
            {
                selectableMuesliList.addAll(usedMuesliList);
                usedMuesliList.clear();
            }

            // Chose muesli at random:
            for (int i = 0; i < INGREDIENTS_AMOUNT; i++)
            {
                chosenMuesliList.add(selectableMuesliList.remove(random.nextInt(selectableMuesliList.size())));
            }

            // Display ingredients:
            List<IngredientEntity> ingredients = new ArrayList<>(INGREDIENTS_AMOUNT);
            for (int i = 0; i < INGREDIENTS_AMOUNT; i++)
            {
                ingredients.add(new IngredientEntity(chosenMuesliList.get(i), 3));
            }
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
