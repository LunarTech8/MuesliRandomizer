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
    public static void setFloat(TextView view, float value)
    {
        if (value <= 0F)
        {
            view.setText("");
        }
        else if (!Float.isNaN(value))
        {
            view.setText(String.valueOf(value));
        }
    }

    @InverseBindingAdapter(attribute = "android:text")
    public static float getFloat(TextView view)
    {
        String num = view.getText().toString();
        if(num.isEmpty())
        {
            return 0.0F;
        }
        try
        {
            return Float.parseFloat(num);
        }
        catch (NumberFormatException e)
        {
            return 0.0F;
        }
    }
}