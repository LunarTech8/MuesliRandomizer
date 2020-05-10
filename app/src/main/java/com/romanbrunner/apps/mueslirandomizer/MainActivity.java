package com.romanbrunner.apps.mueslirandomizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;

import com.romanbrunner.apps.mueslirandomizer.databinding.MainScreenBinding;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity
{
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

        // Setup recycle view adapter:
        adapter = new RecyclerViewAdapter();
        binding.ingredients.setAdapter(adapter);
        binding.ingredients.setLayoutManager(new LinearLayoutManager(this));

        // DEBUG: set test ingredients
        List<IngredientEntity> entries = new ArrayList<>(3);
        entries.add(new IngredientEntity(new MuesliEntity("MuesliNameA", 1, 3F, 0.2F), 3));
        entries.add(new IngredientEntity(new MuesliEntity("MuesliNameB", 1, 4F, 0.15F), 3));
        entries.add(new IngredientEntity(new MuesliEntity("MuesliNameC", 2, 5F, 0F), 2));
        adapter.setIngredients(entries);
    }
}
