package com.blackcj.fitdata.fragment;

import android.support.v4.app.Fragment;

import butterknife.ButterKnife;

/**
 * Created by Chris Black
 *
 * Contains functionality common to all Fragments. Code here should be kept to the bare
 * minimum.
 */
public abstract class BaseFragment extends Fragment {

    public static final String ARG_SECTION_NUMBER = "section_number";

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Release the views injects by butterknife
        ButterKnife.unbind(this);
    }


}