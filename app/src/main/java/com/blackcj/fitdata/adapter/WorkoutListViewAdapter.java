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
public class WorkoutListViewAdapter extends CursorRecyclerViewAdapter<WorkoutViewHolder>{

    public WorkoutListViewAdapter(Context context, Cursor cursor){
        super(context,cursor);
    }



    @Override
    public WorkoutViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.grid_item, parent, false);
        WorkoutViewHolder vh = new WorkoutViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(WorkoutViewHolder holder, Cursor cursor) {
        Workout item = cupboard().withCursor(cursor).get(Workout.class);

        if(item.type == WorkoutTypes.TIME.getValue()) {
            //holder.text.setText(timeDesc);
        } else {
            holder.text.setText(WorkoutTypes.getWorkOutTextById(item.type));
        }
        holder.image.setImageResource(WorkoutTypes.getImageById(item.type));
        holder.itemView.setTag(item);
        Bitmap bitmap = ((BitmapDrawable)holder.image.getDrawable()).getBitmap();
        Palette palette = Palette.generate(bitmap);
        int vibrant = palette.getVibrantColor(0x000000);
        if(vibrant == 0) {
            //vibrant = palette.getLightMutedColor(0x000000);
            vibrant = palette.getMutedColor(0x000000);
        }

        if(item.type == WorkoutTypes.TIME.getValue()) {
            holder.detail.setText("Active: " + WorkoutReport.getDurationBreakdown(item.duration));
        }else if(item.type == WorkoutTypes.WALKING.getValue()) {
            if(item.start != 0) {
                // TODO: Remove this when we have step summary cached
                holder.detail.setText("");
            } else {
                holder.detail.setText(item.stepCount + " steps");
            }
        } else {
            holder.detail.setText(WorkoutReport.getDurationBreakdown(item.duration));
        }

        holder.image.setBackgroundColor(Utilities.lighter(vibrant, 0.4f));
        holder.container.setBackgroundColor(vibrant);
    }
}
