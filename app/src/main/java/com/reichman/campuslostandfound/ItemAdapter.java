package com.reichman.campuslostandfound;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Turns a list of Item objects into rows for the RecyclerView.
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    // The list of items we're displaying
    private final List<Item> items;

    // Lets the Feed screen know which item was tapped (for opening details later)
    public interface OnItemClickListener {
        void onItemClick(Item item);
    }
    private final OnItemClickListener clickListener;

    public ItemAdapter(List<Item> items, OnItemClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }

    // A ViewHolder holds the views for ONE row, so we don't re-find them every time
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemTitle;
        TextView itemLocation;
        TextView itemTime;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemTitle = itemView.findViewById(R.id.itemTitle);
            itemLocation = itemView.findViewById(R.id.itemLocation);
            itemTime = itemView.findViewById(R.id.itemTime);
        }
    }

    // Called when the list needs a NEW empty row — inflates our item_row layout
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_row, parent, false);
        return new ItemViewHolder(view);
    }

    // Called to FILL a row with the data for the item at this position
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);

        holder.itemTitle.setText(item.getTitle());
        holder.itemLocation.setText(item.getLocationLabel());

        // Convert the timestamp into a readable date
        String timeText = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                .format(new Date(item.getCreatedAt()));
        holder.itemTime.setText(timeText);

        // Show the item's bundled image, or a placeholder
        holder.itemImage.setImageResource(
                ItemImages.getImageResource(holder.itemImage.getContext(), item.getPhotoUrl()));

        // When this row is tapped, tell the Feed screen which item it was
        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(item));
    }

    // How many rows total
    @Override
    public int getItemCount() {
        return items.size();
    }
}