package com.blackcj.fitdata.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blackcj.fitdata.R;

/**
 * Created by Chris Black
 */
public class WorkoutViewHolder extends RecyclerView.ViewHolder {
    public final ImageView image;
    public final TextView text;
    public final TextView detail;
    public final LinearLayout container;

    public WorkoutViewHolder(View itemView) {
        super(itemView);
        image = (ImageView) itemView.findViewById(R.id.image);
        text = (TextView) itemView.findViewById(R.id.text);
        detail = (TextView) itemView.findViewById(R.id.summary_text);
        container = (LinearLayout) itemView.findViewById(R.id.container);
    }
}
