package com.aiman.smartwardrobe.ui.analytics;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.data.entity.CategoryCount;
import com.aiman.smartwardrobe.data.entity.ItemWearStats;
import com.aiman.smartwardrobe.data.entity.WearHistoryEntry;
import com.aiman.smartwardrobe.databinding.FragmentAnalyticsBinding;
import com.aiman.smartwardrobe.ui.MainActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ============================================================================
 * AnalyticsFragment — Module 4: Redesigned Analytics Dashboard UI
 * ============================================================================
 */
public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;
    private AnalyticsViewModel viewModel;

    // State for tabs: 0 = Most Worn, 1 = Least Worn, 2 = Wear History
    private static final int TAB_MOST_WORN = 0;
    private static final int TAB_LEAST_WORN = 1;
    private static final int TAB_WEAR_HISTORY = 2;
    private int selectedTab = TAB_MOST_WORN;

    private List<ItemWearStats> currentMostWornList = new ArrayList<>();
    private List<ItemWearStats> currentLeastWornList = new ArrayList<>();
    private List<WearHistoryEntry> currentWearHistoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        AnalyticsViewModel.Factory factory = new AnalyticsViewModel.Factory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(this, factory)
                .get(AnalyticsViewModel.class);

        setupHeaderActions();
        setupTabClickListeners();
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // =========================================================================
    // SETUP ACTIONS
    // =========================================================================

    private void setupHeaderActions() {
        // Tapping profile or settings in the header navigates to the Profile Fragment
        // tab
        View.OnClickListener goToProfileTab = v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                BottomNavigationView navView = mainActivity.findViewById(R.id.bottom_navigation);
                if (navView != null) {
                    navView.setSelectedItemId(R.id.nav_profile);
                }
            }
        };
        binding.btnHeaderProfile.setOnClickListener(goToProfileTab);
        binding.btnHeaderSettings.setOnClickListener(goToProfileTab);
    }

    private void setupTabClickListeners() {
        binding.tabMostWorn.setOnClickListener(v -> {
            if (selectedTab != TAB_MOST_WORN) {
                selectedTab = TAB_MOST_WORN;
                updateTabStyles();
                showWornItemsList();
                buildItemList(binding.layoutWornItemsList, currentMostWornList);
            }
        });

        binding.tabLeastWorn.setOnClickListener(v -> {
            if (selectedTab != TAB_LEAST_WORN) {
                selectedTab = TAB_LEAST_WORN;
                updateTabStyles();
                showWornItemsList();
                buildItemList(binding.layoutWornItemsList, currentLeastWornList);
            }
        });

        binding.tabWearHistory.setOnClickListener(v -> {
            if (selectedTab != TAB_WEAR_HISTORY) {
                selectedTab = TAB_WEAR_HISTORY;
                updateTabStyles();
                showWearHistoryList();
                buildWearHistoryList(currentWearHistoryList);
            }
        });
    }

    /**
     * Show the worn items list container and hide the wear history container.
     */
    private void showWornItemsList() {
        binding.layoutWornItemsList.setVisibility(View.VISIBLE);
        binding.layoutWearHistoryList.setVisibility(View.GONE);
    }

    /**
     * Show the wear history list container and hide the worn items container.
     */
    private void showWearHistoryList() {
        binding.layoutWornItemsList.setVisibility(View.GONE);
        binding.layoutWearHistoryList.setVisibility(View.VISIBLE);
    }

    private void updateTabStyles() {
        int activeColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int inactiveColor = ColorUtils.parseHexColor("#8A9AA4");

        // Most Worn tab
        binding.textTabMostWorn.setTextColor(selectedTab == TAB_MOST_WORN ? activeColor : inactiveColor);
        binding.indicatorTabMostWorn.setVisibility(selectedTab == TAB_MOST_WORN ? View.VISIBLE : View.INVISIBLE);

        // Least Worn tab
        binding.textTabLeastWorn.setTextColor(selectedTab == TAB_LEAST_WORN ? activeColor : inactiveColor);
        binding.indicatorTabLeastWorn.setVisibility(selectedTab == TAB_LEAST_WORN ? View.VISIBLE : View.INVISIBLE);

        // Wear History tab
        binding.textTabWearHistory.setTextColor(selectedTab == TAB_WEAR_HISTORY ? activeColor : inactiveColor);
        binding.indicatorTabWearHistory.setVisibility(selectedTab == TAB_WEAR_HISTORY ? View.VISIBLE : View.INVISIBLE);
    }

    // =========================================================================
    // VIEWMODEL OBSERVATION
    // =========================================================================

    private void observeViewModel() {
        // Total Items
        viewModel.getTotalItems().observe(getViewLifecycleOwner(), count -> {
            if (binding == null)
                return;
            if (count != null && count > 0) {
                binding.layoutDataContent.setVisibility(View.VISIBLE);
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.textTotalItemsValue.setText(String.valueOf(count));
            } else {
                binding.layoutDataContent.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
            }
        });

        // Total Value
        viewModel.getTotalValue().observe(getViewLifecycleOwner(), value -> {
            if (binding == null)
                return;
            if (value != null) {
                binding.textTotalValue.setText(
                        String.format(Locale.getDefault(), "$%,.0f", value));
            }
        });

        // Average CPW
        viewModel.getAverageCpw().observe(getViewLifecycleOwner(), cpw -> {
            if (binding == null)
                return;
            if (cpw != null && cpw > 0) {
                binding.textAvgCpw.setText(
                        String.format(Locale.getDefault(), "$%.2f", cpw));
            } else {
                binding.textAvgCpw.setText("—");
            }
        });

        // Category Distribution
        viewModel.getCategoryDistribution().observe(getViewLifecycleOwner(), this::buildCategoryBars);

        // Most Worn Items
        viewModel.getMostWornItems().observe(getViewLifecycleOwner(), items -> {
            if (binding == null)
                return;
            currentMostWornList = items != null ? items : new ArrayList<>();
            if (selectedTab == TAB_MOST_WORN) {
                buildItemList(binding.layoutWornItemsList, currentMostWornList);
            }
        });

        // Least Worn Items
        viewModel.getLeastWornItems().observe(getViewLifecycleOwner(), items -> {
            if (binding == null)
                return;
            currentLeastWornList = items != null ? items : new ArrayList<>();
            if (selectedTab == TAB_LEAST_WORN) {
                buildItemList(binding.layoutWornItemsList, currentLeastWornList);
            }
        });

        // Wear History
        viewModel.getWearHistory().observe(getViewLifecycleOwner(), entries -> {
            if (binding == null)
                return;
            currentWearHistoryList = entries != null ? entries : new ArrayList<>();
            if (selectedTab == TAB_WEAR_HISTORY) {
                buildWearHistoryList(currentWearHistoryList);
            }
        });
    }

    // =========================================================================
    // UI BUILDERS
    // =========================================================================
    private void buildCategoryBars(@Nullable List<CategoryCount> distribution) {
        if (binding == null)
            return;

        // Build and bind canvas donut chart slices
        List<PieChartView.Slice> slices = new ArrayList<>();
        if (distribution != null) {
            for (CategoryCount cc : distribution) {
                String category = cc.getCategory();
                int count = cc.getCount();
                int color = android.graphics.Color.parseColor(getCategoryColor(category));
                slices.add(new PieChartView.Slice(category, count, color));
            }
        }
        binding.pieChartView.setData(slices);

        GridLayout container = binding.gridCategoryBars;
        container.removeAllViews();

        if (distribution == null || distribution.isEmpty())
            return;

        // Calculate total count across all categories to get percentage
        int totalCount = 0;
        for (CategoryCount cc : distribution) {
            totalCount += cc.getCount();
        }

        // Find max count for proportional bar sizing (weight calculation)
        int maxCount = 0;
        for (CategoryCount cc : distribution) {
            if (cc.getCount() > maxCount) {
                maxCount = cc.getCount();
            }
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < distribution.size(); i++) {
            CategoryCount cc = distribution.get(i);
            String category = cc.getCategory();
            int count = cc.getCount();

            View card = inflater.inflate(R.layout.item_analytics_category, container, false);

            // Map category to color and icon
            String colorHex = getCategoryColor(category);
            int iconRes = getCategoryIcon(category);

            // Percentage
            double percentageVal = totalCount > 0 ? ((double) count / totalCount * 100.0) : 0;
            String percentageText = String.format(Locale.getDefault(), "%.0f%%", percentageVal);

            // Setup views in card
            ImageView iconView = card.findViewById(R.id.image_category_icon);
            View iconContainer = card.findViewById(R.id.card_category_icon_container);
            TextView nameText = card.findViewById(R.id.text_category_name);
            TextView percentageTextV = card.findViewById(R.id.text_category_percentage);
            TextView colorCodeText = card.findViewById(R.id.text_category_color_code);
            TextView countText = card.findViewById(R.id.text_category_count);

            View progressFill = card.findViewById(R.id.view_progress_fill);
            View progressEmpty = card.findViewById(R.id.view_progress_empty);

            // Set data
            iconView.setImageResource(iconRes);
            int colorInt = ColorUtils.parseHexColor(colorHex);
            iconView.setImageTintList(android.content.res.ColorStateList.valueOf(colorInt));

            // Icon container background tint: semi-transparent version of color (15%
            // opacity)
            int tintedBg = android.graphics.Color.argb(38, android.graphics.Color.red(colorInt),
                    android.graphics.Color.green(colorInt), android.graphics.Color.blue(colorInt));
            ((com.google.android.material.card.MaterialCardView) iconContainer).setCardBackgroundColor(tintedBg);

            nameText.setText(category);
            percentageTextV.setText(percentageText);
            colorCodeText.setText(colorHex);
            colorCodeText.setTextColor(colorInt);
            countText.setText(String.valueOf(count));

            // Progress bar fill proportional weights
            float proportion = maxCount > 0 ? (float) count / maxCount : 0f;
            proportion = Math.max(proportion, 0.08f); // Minimum fill for visibility

            // Update weights
            LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) progressFill.getLayoutParams();
            fillParams.weight = proportion;
            progressFill.setLayoutParams(fillParams);

            // Progress fill color drawable
            GradientDrawable fillDrawable = new GradientDrawable();
            fillDrawable.setShape(GradientDrawable.RECTANGLE);
            fillDrawable.setColor(colorInt);
            fillDrawable.setCornerRadius(dpToPx(3));
            progressFill.setBackground(fillDrawable);

            LinearLayout.LayoutParams emptyParams = (LinearLayout.LayoutParams) progressEmpty.getLayoutParams();
            emptyParams.weight = 1.0f - proportion;
            progressEmpty.setLayoutParams(emptyParams);

            // Set Grid parameters for 2-column layout
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            card.setLayoutParams(params);

            container.addView(card);
        }
    }

    private void buildItemList(LinearLayout container, @Nullable List<ItemWearStats> items) {
        if (binding == null)
            return;
        container.removeAllViews();

        if (items == null || items.isEmpty())
            return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < items.size(); i++) {
            ItemWearStats item = items.get(i);
            View row = inflater.inflate(R.layout.item_analytics_wear, container, false);

            // Image thumbnail
            ImageView thumbnail = row.findViewById(R.id.image_item_thumbnail);
            if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                Glide.with(this)
                        .load(new File(item.getImagePath()))
                        .placeholder(R.drawable.ic_placeholder_clothing)
                        .error(R.drawable.ic_placeholder_clothing)
                        .centerCrop()
                        .into(thumbnail);
            } else {
                thumbnail.setImageResource(R.drawable.ic_placeholder_clothing);
            }

            // Dynamic descriptive title
            TextView titleText = row.findViewById(R.id.text_item_category);
            titleText.setText(formatItemTitle(item));

            // Stats details
            TextView wearCountText = row.findViewById(R.id.text_item_wear_count);
            wearCountText.setText(String.format(Locale.getDefault(),
                    "Total Wears: %d  •  CPW: $%.2f",
                    item.getWearCount(),
                    item.getCostPerWear()));

            // Rank rank number
            TextView rankText = row.findViewById(R.id.text_item_rank);
            rankText.setText(String.valueOf(i + 1));

            container.addView(row);
        }
    }

    // =========================================================================
    // COLOR & ICON MAPPINGS
    // =========================================================================

    private String getCategoryColor(String category) {
        if (category == null)
            return "#8A9AA4";
        switch (category.toLowerCase().trim()) {
            case "t-shirt":
            case "t-shirts":
            case "shirt":
            case "shirts":
                return "#38B2AC"; // Teal
            case "pants":
            case "jeans":
            case "shorts":
                return "#805AD5"; // Purple
            case "shoes":
            case "sneakers":
                return "#E53E3E"; // Red
            case "jacket":
            case "jackets":
            case "hoodie":
            case "sweater":
                return "#F6AD55"; // Orange
            default:
                return "#8A9AA4"; // Grey
        }
    }

    private int getCategoryIcon(String category) {
        if (category == null)
            return R.drawable.ic_tshirt;
        switch (category.toLowerCase().trim()) {
            case "t-shirt":
            case "t-shirts":
            case "shirt":
            case "shirts":
                return R.drawable.ic_tshirt;
            case "pants":
            case "jeans":
            case "shorts":
                return R.drawable.ic_pants;
            case "shoes":
            case "sneakers":
                return R.drawable.ic_shoe;
            case "jacket":
            case "jackets":
            case "hoodie":
            case "sweater":
                return R.drawable.ic_jacket;
            default:
                return R.drawable.ic_tshirt;
        }
    }

    private String formatItemTitle(ItemWearStats item) {
        String colorName = getColorName(item.getColorHex());
        String fabric = item.getFabricType() != null ? item.getFabricType() : "";
        String category = item.getCategory() != null ? item.getCategory() : "Item";

        StringBuilder sb = new StringBuilder();
        if (!colorName.isEmpty()) {
            sb.append(colorName).append(" ");
        }
        if (!fabric.isEmpty()) {
            sb.append(fabric).append(" ");
        }
        sb.append(category);
        return sb.toString();
    }

    private String getColorName(String hex) {
        if (hex == null)
            return "";
        hex = hex.toUpperCase().trim();
        switch (hex) {
            case "#FF5733":
                return "Orange";
            case "#0000FF":
                return "Blue";
            case "#000000":
                return "Black";
            case "#FFFFFF":
                return "White";
            case "#FF00FF":
                return "Pink";
            case "#FFFF00":
                return "Yellow";
            case "#00FFFF":
                return "Cyan";
            case "#808080":
                return "Grey";
            case "#87CEEB":
                return "Sky Blue";
            case "#8B4513":
                return "Brown";
            default:
                return "";
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // =========================================================================
    // WEAR HISTORY LIST BUILDER
    // =========================================================================

    /**
     * Build the wear history list from WearHistoryEntry objects.
     * Each row shows: item thumbnail, title (category + fabric), and formatted date.
     */
    private void buildWearHistoryList(@Nullable List<WearHistoryEntry> entries) {
        if (binding == null) return;

        LinearLayout container = binding.layoutWearHistoryList;
        container.removeAllViews();

        if (entries == null || entries.isEmpty()) {
            // Show empty state message
            TextView emptyText = new TextView(requireContext());
            emptyText.setText(R.string.wear_history_empty);
            emptyText.setTextColor(ColorUtils.parseHexColor("#8A9AA4"));
            emptyText.setTextSize(14);
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32));
            container.addView(emptyText);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (WearHistoryEntry entry : entries) {
            View row = inflater.inflate(R.layout.item_wear_history, container, false);

            // Thumbnail
            ImageView thumbnail = row.findViewById(R.id.image_history_item);
            if (entry.getImagePath() != null && !entry.getImagePath().isEmpty()) {
                Glide.with(this)
                        .load(new File(entry.getImagePath()))
                        .placeholder(R.drawable.ic_placeholder_clothing)
                        .error(R.drawable.ic_placeholder_clothing)
                        .centerCrop()
                        .into(thumbnail);
            } else {
                thumbnail.setImageResource(R.drawable.ic_placeholder_clothing);
            }

            // Item title (Category + Fabric)
            TextView titleText = row.findViewById(R.id.text_history_item_title);
            String fabric = entry.getFabricType() != null ? entry.getFabricType() : "";
            String category = entry.getCategory() != null ? entry.getCategory() : "Item";
            String title = (!fabric.isEmpty() ? fabric + " " : "") + category;
            titleText.setText(title);

            // Date worn
            TextView dateText = row.findViewById(R.id.text_history_date);
            dateText.setText(dateFormat.format(new Date(entry.getDateWorn())));

            // Color dot
            View colorDot = row.findViewById(R.id.view_history_color_dot);
            try {
                GradientDrawable dotDrawable = (GradientDrawable) colorDot.getBackground();
                if (dotDrawable != null && entry.getColorHex() != null) {
                    dotDrawable.setColor(android.graphics.Color.parseColor(entry.getColorHex()));
                }
            } catch (Exception e) {
                // Ignore color parse errors
            }

            container.addView(row);
        }
    }

    /**
     * Helper to safely parse colors in Java.
     */
    private static class ColorUtils {
        public static int parseHexColor(String hex) {
            try {
                return android.graphics.Color.parseColor(hex);
            } catch (Exception e) {
                return android.graphics.Color.GRAY;
            }
        }
    }
}
