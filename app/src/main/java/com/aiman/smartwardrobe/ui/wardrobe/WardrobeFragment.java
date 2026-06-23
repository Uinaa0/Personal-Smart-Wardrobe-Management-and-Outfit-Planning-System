package com.aiman.smartwardrobe.ui.wardrobe;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.data.entity.WardrobeItem;
import com.aiman.smartwardrobe.databinding.FragmentWardrobeBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * ============================================================================
 * WardrobeFragment — Module 1: The Digital Inventory Screen
 * ============================================================================
 *
 * <p>This fragment displays the user's entire clothing wardrobe in a
 * 2-column grid (RecyclerView with GridLayoutManager). It is the primary
 * screen for Module 1 — "The Digital Inventory".</p>
 *
 * <p><b>UI Components:</b></p>
 * <ul>
 *   <li><b>Category Filter Chips:</b> A horizontally scrollable row of
 *       Material 3 Chip components. Tapping a chip filters the grid to
 *       show only items of that category. An "All" chip shows everything.</li>
 *   <li><b>Wardrobe Grid:</b> A 2-column RecyclerView displaying
 *       MaterialCardView items with the clothing image, category label,
 *       color dot indicator, and fabric type.</li>
 *   <li><b>FAB (Floating Action Button):</b> Opens the AddItemActivity
 *       form for adding a new clothing item to the wardrobe.</li>
 *   <li><b>Empty State:</b> When the wardrobe is empty, a placeholder
 *       message encourages the user to add their first item.</li>
 * </ul>
 *
 * <p><b>MVVM Architecture:</b>
 * This Fragment only handles UI rendering and user interaction.
 * All data and business logic is in the {@link WardrobeViewModel}.
 * The Fragment observes LiveData from the ViewModel and updates the
 * UI reactively when data changes.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class WardrobeFragment extends Fragment
        implements WardrobeAdapter.OnItemClickListener {

    // =========================================================================
    // VIEW BINDING & VIEWMODEL
    // =========================================================================

    /** Generated ViewBinding class for fragment_wardrobe.xml */
    private FragmentWardrobeBinding binding;

    /** MVVM ViewModel — holds and manages the wardrobe data */
    private WardrobeViewModel viewModel;

    /** RecyclerView adapter for the wardrobe grid */
    private WardrobeAdapter adapter;

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** Number of columns in the wardrobe grid */
    private static final int GRID_SPAN_COUNT = 2;

    // =========================================================================
    // FRAGMENT LIFECYCLE
    // =========================================================================

    /**
     * Inflate the fragment layout using ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWardrobeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Set up the UI components, ViewModel, and data observers.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupViewModel();
        setupRecyclerView();
        setupFab();
        observeData();
    }

    /**
     * Clean up the ViewBinding reference to prevent memory leaks.
     *
     * <p>Fragments can outlive their views (e.g., when placed on the
     * back stack). Setting binding to null ensures we don't hold
     * references to destroyed views.</p>
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // =========================================================================
    // SETUP METHODS
    // =========================================================================

    /**
     * Initialize the ViewModel using the custom Factory.
     *
     * <p>The Factory injects the WardrobeRepository into the ViewModel.
     * {@code ViewModelProvider} ensures only ONE ViewModel instance exists
     * per Fragment lifecycle — surviving configuration changes.</p>
     */
    private void setupViewModel() {
        WardrobeViewModel.Factory factory =
                new WardrobeViewModel.Factory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(this, factory)
                .get(WardrobeViewModel.class);
    }

    /**
     * Configure the RecyclerView with a 2-column GridLayoutManager
     * and attach the WardrobeAdapter.
     *
     * <p><b>GridLayoutManager:</b> Arranges items in a 2-column grid.
     * The span count (2) provides a good balance between showing enough
     * items per screen and keeping each item card large enough to see
     * the clothing image clearly.</p>
     */
    private void setupRecyclerView() {
        adapter = new WardrobeAdapter(this);
        binding.recyclerWardrobe.setLayoutManager(
                new GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        );
        binding.recyclerWardrobe.setAdapter(adapter);

        // Add item decoration for spacing between grid cells
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        binding.recyclerWardrobe.addItemDecoration(
                new GridSpacingItemDecoration(GRID_SPAN_COUNT, spacing, true)
        );
    }

    /**
     * Set up the Floating Action Button to launch the Add Item screen.
     */
    private void setupFab() {
        binding.fabAddItem.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddItemActivity.class);
            startActivity(intent);
        });
    }

    // =========================================================================
    // DATA OBSERVERS — Reactive UI Updates
    // =========================================================================

    /**
     * Observe LiveData from the ViewModel and update the UI.
     *
     * <p>This is the core of the MVVM pattern's reactive data binding:
     * <ol>
     *   <li>Room emits new data via Flowable whenever the DB changes</li>
     *   <li>Repository passes the Flowable to the ViewModel</li>
     *   <li>ViewModel converts Flowable → LiveData</li>
     *   <li>Fragment observes LiveData (THIS METHOD)</li>
     *   <li>When data changes, the observer callback updates the UI</li>
     * </ol></p>
     */
    private void observeData() {
        // Observe wardrobe items and update the RecyclerView
        viewModel.getWardrobeItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);

            // Show/hide empty state based on whether there are items
            if (items == null || items.isEmpty()) {
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerWardrobe.setVisibility(View.GONE);
            } else {
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.recyclerWardrobe.setVisibility(View.VISIBLE);
            }
        });

        // Observe categories and update the filter chip group
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            binding.chipGroupCategories.removeAllViews();

            // Add the "All" chip first (always present)
            Chip allChip = createFilterChip("All", true);
            binding.chipGroupCategories.addView(allChip);

            // Add a chip for each category found in the database
            for (String category : categories) {
                Chip chip = createFilterChip(category, false);
                binding.chipGroupCategories.addView(chip);
            }
        });
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Create a Material 3 filter chip for category selection.
     *
     * @param text       The chip label (category name or "All")
     * @param isChecked  Whether this chip should be initially selected
     * @return A configured Chip view
     */
    private Chip createFilterChip(String text, boolean isChecked) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(isChecked);
        chip.setChipBackgroundColorResource(R.color.chip_background_selector);
        chip.setTextColor(getResources().getColorStateList(
                R.color.chip_text_selector, requireContext().getTheme()));

        // Handle chip selection — filter items by category
        chip.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                // Uncheck all other chips
                for (int i = 0; i < binding.chipGroupCategories.getChildCount(); i++) {
                    View child = binding.chipGroupCategories.getChildAt(i);
                    if (child instanceof Chip && child != buttonView) {
                        ((Chip) child).setChecked(false);
                    }
                }

                // Apply the category filter
                if ("All".equals(text)) {
                    viewModel.setSelectedCategory(null); // null = no filter
                } else {
                    viewModel.setSelectedCategory(text);
                }
            }
        });

        return chip;
    }

    // =========================================================================
    // ITEM CLICK HANDLERS
    // =========================================================================

    /**
     * Handle tap on a wardrobe item card.
     * Currently shows a Snackbar with item details.
     * In a full implementation, this would open an item detail screen.
     */
    @Override
    public void onItemClick(WardrobeItem item) {
        Snackbar.make(
                binding.getRoot(),
                item.getCategory() + " — " + item.getFabricType(),
                Snackbar.LENGTH_SHORT
        ).show();
    }

    /**
     * Handle long-press on a wardrobe item card.
     * Shows a confirmation dialog to delete the item.
     */
    @Override
    public void onItemLongClick(WardrobeItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to remove this "
                        + item.getCategory() + " from your wardrobe?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteItem(item);
                    Snackbar.make(
                            binding.getRoot(),
                            item.getCategory() + " removed",
                            Snackbar.LENGTH_SHORT
                    ).show();
                })
                .show();
    }

    // =========================================================================
    // INNER CLASS — Grid Spacing Item Decoration
    // =========================================================================

    /**
     * RecyclerView.ItemDecoration that adds uniform spacing between
     * grid items. This creates visual separation between the cards.
     */
    public static class GridSpacingItemDecoration
            extends RecyclerView.ItemDecoration {

        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing,
                                         boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}
