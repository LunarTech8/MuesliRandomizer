package com.romanbrunner.apps.mueslirandomizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.romanbrunner.apps.mueslirandomizer.databinding.ArticleBinding;

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
            binding.availableCheckbox.setOnClickListener((View view) ->
            {
                final int position = getAdapterPosition();
                articlesAdapter.articles.get(position).setAvailable(binding.availableCheckbox.isChecked());
            });  // Adjust article availability via listener instead of directly through the layout to avoid problems with item recycling
            this.binding = binding;
        }
    }

    ArticlesAdapter()
    {
        articles = null;
    }

    void setArticles(@NonNull final List<? extends Article> articles)
    {
        if (this.articles == null)
        {
            // Add all entries:
            this.articles = articles;
            notifyItemRangeInserted(0, articles.size());
        }
        else
        {
            // Update changed entries:
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback()
            {
                @Override
                public int getOldListSize()
                {
                    return ArticlesAdapter.this.articles.size();
                }

                @Override
                public int getNewListSize()
                {
                    return articles.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition)
                {
                    return ArticlesAdapter.this.articles.get(oldItemPosition).getName().equals(articles.get(newItemPosition).getName());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition)
                {
                    return ArticleEntity.isContentTheSame(articles.get(newItemPosition), ArticlesAdapter.this.articles.get(oldItemPosition));
                }
            });
            this.articles = articles;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    public int getItemCount()
    {
        return articles == null ? 0 : articles.size();
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
        exerciseViewHolder.binding.setArticle(articles.get(position));
        exerciseViewHolder.binding.executePendingBindings();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
    }
}