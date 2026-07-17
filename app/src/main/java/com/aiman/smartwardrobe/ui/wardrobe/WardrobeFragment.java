package com.aiman.smartwardrobe.ui.wardrobe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.aiman.smartwardrobe.ui.auth.LoginActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.widget.TextView;
import android.widget.ImageView;

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

    // Weather & Location Configuration (Weather Feature)
    private static final String DEFAULT_CITY = "Kuala Lumpur";

    private String getWeatherApiKey() {
        if (getContext() == null) return "";
        return getContext().getSharedPreferences(LoginActivity.PREFS_AUTH, Context.MODE_PRIVATE)
                .getString("weather_api_key", "");
    }

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                    fetchLocalLocationAndWeather();
                } else {
                    fetchWeatherByCity(DEFAULT_CITY);
                }
            });

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
        setupSearch();
        setupToolbarSort();
        setupWeatherStatus();
        observeData();
        checkLocationPermissionsAndStart();
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

        // Swipe-to-delete touch helper configuration
        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback swipeCallback =
                new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0,
                        androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                WardrobeItem item = adapter.getItemAt(position);
                if (item != null) {
                    deleteItemWithUndo(item);
                }
            }
        };
        new androidx.recyclerview.widget.ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerWardrobe);
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

    /**
     * Set up the search bar with a TextWatcher that triggers
     * a database search on every keystroke.
     */
    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Inflate sorting options menu in the toolbar and handle selections.
     */
    private void setupToolbarSort() {
        binding.toolbarWardrobe.inflateMenu(R.menu.menu_wardrobe_sort);
        binding.toolbarWardrobe.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.sort_newest) {
                viewModel.setSortOrder(WardrobeViewModel.SortOrder.NEWEST);
                return true;
            } else if (id == R.id.sort_alphabetical) {
                viewModel.setSortOrder(WardrobeViewModel.SortOrder.ALPHABETICAL);
                return true;
            } else if (id == R.id.sort_price_high) {
                viewModel.setSortOrder(WardrobeViewModel.SortOrder.PRICE_HIGH);
                return true;
            } else if (id == R.id.sort_price_low) {
                viewModel.setSortOrder(WardrobeViewModel.SortOrder.PRICE_LOW);
                return true;
            }
            return false;
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
            rebuildFilterChips(categories, viewModel.getCategoryCounts().getValue());
        });

        // Observe category counts and update chips
        viewModel.getCategoryCounts().observe(getViewLifecycleOwner(), counts -> {
            rebuildFilterChips(viewModel.getCategories().getValue(), counts);
        });

        // Observe errors and show Snackbar
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty() && getView() != null) {
                Snackbar.make(getView(), errorMsg, Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });

        // Observe weather loading state
        viewModel.getIsWeatherLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding == null) return;
            if (isLoading != null && isLoading) {
                binding.cardWeatherStatus.setVisibility(View.VISIBLE);
                binding.textWardrobeWeatherTitle.setText("Loading local weather...");
                binding.textWardrobeWeatherAlert.setText("Fetching current location temperature...");
            }
        });

        // Observe weather info updates
        viewModel.getCurrentTemperature().observe(getViewLifecycleOwner(), temp -> {
            if (binding == null) return;
            if (temp == null) {
                binding.cardWeatherStatus.setVisibility(View.VISIBLE);
                int surfaceVariant = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant);
                int onSurfaceVariant = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant);

                binding.cardWeatherStatus.setCardBackgroundColor(ColorStateList.valueOf(surfaceVariant));
                binding.cardWeatherStatus.setStrokeColor(ColorStateList.valueOf(surfaceVariant));
                binding.textWardrobeWeatherTitle.setTextColor(onSurfaceVariant);
                binding.textWardrobeWeatherAlert.setTextColor(onSurfaceVariant);

                binding.textWardrobeWeatherTitle.setText("Weather Filter: Off");
                binding.textWardrobeWeatherAlert.setText("Tap here to simulate or fetch live weather.");
                binding.imageWardrobeWeatherIcon.setImageTintList(ColorStateList.valueOf(onSurfaceVariant));
                return;
            }

            binding.cardWeatherStatus.setVisibility(View.VISIBLE);
            String desc = viewModel.getWeatherDescription().getValue();
            String city = viewModel.getLocationName().getValue();

            // Title displaying: e.g. "Kuala Lumpur: 31.2°C, Light rain"
            String formattedTemp = String.format(java.util.Locale.getDefault(), "%.1f°C", temp);
            binding.textWardrobeWeatherTitle.setText(city + ": " + formattedTemp + " (" + desc + ")");

            // Red warning styling if temperature is too hot (>= 28°C)
            if (temp >= 28.0) {
                binding.cardWeatherStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#C53030")));
                binding.cardWeatherStatus.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#9B2C2C")));
                binding.textWardrobeWeatherTitle.setTextColor(Color.WHITE);
                binding.textWardrobeWeatherAlert.setTextColor(Color.WHITE);
                binding.imageWardrobeWeatherIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                binding.textWardrobeWeatherAlert.setText("🚨 It is too hot today! Showing lightweight clothes.");
            } else {
                // Return to normal primaryContainer theme styling
                int primaryContainer = getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer);
                int onPrimaryContainer = getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer);

                binding.cardWeatherStatus.setCardBackgroundColor(ColorStateList.valueOf(primaryContainer));
                binding.cardWeatherStatus.setStrokeColor(ColorStateList.valueOf(primaryContainer));
                binding.textWardrobeWeatherTitle.setTextColor(onPrimaryContainer);
                binding.textWardrobeWeatherAlert.setTextColor(onPrimaryContainer);
                binding.textWardrobeWeatherAlert.setText("Weather-appropriate wardrobe items are automatically filtered.");
            }
        });
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void rebuildFilterChips(List<String> categoriesList, java.util.Map<String, Integer> counts) {
        if (binding == null) return;

        // Save currently checked chip text
        String checkedText = "All";
        for (int i = 0; i < binding.chipGroupCategories.getChildCount(); i++) {
            View child = binding.chipGroupCategories.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                String rawText = ((Chip) child).getText().toString();
                if (rawText.contains(" (")) {
                    checkedText = rawText.substring(0, rawText.indexOf(" ("));
                } else {
                    checkedText = rawText;
                }
                break;
            }
        }

        binding.chipGroupCategories.removeAllViews();

        // 1. Add "All" Chip
        int totalCount = 0;
        if (counts != null) {
            for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (!"Favorites ❤️".equals(entry.getKey())) {
                    totalCount += entry.getValue();
                }
            }
        }
        Chip allChip = createFilterChip("All", "All".equals(checkedText), totalCount);
        binding.chipGroupCategories.addView(allChip);

        // 2. Add "Favorites ❤️" Chip
        int favoritesCount = counts != null ? counts.getOrDefault("Favorites ❤️", 0) : 0;
        Chip favChip = createFilterChip("Favorites ❤️", "Favorites ❤️".equals(checkedText), favoritesCount);
        binding.chipGroupCategories.addView(favChip);

        // 3. Add Category Chips
        if (categoriesList != null) {
            for (String category : categoriesList) {
                int count = counts != null ? counts.getOrDefault(category, 0) : 0;
                Chip chip = createFilterChip(category, category.equals(checkedText), count);
                binding.chipGroupCategories.addView(chip);
            }
        }
    }

    /**
     * Create a Material 3 filter chip for category selection.
     */
    private Chip createFilterChip(String text, boolean isChecked, int count) {
        Chip chip = new Chip(requireContext());
        chip.setText(text + " (" + count + ")");
        chip.setCheckable(true);
        chip.setChecked(isChecked);
        chip.setChipBackgroundColorResource(R.color.chip_background_selector);
        chip.setTextColor(getResources().getColorStateList(
                R.color.chip_text_selector, requireContext().getTheme()));

        chip.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                // Uncheck all other chips
                for (int i = 0; i < binding.chipGroupCategories.getChildCount(); i++) {
                    View child = binding.chipGroupCategories.getChildAt(i);
                    if (child instanceof Chip && child != buttonView) {
                        ((Chip) child).setChecked(false);
                    }
                }

                // Apply filters
                if ("All".equals(text)) {
                    viewModel.setFavoritesOnly(false);
                    viewModel.setSelectedCategory(null);
                } else if ("Favorites ❤️".equals(text)) {
                    viewModel.setFavoritesOnly(true);
                    viewModel.setSelectedCategory(null);
                } else {
                    viewModel.setFavoritesOnly(false);
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
     * Launches AddItemActivity in edit mode with the item's ID.
     */
    @Override
    public void onItemClick(WardrobeItem item) {
        Intent intent = new Intent(requireContext(), AddItemActivity.class);
        intent.putExtra(AddItemActivity.EXTRA_ITEM_ID, item.getItemId());
        startActivity(intent);
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
                        + (item.getName() != null && !item.getName().trim().isEmpty() ? item.getName() : item.getCategory())
                        + " from your wardrobe?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteItemWithUndo(item);
                })
                .show();
    }

    private void deleteItemWithUndo(WardrobeItem item) {
        viewModel.deleteItem(item);
        String label = (item.getName() != null && !item.getName().trim().isEmpty())
                ? item.getName() : item.getCategory();
        Snackbar.make(binding.getRoot(), label + " removed", Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> viewModel.insertItem(item))
                .show();
    }

    @Override
    public void onFavoriteClick(WardrobeItem item) {
        viewModel.toggleFavorite(item);
    }

    // =========================================================================
    // WEATHER STATUS INTERACTION (Weather Feature)
    // =========================================================================

    private void setupWeatherStatus() {
        // Tapping the weather card opens the simulator dialog so the user/examiner
        // can easily test how the wardrobe grid automatically filters by temperature
        binding.cardWeatherStatus.setOnClickListener(v -> showWeatherSimulatorDialog());
    }

    private void checkLocationPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) 
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fetchLocalLocationAndWeather();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchLocalLocationAndWeather() {
        try {
            LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                fetchWeatherByCity(DEFAULT_CITY);
                return;
            }

            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsEnabled && !isNetworkEnabled) {
                fetchWeatherByCity(DEFAULT_CITY);
                return;
            }

            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchWeatherByCity(DEFAULT_CITY);
                return;
            }

            Location location = null;
            if (isNetworkEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (location == null && isGpsEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (location != null) {
                fetchWeatherByCoords(location.getLatitude(), location.getLongitude());
            } else {
                String provider = isNetworkEnabled ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
                locationManager.requestSingleUpdate(provider, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location loc) {
                        fetchWeatherByCoords(loc.getLatitude(), loc.getLongitude());
                    }
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}
                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                }, android.os.Looper.getMainLooper());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fetchWeatherByCity(DEFAULT_CITY);
        }
    }

    private void fetchWeatherByCity(String city) {
        String apiKey = getWeatherApiKey();
        if (apiKey.isEmpty()) {
            // Default simulator start since key is empty
            viewModel.setTemperatureAndFilter(32.0, "Sunny", "Kuala Lumpur (Simulated)");
        } else {
            io.reactivex.rxjava3.disposables.Disposable disposable = viewModel.fetchWeatherByCityName(city, apiKey)
                    .subscribe(
                            temp -> {},
                            throwable -> {
                                throwable.printStackTrace();
                                // Fallback to simulated hot weather on failure
                                viewModel.setTemperatureAndFilter(32.0, "Sunny", "Kuala Lumpur (Simulated)");
                            }
                    );
            // ViewModel takes care of CompositeDisposable, but we can track one-off failures
        }
    }

    private void fetchWeatherByCoords(double lat, double lon) {
        String apiKey = getWeatherApiKey();
        if (apiKey.isEmpty()) {
            viewModel.setTemperatureAndFilter(32.0, "Sunny", "Current Location (Simulated)");
        } else {
            io.reactivex.rxjava3.disposables.Disposable disposable = viewModel.fetchWeatherByCoords(lat, lon, apiKey)
                    .subscribe(
                            temp -> {},
                            throwable -> {
                                throwable.printStackTrace();
                                fetchWeatherByCity(DEFAULT_CITY);
                            }
                    );
        }
    }

    private void showWeatherSimulatorDialog() {
        String[] options = {"Cold (< 15°C)", "Mild (15°C - 25°C)", "Hot (> 25°C)", "Real API Weather", "Disable Weather Filter (Show All)"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Weather Simulator")
                .setItems(options, (dialog, which) -> {
                    if (which == 3) {
                        checkLocationPermissionsAndStart();
                    } else if (which == 4) {
                        viewModel.clearWeatherFilter();
                    } else {
                        double temp = (which == 0) ? 12.0 : (which == 1) ? 22.0 : 32.0;
                        String desc = (which == 0) ? "Snowy" : (which == 1) ? "Cloudy" : "Sunny";
                        viewModel.setTemperatureAndFilter(temp, desc, "Current Location (Simulated)");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int getThemeColor(int attrRes) {
        TypedValue typedValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(attrRes, typedValue, true)) {
            return typedValue.data;
        }
        return Color.GRAY;
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
