package de.vanselow.deliveryhelper.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.GridLayout;

/**
 * Created by Felix on 16.05.2016.
 */
public class CheckableGridLayout extends GridLayout implements Checkable {
    private boolean checked;

    public CheckableGridLayout(Context context) {
        super(context);
        checked = false;
    }

    public CheckableGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        checked = false;
    }

    public CheckableGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        checked = false;
    }

    public CheckableGridLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        checked = false;
    }

    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
        refreshDrawableState();
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }

    private static final int[] checkedStateSet = {
            android.R.attr.state_checked,
    };

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, checkedStateSet);
        }
        return drawableState;
    }
}
