package com.aiman.smartwardrobe.ui.stylist;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.data.entity.WardrobeItem;
import com.aiman.smartwardrobe.databinding.ItemSelectClothingBinding;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * OutfitAdapter — RecyclerView Adapter for Outfit Selection Dialog
 * ============================================================================
 *
 * <p>Binds clothing items to cards inside the selector BottomSheet dialog.
 * Includes a visual highlight (border and elevation change) if the item is
 * currently selected on the stylist canvas.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class OutfitAdapter extends RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder> {

    private List<WardrobeItem> items;
    private final OnItemClickListener listener;
    private long selectedItemId = -1;

    public interface OnItemClickListener {
        void onItemSelect(WardrobeItem item);
    }

    public OutfitAdapter(OnItemClickListener listener) {
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    public void submitList(List<WardrobeItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return items.get(oldItemPosition).getItemId() == newItems.get(newItemPosition).getItemId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                WardrobeItem oldItem = items.get(oldItemPosition);
                WardrobeItem newItem = newItems.get(newItemPosition);
                return oldItem.getCategory().equals(newItem.getCategory())
                        && oldItem.getColorHex().equals(newItem.getColorHex())
                        && oldItem.getFabricType().equals(newItem.getFabricType())
                        && (oldItem.getImagePath() != null
                            ? oldItem.getImagePath().equals(newItem.getImagePath())
                            : newItem.getImagePath() == null);
            }
        });

        this.items = new ArrayList<>(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Sets the ID of the item currently loaded on the canvas.
     * Triggers a UI refresh to draw the highlight stroke on the matching card.
     */
    public void setSelectedItemId(long selectedItemId) {
        this.selectedItemId = selectedItemId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OutfitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSelectClothingBinding binding = ItemSelectClothingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OutfitViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OutfitViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class OutfitViewHolder extends RecyclerView.ViewHolder {
        private final ItemSelectClothingBinding binding;

        OutfitViewHolder(@NonNull ItemSelectClothingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WardrobeItem item) {
            binding.textSelectCategory.setText(item.getCategory());
            binding.textSelectFabric.setText(item.getFabricType());

            // Color Indicator Dot
            try {
                GradientDrawable colorDot = (GradientDrawable)
                        binding.viewSelectColorIndicator.getBackground();
                if (colorDot != null) {
                    colorDot.setColor(Color.parseColor(item.getColorHex()));
                }
            } catch (IllegalArgumentException e) {
                GradientDrawable colorDot = (GradientDrawable)
                        binding.viewSelectColorIndicator.getBackground();
                if (colorDot != null) {
                    colorDot.setColor(Color.GRAY);
                }
            }

            // Image Preview
            if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                Glide.with(binding.imageSelectItem.getContext())
                        .load(new File(item.getImagePath()))
                        .placeholder(R.drawable.ic_placeholder_clothing)
                        .error(R.drawable.ic_placeholder_clothing)
                        .centerCrop()
                        .into(binding.imageSelectItem);
            } else {
                binding.imageSelectItem.setImageResource(R.drawable.ic_placeholder_clothing);
            }

            // Highlight border if this item is selected on the canvas
            if (item.getItemId() == selectedItemId) {
                int primaryColor = ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.primary);
                binding.cardSelectItem.setStrokeColor(primaryColor);
                binding.cardSelectItem.setStrokeWidth(6); // Bold highlight
                binding.cardSelectItem.setCardElevation(4f);
            } else {
                int outlineColor = ContextCompat.getColor(
                        binding.getRoot().getContext(), com.google.android.material.R.color.material_dynamic_neutral_variant80);
                binding.cardSelectItem.setStrokeColor(outlineColor);
                binding.cardSelectItem.setStrokeWidth(2); // Normal outline
                binding.cardSelectItem.setCardElevation(0f);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemSelect(item);
                }
            });
        }
    }
}
