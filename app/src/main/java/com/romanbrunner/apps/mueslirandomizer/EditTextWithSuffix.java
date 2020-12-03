package com.romanbrunner.apps.mueslirandomizer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;


public class EditTextWithSuffix extends androidx.appcompat.widget.AppCompatEditText
{
    TextPaint textPaint = new TextPaint();
    private String suffix = "";
    private float suffixPadding;

    public EditTextWithSuffix(Context context)
    {
        super(context);
    }
    public EditTextWithSuffix(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        getAttributes(context, attrs, 0);
    }
    public EditTextWithSuffix(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        getAttributes(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        textPaint.setColor(getCurrentTextColor());
        textPaint.setTextSize(getTextSize());
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public void onDraw(Canvas c)
    {
        super.onDraw(c);
        int suffixXPosition = 0;
        if (getText() != null && !getText().toString().isEmpty())
        {
            suffixXPosition = (int)textPaint.measureText(getText().toString()) + getPaddingLeft();
        }
        else if (getHint() != null && !getHint().toString().isEmpty())
        {
            suffixXPosition = (int)textPaint.measureText(getHint().toString()) + getPaddingLeft();  // FIXME: hint suffix should have the same colour as the hint text (gray)
        }
        c.drawText(suffix, Math.max(suffixXPosition, suffixPadding), getBaseline(), textPaint);  // FIXME: suffixPadding probably not needed and can be dismantled
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr)
    {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EditTextWithSuffix, defStyleAttr, 0);
        suffix = typedArray.getString(R.styleable.EditTextWithSuffix_suffix);
        if(suffix == null)
        {
            suffix = "";
        }
        suffixPadding = typedArray.getDimension(R.styleable.EditTextWithSuffix_suffixPadding, 0F);
        typedArray.recycle();
    }
}