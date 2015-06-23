package com.blackcj.fitdata.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by chris.black on 6/19/15.
 */
public class PageFragment extends BaseFragment implements RecyclerViewAdapter.OnItemClickListener {

    public static final String ARG_PAGE = "ARG_PAGE";
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerViewAdapter adapter;
    private int mPage;

    @InjectView(R.id.recyclerView)
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page, container, false);

        ButterKnife.inject(this, view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this.getActivity(), 2));

        List<Workout> items = mCallback.getData(Utilities.TimeFrame.values()[mPage - 1]);
        adapter = new RecyclerViewAdapter(items, this.getActivity(), Utilities.getTimeFrameText(Utilities.TimeFrame.values()[mPage - 1]));
        adapter.setOnItemClickListener(this);

        mRecyclerView.setItemAnimator(new ItemAnimator());
        mRecyclerView.setAdapter(adapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.contentView);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<Workout> items = mCallback.getData(Utilities.TimeFrame.values()[mPage - 1]);
                        adapter.setItems(items, Utilities.getTimeFrameText(Utilities.TimeFrame.values()[mPage - 1]));
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 1000);
            }
        });
        mSwipeRefreshLayout.setEnabled(false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //mSwipeRefreshLayout.setEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSwipeRefreshLayout!=null) {
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
        if(viewModel.type == WorkoutTypes.TIME.getValue()) {
            //cancelTimer();
            //timeFrame = timeFrame.next();
            //adapter.setNeedsAnimate();
            //populateReport();
        } else {
            //DetailActivity.launch(MainActivity.this, view.findViewById(R.id.image), viewModel);
        }
        mCallback.launch(view.findViewById(R.id.image), viewModel);
    }

}

