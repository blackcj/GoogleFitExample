package com.blackcj.fitdata.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.blackcj.fitdata.R;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.adapter.RecyclerViewAdapter;
import com.blackcj.fitdata.adapter.WorkoutViewHolder;
import com.blackcj.fitdata.animation.ItemAnimator;
import com.blackcj.fitdata.database.CacheManager;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutTypes;
import com.blackcj.fitdata.service.CacheResultReceiver;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by chris.black on 6/19/15.
 */
public class PageFragment extends BaseFragment implements RecyclerViewAdapter.OnItemClickListener, CacheResultReceiver.Receiver {

    public static final String TAG = "PageFragment";
    public static final String ARG_PAGE = "ARG_PAGE";
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerViewAdapter adapter;
    private int mPage;
    private CacheResultReceiver mReciever;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    public static PageFragment create(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        PageFragment fragment = new PageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
        mReciever = new CacheResultReceiver(new Handler());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page, container, false);

        ButterKnife.bind(this, view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this.getActivity(), 2));

        List<Workout> items = new ArrayList<>();

        adapter = new RecyclerViewAdapter(items, this.getActivity(), Utilities.getTimeFrameText(Utilities.TimeFrame.values()[mPage - 1]));
        adapter.setOnItemClickListener(this);

        mRecyclerView.setItemAnimator(new ItemAnimator());
        mRecyclerView.setAdapter(adapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.contentView);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                CacheManager.getReport(Utilities.TimeFrame.values()[mPage - 1], mReciever, getActivity());
            }
        });
        mSwipeRefreshLayout.setEnabled(false);
        return view;
    }

    public void refreshData() {
        mSwipeRefreshLayout.setRefreshing(true);
        CacheManager.getReport(Utilities.TimeFrame.values()[mPage - 1], mReciever, getActivity());
    }

    public void filter(String filterText) {
        adapter.filter(filterText);
    }

    @Override
    public void onResume() {
        super.onResume();
        mReciever.setReceiver(this);
        CacheManager.getReport(Utilities.TimeFrame.values()[mPage - 1], mReciever, getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        mReciever.setReceiver(null);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(false);
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.destroyDrawingCache();
            mSwipeRefreshLayout.clearAnimation();
        }
    }

    public void setSwipeToRefreshEnabled(boolean enabled) {
        mSwipeRefreshLayout.setEnabled(enabled);
    }

    @Override
    public void onItemClick(View view, Workout viewModel) {
        mCallback.launch(view.findViewById(R.id.image), viewModel);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        final List<Workout> workoutList = resultData.getParcelableArrayList("workoutList");
        // Update the UI
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.setItems(workoutList, Utilities.getTimeFrameText(Utilities.TimeFrame.values()[mPage - 1]));
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 700);
            }
        });

    }
}

