package com.romanbrunner.apps.mueslirandomizer;

import android.view.View;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;


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

    @BindingAdapter("android:text")
    public static void setDouble(TextView view, double value)
    {
        if (value <= 0.0D)
        {
            view.setText("");
        }
        else if (!Double.isNaN(value))
        {
            view.setText(String.valueOf(value));
        }
    }

    @InverseBindingAdapter(attribute = "android:text")
    public static double getDouble(TextView view)
    {
        String num = view.getText().toString();
        if(num.isEmpty())
        {
            return 0.0D;
        }
        try
        {
            return Double.parseDouble(num);
        }
        catch (NumberFormatException e)
        {
            return 0.0D;
        }
    }
}