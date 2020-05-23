package com.romanbrunner.apps.mueslirandomizer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.romanbrunner.apps.mueslirandomizer.databinding.IngredientBinding;

import java.util.List;


class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.EntryViewHolder>
{
    // --------------------
    // Functional code
    // --------------------

    private List<? extends Ingredient> ingredients;

    static class EntryViewHolder extends RecyclerView.ViewHolder
    {
        final IngredientBinding binding;

        EntryViewHolder(IngredientBinding binding)
        {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    IngredientsAdapter()
    {
        ingredients = null;
    }

    void setIngredients(@NonNull final List<? extends Ingredient> ingredients)
    {
        if (this.ingredients == null)
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
            this.ingredients = ingredients;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    public int getItemCount()
    {
        return ingredients == null ? 0 : ingredients.size();
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
        return new EntryViewHolder(binding);
    }

    @Override
    /* Is called when an item is reloaded (that was previously not visible) */
    public void onBindViewHolder(EntryViewHolder exerciseViewHolder, int position)
    {
        // Adjust changeable values of the view fields by the current entries list:
        exerciseViewHolder.binding.setIngredient(ingredients.get(position));
        exerciseViewHolder.binding.executePendingBindings();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
    }
}