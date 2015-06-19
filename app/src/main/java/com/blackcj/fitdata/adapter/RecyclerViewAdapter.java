package com.blackcj.fitdata.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.activity.MainActivity;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutReport;
import com.blackcj.fitdata.model.WorkoutTypes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by chris.black on 5/2/15.
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> implements View.OnClickListener {

    private List<Workout> items;
    private OnItemClickListener onItemClickListener;
    private String timeDesc = "Today";
    private boolean animate = true;

    public RecyclerViewAdapter(List<Workout> items, Context context) {
        this.items = items;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    /**
     * Do some fun animation for remove and add. If the data is fresh or some how smaller,
     * than just reset the data set normally.
     *
     * @param newItems
     * @param time
     */
    public void setItems(final List<Workout> newItems, final String time) {
        timeDesc = time;
        if(newItems.size() > items.size()) {
            lastPosition = 0;
        }
        if(items.size() > 0 && newItems.size() > 0) {
            items.set(0, newItems.get(0));
            notifyItemChanged(0);
            if(items.size() > 1) {
                int itemSize = items.size() - 1;
                for (int i = itemSize; i > 0; i--) {
                    if (i > lastPosition) {
                        items.remove(i);
                        notifyItemRemoved(i);
                    }
                }
            }
            if(newItems.size() > 1) {
                for (int i = 1; i < newItems.size(); i++) {
                    if (i > lastPosition) {
                        items.add(newItems.get(i));
                        notifyItemInserted(i);
                    } else {
                        items.set(i, newItems.get(i));
                        notifyItemChanged(i);
                    }
                }
            }
        } else {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }
    }

    public void setNeedsAnimate() {
        lastPosition = 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
        v.setOnClickListener(this);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Workout item;
        item = items.get(position);

        if(item.type == WorkoutTypes.TIME.getValue()) {
            holder.text.setText(timeDesc);
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
        setAnimation(holder.container, position);
    }

    private Context context;
    private int lastPosition = 0;

    /**
     * Here is the key method to apply the animation
     */
    private void setAnimation(View viewToAnimate, int position)
    {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition && position > 0)
        {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
            animation.setInterpolator(new DecelerateInterpolator());
            if(position < 6) {
                animation.setStartOffset(150 * (position - 1));
            } else if (position % 2 != 0) {
                animation.setStartOffset(150);
            }
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void onClick(final View v) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(v, (Workout) v.getTag());
        }
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView text;
        public TextView detail;
        public LinearLayout container;

        public ViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);
            text = (TextView) itemView.findViewById(R.id.text);
            detail = (TextView) itemView.findViewById(R.id.summary_text);
            container = (LinearLayout) itemView.findViewById(R.id.container);
        }
    }

    public interface OnItemClickListener {

        void onItemClick(View view, Workout viewModel);

    }
}