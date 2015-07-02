package com.blackcj.fitdata.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.adapter.CursorRecyclerViewAdapter;
import com.blackcj.fitdata.adapter.RecyclerViewAdapter;
import com.blackcj.fitdata.adapter.WorkoutListViewAdapter;
import com.blackcj.fitdata.animation.ItemAnimator;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by chris.black on 7/2/15.
 */
public class RecentFragment extends BaseFragment {

    public static final String TAG = "RecentFragment";

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    private CursorRecyclerViewAdapter adapter;

    public static RecentFragment create() {
        RecentFragment fragment = new RecentFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent, container, false);

        ButterKnife.bind(this, view);

        mRecyclerView.setLayoutManager(new GridLayoutManager(this.getActivity(), 1));

        adapter = new WorkoutListViewAdapter(this.getActivity(), mCallback.getCursor());

        mRecyclerView.setItemAnimator(new ItemAnimator());
        mRecyclerView.setAdapter(adapter);

        return view;
    }
}
