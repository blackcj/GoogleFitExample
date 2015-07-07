package com.blackcj.fitdata.adapter;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.util.AttributeSet;

/**
 * Created by chris.black on 7/7/15.
 */
public class AnimatedLayoutManager extends GridLayoutManager {


    public AnimatedLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return true;
    }
}
