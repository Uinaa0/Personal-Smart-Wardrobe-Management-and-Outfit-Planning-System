package com.aiman.smartwardrobe.ui.wardrobe;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.data.entity.WardrobeItem;
import com.aiman.smartwardrobe.databinding.ItemWardrobeBinding;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * WardrobeAdapter — RecyclerView Adapter for the Wardrobe Grid
 * ============================================================================
 *
 * <p>This adapter binds {@link WardrobeItem} data to the wardrobe grid
 * UI (2-column RecyclerView). Each grid cell displays:
 * <ul>
 *   <li>The item's image (loaded via Glide from local storage)</li>
 *   <li>The clothing category label</li>
 *   <li>A colored dot indicator showing the item's color</li>
 *   <li>The fabric type label</li>
 * </ul></p>
 *
 * <p><b>ViewBinding:</b> Uses generated {@link ItemWardrobeBinding} class
 * instead of {@code findViewById()} for type-safe, null-safe view access.</p>
 *
 * <p><b>DiffUtil:</b> Uses {@link DiffUtil} for efficient list updates.
 * Instead of calling {@code notifyDataSetChanged()} (which redraws every
 * item), DiffUtil calculates the minimum set of changes and only updates
 * the affected items. This results in smooth animations and better
 * performance.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.ui.wardrobe.WardrobeFragment
 */
public class WardrobeAdapter extends RecyclerView.Adapter<WardrobeAdapter.WardrobeViewHolder> {

    // =========================================================================
    // DATA & LISTENER
    // =========================================================================

    /** The current list of wardrobe items being displayed */
    private List<WardrobeItem> items;

    /** Callback interface for item click events */
    private final OnItemClickListener listener;

    // =========================================================================
    // CLICK LISTENER INTERFACE
    // =========================================================================

    /**
     * Interface for handling wardrobe item click events.
     *
     * <p>The Fragment implements this interface to respond to clicks
     * (e.g., opening item details, long-press for delete).</p>
     */
    public interface OnItemClickListener {
        /** Called when a wardrobe item card is tapped */
        void onItemClick(WardrobeItem item);

        /** Called when a wardrobe item card is long-pressed */
        void onItemLongClick(WardrobeItem item);
    }

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Create a new WardrobeAdapter.
     *
     * @param listener Callback for item click events
     */
    public WardrobeAdapter(OnItemClickListener listener) {
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    // =========================================================================
    // DATA UPDATE — Using DiffUtil for Efficient Updates
    // =========================================================================

    /**
     * Update the adapter's data set with a new list of items.
     *
     * <p>Uses {@link DiffUtil} to calculate the difference between the
     * old and new lists, then dispatches only the necessary change events
     * (insert, remove, move, change). This approach:
     * <ul>
     *   <li>Provides smooth item animations (fade, slide)</li>
     *   <li>Only redraws changed items (not the entire list)</li>
     *   <li>Is more performant than notifyDataSetChanged()</li>
     * </ul></p>
     *
     * @param newItems The new list of wardrobe items
     */
    public void submitList(List<WardrobeItem> newItems) {
        // Calculate differences between old and new lists
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new WardrobeDiffCallback(this.items, newItems)
        );

        // Replace the data
        this.items = new ArrayList<>(newItems);

        // Dispatch the calculated changes to the RecyclerView
        diffResult.dispatchUpdatesTo(this);
    }

    // =========================================================================
    // RECYCLERVIEW.ADAPTER OVERRIDES
    // =========================================================================

    /**
     * Called when RecyclerView needs a new ViewHolder.
     * Inflates the item_wardrobe.xml layout using ViewBinding.
     */
    @NonNull
    @Override
    public WardrobeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout using ViewBinding (NOT LayoutInflater + findViewById)
        ItemWardrobeBinding binding = ItemWardrobeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new WardrobeViewHolder(binding);
    }

    /**
     * Called to bind data to a ViewHolder for a specific position.
     * This is where we populate the card with item data.
     */
    @Override
    public void onBindViewHolder(@NonNull WardrobeViewHolder holder, int position) {
        WardrobeItem item = items.get(position);
        holder.bind(item);
    }

    /** @return The total number of items in the data set */
    @Override
    public int getItemCount() {
        return items.size();
    }

    // =========================================================================
    // VIEWHOLDER — Holds References to a Single Card's Views
    // =========================================================================

    /**
     * ViewHolder for a single wardrobe item card.
     *
     * <p>The ViewHolder pattern is a core RecyclerView optimization.
     * Instead of calling {@code findViewById()} every time a card is
     * displayed, the ViewHolder caches the view references once and
     * reuses them as the user scrolls.</p>
     *
     * <p>With ViewBinding, the references are generated automatically
     * in the {@link ItemWardrobeBinding} class.</p>
     */
    class WardrobeViewHolder extends RecyclerView.ViewHolder {

        /** Generated ViewBinding class for item_wardrobe.xml */
        private final ItemWardrobeBinding binding;

        WardrobeViewHolder(@NonNull ItemWardrobeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Bind a WardrobeItem's data to this card's views.
         *
         * @param item The WardrobeItem to display
         */
        void bind(WardrobeItem item) {
            // --- Category Label ---
            binding.textCategory.setText(item.getCategory());

            // --- Fabric Type Label ---
            binding.textFabricType.setText(item.getFabricType());

            // --- Color Indicator Dot ---
            // Create a circular drawable and tint it with the item's color.
            // This gives a visual preview of the clothing color on the card.
            try {
                GradientDrawable colorDot = (GradientDrawable)
                        binding.viewColorIndicator.getBackground();
                if (colorDot != null) {
                    colorDot.setColor(Color.parseColor(item.getColorHex()));
                }
            } catch (IllegalArgumentException e) {
                // If the hex color is invalid, fall back to gray
                GradientDrawable colorDot = (GradientDrawable)
                        binding.viewColorIndicator.getBackground();
                if (colorDot != null) {
                    colorDot.setColor(Color.GRAY);
                }
            }

            // --- Item Image ---
            // Load the image from local storage using Glide.
            // Glide handles memory/disk caching, image resizing, and
            // placeholder display while the image loads.
            if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                Glide.with(binding.imageItem.getContext())
                        .load(new File(item.getImagePath()))
                        .placeholder(R.drawable.ic_placeholder_clothing)
                        .error(R.drawable.ic_placeholder_clothing)
                        .centerCrop()
                        .into(binding.imageItem);
            } else {
                // No image available — show placeholder
                binding.imageItem.setImageResource(R.drawable.ic_placeholder_clothing);
            }

            // --- Click Listeners ---
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(item);
                }
                return true; // Consume the long-click event
            });
        }
    }

    // =========================================================================
    // DIFFUTIL CALLBACK — Efficient List Comparison
    // =========================================================================

    /**
     * DiffUtil.Callback implementation for comparing wardrobe item lists.
     *
     * <p>DiffUtil uses the Myers difference algorithm to compute the
     * minimum number of updates needed to transform the old list into
     * the new list. This is significantly more efficient than calling
     * {@code notifyDataSetChanged()} which forces a full redraw.</p>
     */
    private static class WardrobeDiffCallback extends DiffUtil.Callback {

        private final List<WardrobeItem> oldList;
        private final List<WardrobeItem> newList;

        WardrobeDiffCallback(List<WardrobeItem> oldList, List<WardrobeItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        /**
         * Check if two items represent the same entity (by primary key).
         * If this returns true, DiffUtil then calls areContentsTheSame()
         * to check if the item needs to be redrawn.
         */
        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getItemId() ==
                    newList.get(newPos).getItemId();
        }

        /**
         * Check if two items have identical content.
         * If this returns false, the item's ViewHolder is rebound.
         */
        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            WardrobeItem oldItem = oldList.get(oldPos);
            WardrobeItem newItem = newList.get(newPos);
            return oldItem.getCategory().equals(newItem.getCategory())
                    && oldItem.getColorHex().equals(newItem.getColorHex())
                    && oldItem.getFabricType().equals(newItem.getFabricType())
                    && oldItem.getPurchasePrice() == newItem.getPurchasePrice()
                    && (oldItem.getImagePath() != null
                        ? oldItem.getImagePath().equals(newItem.getImagePath())
                        : newItem.getImagePath() == null);
        }
    }
}
