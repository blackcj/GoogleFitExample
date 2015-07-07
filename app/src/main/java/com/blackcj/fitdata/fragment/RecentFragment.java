package com.blackcj.fitdata.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.activity.IMainActivityCallback;
import com.blackcj.fitdata.adapter.AnimatedLayoutManager;
import com.blackcj.fitdata.adapter.CursorRecyclerViewAdapter;
import com.blackcj.fitdata.adapter.RecyclerViewAdapter;
import com.blackcj.fitdata.adapter.WorkoutListViewAdapter;
import com.blackcj.fitdata.animation.ItemAnimator;
import com.blackcj.fitdata.database.DataManager;
import com.blackcj.fitdata.model.Workout;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by chris.black on 7/2/15.
 */
public class RecentFragment extends BaseFragment implements WorkoutListViewAdapter.OnItemClickListener {

    public static final String TAG = "RecentFragment";

    public DataManager.IDataManager dataReceiver;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    private WorkoutListViewAdapter adapter;

    public static RecentFragment create() {
        RecentFragment fragment = new RecentFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent, container, false);

        ButterKnife.bind(this, view);

        mRecyclerView.setLayoutManager(new AnimatedLayoutManager(this.getActivity(), 1));

        adapter = new WorkoutListViewAdapter(this.getActivity(), mCallback.getCursor());
        adapter.setHasStableIds(true);
        adapter.setOnItemClickListener(this);

        mRecyclerView.setItemAnimator(new ItemAnimator());
        mRecyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof DataManager.IDataManager) {
            dataReceiver = (DataManager.IDataManager)activity;
        }
    }

    public void swapCursor(Cursor cursor) {
        adapter.swapCursor(cursor);
        adapter.notifyDataSetChanged();
    }

    /**
     * Clear callback on detach to prevent null reference errors after the view has been
     */
    @Override
    public void onDetach() {
        super.onDetach();
        dataReceiver = null;
    }

    @Override
    public void onItemClick(View view, Workout viewModel) {
        if (dataReceiver != null) {
            dataReceiver.removeData(viewModel);
        }
    }
}
