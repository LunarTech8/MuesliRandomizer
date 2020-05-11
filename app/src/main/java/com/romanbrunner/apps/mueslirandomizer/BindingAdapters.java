package com.romanbrunner.apps.mueslirandomizer;

import android.view.View;

import androidx.databinding.BindingAdapter;


public class BindingAdapters
{
    // --------------------
    // Functional code
    // --------------------

    @BindingAdapter("visibleGone")
    public static void showHide(View view, boolean show)
    {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}