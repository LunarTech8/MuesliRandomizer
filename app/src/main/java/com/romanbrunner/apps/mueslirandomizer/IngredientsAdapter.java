package com.romanbrunner.apps.mueslirandomizer;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.romanbrunner.apps.mueslirandomizer.databinding.IngredientBinding;

import java.util.ArrayList;
import java.util.List;


class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.EntryViewHolder>
{
    // --------------------
    // Functional code
    // --------------------

    private final MainActivity mainActivity;
    private List<? extends Ingredient> ingredients;

    static class EntryViewHolder extends RecyclerView.ViewHolder
    {
        final IngredientBinding binding;

        EntryViewHolder(IngredientBinding binding, IngredientsAdapter ingredientsAdapter)
        {
            super(binding.getRoot());
            this.binding = binding;
            binding.setIsChosenMuesliUsed(ingredientsAdapter.mainActivity.isChosenMuesliUsed);
            binding.emptyButton.setOnClickListener((View view) ->
            {
                setMarkAsEmpty(binding, ingredientsAdapter.mainActivity, ingredientsAdapter.ingredients.get(getBindingAdapterPosition()).switchMarkAsEmpty());
                ingredientsAdapter.mainActivity.refreshData(false);
            });
        }
    }

    IngredientsAdapter(MainActivity mainActivity)
    {
        this.mainActivity = mainActivity;
        ingredients = new ArrayList<>(0);
    }

    static void setMarkAsEmpty(final IngredientBinding binding, final MainActivity mainActivity, final boolean enabled)
    {
        if (enabled)
        {
            binding.name.setPaintFlags(binding.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.data.setPaintFlags(binding.data.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.emptyButton.setTextColor(ResourcesCompat.getColor(mainActivity.getResources(), R.color.colorAccent, null));
        }
        else
        {
            binding.name.setPaintFlags(binding.name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.data.setPaintFlags(binding.data.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.emptyButton.setTextColor(ResourcesCompat.getColor(mainActivity.getResources(), R.color.colorButtonDarkText, null));
        }
    }

    void setIngredients(@NonNull final List<? extends Ingredient> ingredients)
    {
        if (this.ingredients.isEmpty())
        {
            // Add all entries:
            this.ingredients = ingredients;
            notifyItemRangeInserted(0, ingredients.size());
        }
        else
        {
            // Update changed entries:
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback()
            {
                @Override
                public int getOldListSize()
                {
                    return IngredientsAdapter.this.ingredients.size();
                }

                @Override
                public int getNewListSize()
                {
                    return ingredients.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition)
                {
                    return IngredientsAdapter.this.ingredients.get(oldItemPosition).getName().equals(ingredients.get(newItemPosition).getName());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition)
                {
                    return IngredientEntity.isContentTheSame(ingredients.get(newItemPosition), IngredientsAdapter.this.ingredients.get(oldItemPosition));
                }
            });
            for (var oldIngredient : this.ingredients)
            {
                if (!ingredients.contains(oldIngredient))
                {
                    oldIngredient.onRemoval();
                }
            }
            this.ingredients = ingredients;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    public int getItemCount()
    {
        return ingredients.size();
    }

    @Override
    public long getItemId(int position)
    {
        return ingredients.get(position).getName().hashCode();
    }

    @Override
    public @NonNull
    EntryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType)
    {
        IngredientBinding binding = DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.ingredient, viewGroup, false);
        return new EntryViewHolder(binding, this);
    }

    @Override
    /* Is called when an item is reloaded (that was previously not visible) */
    public void onBindViewHolder(EntryViewHolder exerciseViewHolder, int position)
    {
        // Adjust changeable values of the view fields by the current entries list:
        exerciseViewHolder.binding.setIngredient(ingredients.get(position));
        exerciseViewHolder.binding.setIsChosenMuesliUsed(mainActivity.isChosenMuesliUsed);
        var colour = mainActivity.isDarkModeEnabled() ? Color.WHITE : Color.BLACK;
        exerciseViewHolder.binding.name.setTextColor(colour);
        exerciseViewHolder.binding.data.setTextColor(colour);
        if (!mainActivity.isChosenMuesliUsed)
        {
            setMarkAsEmpty(exerciseViewHolder.binding, mainActivity, false);
        }
        exerciseViewHolder.binding.executePendingBindings();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
    }
}