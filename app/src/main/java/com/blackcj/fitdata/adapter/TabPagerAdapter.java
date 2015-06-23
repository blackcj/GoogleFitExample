package com.blackcj.fitdata.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;

import com.blackcj.fitdata.fragment.PageFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chris.black on 6/19/15.
 */
public class TabPagerAdapter extends FragmentPagerAdapter {
    private Map<Integer, PageFragment> mPageReferenceMap = new HashMap<>();

    private static final String[] TITLES = new String[] {
            "Day",
            "Week",
            "Month"
    };

    public static final int NUM_TITLES = TITLES.length;

    public TabPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return NUM_TITLES;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return TITLES[position];
    }

    @Override
    public Fragment getItem(int position) {
        PageFragment myFragment = PageFragment.create(position + 1);
        mPageReferenceMap.put(position, myFragment);
        return myFragment;
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        super.destroyItem(container, position, object);
        mPageReferenceMap.remove(position);
    }

    public PageFragment getFragment(int key) {
        return mPageReferenceMap.get(key);
    }
}
