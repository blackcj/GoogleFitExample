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
public class WorkoutListViewAdapter extends CursorRecyclerViewAdapter<WorkoutListViewAdapter.ListViewHolder> implements View.OnClickListener{

    private OnItemClickListener onItemClickListener;

    public WorkoutListViewAdapter(Context context, Cursor cursor){
        super(context, cursor);
    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_item, parent, false);
        ListViewHolder vh = new ListViewHolder(itemView);
        vh.deleteButton.setOnClickListener(this);
        return vh;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public void onBindViewHolder(ListViewHolder holder, Cursor cursor) {
        Workout item = cupboard().withCursor(cursor).get(Workout.class);

        holder.text.setText(item.toString());
        holder.deleteButton.setTag(item);

        //holder.container.setBackgroundColor(WorkoutTypes.getColorById(item.type));

        holder.image.setImageResource(WorkoutTypes.getImageById(item.type));
    }

    @Override
    public void onClick(final View v) {

        if (onItemClickListener != null && v.getId() == R.id.close_button) {
            onItemClickListener.onItemClick(v, (Workout) v.getTag());
        }
    }

    public class ListViewHolder extends RecyclerView.ViewHolder {
        public TextView text;
        public ImageView image;
        public ImageView deleteButton;
        public View container;

        public ListViewHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.text);
            image = (ImageView) itemView.findViewById(R.id.image);
            container = itemView.findViewById(R.id.container);
            deleteButton = (ImageView) itemView.findViewById(R.id.close_button);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, Workout viewModel);
    }
}
