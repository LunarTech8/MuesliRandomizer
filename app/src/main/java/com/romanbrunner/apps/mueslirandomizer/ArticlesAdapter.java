package com.romanbrunner.apps.mueslirandomizer;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.romanbrunner.apps.mueslirandomizer.databinding.ArticleBinding;

import java.util.ArrayList;
import java.util.List;


class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.EntryViewHolder>
{
    // --------------------
    // Functional code
    // --------------------

    private List<? extends Article> articles;

    static class EntryViewHolder extends RecyclerView.ViewHolder
    {
        final ArticleBinding binding;

        EntryViewHolder(ArticleBinding binding, ArticlesAdapter articlesAdapter)
        {
            super(binding.getRoot());
            binding.availabilityButton.setOnClickListener((View view) ->
            {
                final int position = getAdapterPosition();
                articlesAdapter.articles.get(position).incrementMultiplier();
                articlesAdapter.notifyItemChanged(position);
            });
            this.binding = binding;
        }
    }

    ArticlesAdapter()
    {
        articles = null;
    }

    void setArticles(@NonNull final List<? extends Article> newArticles)
    {
        if (articles == null)
        {
            // Add all entries:
            articles = new ArrayList<>(newArticles);
            notifyItemRangeInserted(0, newArticles.size());
        }
        else
        {
            Log.d("setArticles", "setArticles run");
            // Update changed entries:
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback()
            {
                @Override
                public int getOldListSize()
                {
                    return articles.size();
                }

                @Override
                public int getNewListSize()
                {
                    return newArticles.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition)
                {
                    return articles.get(oldItemPosition).getName().equals(newArticles.get(newItemPosition).getName());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition)
                {
                    return ArticleEntity.isContentTheSame(newArticles.get(newItemPosition), articles.get(oldItemPosition));
                }
            });
            articles = new ArrayList<>(newArticles);
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    public int getItemCount()
    {
        return (articles == null ? 0 : articles.size());
    }

    @Override
    public long getItemId(int position)
    {
        return articles.get(position).getName().hashCode();
    }

    @Override
    public @NonNull EntryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType)
    {
        ArticleBinding binding = DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.article, viewGroup, false);
        return new EntryViewHolder(binding, this);
    }

    @Override
    /* Is called when an item is reloaded (that was previously not visible) */
    public void onBindViewHolder(EntryViewHolder exerciseViewHolder, int position)
    {
        // Adjust changeable values of the view fields by the current entries list:
        final Article article = articles.get(position);
        exerciseViewHolder.binding.setArticle(article);
        if (article.isAvailable())
        {
            exerciseViewHolder.binding.name.setTextColor(Color.BLACK);
        }
        else
        {
            exerciseViewHolder.binding.name.setTextColor(Color.GRAY);
        }
        exerciseViewHolder.binding.executePendingBindings();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
    }
}