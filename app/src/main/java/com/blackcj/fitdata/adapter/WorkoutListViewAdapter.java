package com.blackcj.fitdata.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutReport;
import com.blackcj.fitdata.model.WorkoutTypes;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by chris.black on 6/22/15.
 */
public class WorkoutListViewAdapter extends CursorRecyclerViewAdapter<WorkoutListViewAdapter.ListViewHolder>{

    public WorkoutListViewAdapter(Context context, Cursor cursor){
        super(context,cursor);
    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_item, parent, false);
        ListViewHolder vh = new ListViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(ListViewHolder holder, Cursor cursor) {
        Workout item = cupboard().withCursor(cursor).get(Workout.class);

        holder.text.setText(item.toString());

        holder.itemView.setTag(item);

    }

    public class ListViewHolder extends RecyclerView.ViewHolder {
        public TextView text;
        public LinearLayout container;

        public ListViewHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.text);
            container = (LinearLayout) itemView.findViewById(R.id.container);
        }
    }
}
