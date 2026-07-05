package com.aiman.smartwardrobe.ui.wardrobe;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.aiman.smartwardrobe.data.entity.StylingOntology;
import com.aiman.smartwardrobe.data.entity.WardrobeItem;
import com.aiman.smartwardrobe.data.repository.WardrobeRepository;
import com.aiman.smartwardrobe.data.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * ============================================================================
 * WardrobeViewModel — MVVM ViewModel for the Wardrobe Grid Screen
 * ============================================================================
 *
 * <p>This ViewModel manages the UI state for the wardrobe inventory screen
 * (Module 1). It sits between the UI (WardrobeFragment) and the data layer
 * (WardrobeRepository), following the <b>MVVM (Model-View-ViewModel)</b>
 * architecture pattern.</p>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ol>
 *   <li><b>Data Exposure:</b> Exposes wardrobe items and categories as
 *       {@link LiveData} objects that the Fragment observes.</li>
 *   <li><b>RxJava Bridge:</b> Converts RxJava3 {@code Flowable} streams
 *       from the Repository into {@code LiveData} for the UI. This is
 *       necessary because Android's UI components (RecyclerView, etc.)
 *       work best with lifecycle-aware LiveData.</li>
 *   <li><b>Lifecycle Safety:</b> The ViewModel survives configuration
 *       changes (e.g., screen rotation), preventing data loss and
 *       unnecessary database re-queries.</li>
 *   <li><b>Resource Cleanup:</b> Uses {@link CompositeDisposable} to
 *       manage RxJava subscriptions and prevent memory leaks.</li>
 * </ol>
 *
 * <p><b>Data Flow:</b>
 * <pre>
 *   Room DB (SQLite)
 *       ↓ Flowable (auto-updates)
 *   WardrobeRepository
 *       ↓ Flowable (on Schedulers.io)
 *   WardrobeViewModel (THIS CLASS)
 *       ↓ LiveData (lifecycle-aware)
 *   WardrobeFragment (observes LiveData)
 *       ↓ updates
 *   RecyclerView (displays items)
 * </pre></p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.repository.WardrobeRepository
 * @see com.aiman.smartwardrobe.ui.wardrobe.WardrobeFragment
 */
public class WardrobeViewModel extends ViewModel {

    // =========================================================================
    // DATA SOURCES
    // =========================================================================

    /** The repository that provides data from Room database */
    private final WardrobeRepository repository;

    // =========================================================================
    // LIVEDATA — Observable UI State
    // =========================================================================

    /**
     * The list of wardrobe items to display in the grid.
     * This is a MutableLiveData internally (so we can post updates from
     * RxJava callbacks) but exposed as LiveData (read-only) to the UI.
     */
    private final MutableLiveData<List<WardrobeItem>> wardrobeItems;

    /**
     * The list of distinct clothing categories available.
     * Used to populate the filter chip group in the UI.
     */
    private final MutableLiveData<List<String>> categories;

    /**
     * The currently selected category filter.
     * null means "All" (no filter applied).
     */
    private final MutableLiveData<String> selectedCategory;

    /**
     * The current search query text.
     * Empty string means no search filter is active.
     */
    private String currentSearchQuery = "";

    // Weather & Recommendation State (Weather Feature)
    private final MutableLiveData<Double> currentTemperature;
    private final MutableLiveData<String> weatherDescription;
    private final MutableLiveData<String> locationName;
    private final MutableLiveData<Boolean> isWeatherLoading;
    private final MutableLiveData<List<String>> allowedCategoriesByWeather;

    // Sorting and Favorites State
    public enum SortOrder {
        NEWEST,
        ALPHABETICAL,
        PRICE_HIGH,
        PRICE_LOW
    }
    private SortOrder currentSortOrder = SortOrder.NEWEST;
    private boolean isFavoritesOnly = false;
    private final MutableLiveData<java.util.Map<String, Integer>> categoryCounts;

    // =========================================================================
    // RXJAVA SUBSCRIPTION MANAGEMENT
    // =========================================================================

    /**
     * CompositeDisposable collects all active RxJava subscriptions.
     *
     * <p>When the ViewModel is cleared (e.g., Fragment is destroyed),
     * all subscriptions are disposed at once via {@link #onCleared()}.
     * This prevents:
     * <ul>
     *   <li>Memory leaks from lingering database observers</li>
     *   <li>Crashes from posting to destroyed UI components</li>
     * </ul></p>
     */
    private final CompositeDisposable compositeDisposable;

    /** Reference to the current items subscription (for category switching) */
    private Disposable currentItemsDisposable;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Initialize the ViewModel with a WardrobeRepository.
     *
     * @param repository The repository providing wardrobe data
     */
    public WardrobeViewModel(WardrobeRepository repository) {
        this.repository = repository;
        this.wardrobeItems = new MutableLiveData<>(new ArrayList<>());
        this.categories = new MutableLiveData<>(new ArrayList<>());
        this.selectedCategory = new MutableLiveData<>(null);
        this.currentTemperature = new MutableLiveData<>(null);
        this.weatherDescription = new MutableLiveData<>("");
        this.locationName = new MutableLiveData<>("");
        this.isWeatherLoading = new MutableLiveData<>(false);
        this.allowedCategoriesByWeather = new MutableLiveData<>(null);
        this.categoryCounts = new MutableLiveData<>(new java.util.HashMap<>());
        this.compositeDisposable = new CompositeDisposable();

        // Start observing data from the database
        loadAllItems();
        loadCategories();
        startObservingAllItemsForCounts();
    }

    // =========================================================================
    // PUBLIC API — LiveData Getters (Read-Only)
    // =========================================================================

    /**
     * Get the observable list of wardrobe items.
     * The Fragment observes this and updates the RecyclerView.
     *
     * @return LiveData containing the current list of wardrobe items
     */
    public LiveData<List<WardrobeItem>> getWardrobeItems() {
        return wardrobeItems;
    }

    /**
     * Get the observable list of distinct categories.
     * The Fragment observes this to populate filter chips.
     *
     * @return LiveData containing the list of category strings
     */
    public LiveData<List<String>> getCategories() {
        return categories;
    }

    /**
     * Get the currently selected category filter.
     *
     * @return LiveData containing the selected category (null = "All")
     */
    public LiveData<String> getSelectedCategory() {
        return selectedCategory;
    }

    // Weather getters
    public LiveData<Double> getCurrentTemperature() {
        return currentTemperature;
    }

    public LiveData<String> getWeatherDescription() {
        return weatherDescription;
    }

    public LiveData<String> getLocationName() {
        return locationName;
    }

    public LiveData<Boolean> getIsWeatherLoading() {
        return isWeatherLoading;
    }

    public LiveData<List<String>> getAllowedCategoriesByWeather() {
        return allowedCategoriesByWeather;
    }

    public LiveData<java.util.Map<String, Integer>> getCategoryCounts() {
        return categoryCounts;
    }

    public boolean isFavoritesOnly() {
        return isFavoritesOnly;
    }

    // =========================================================================
    // PUBLIC API — Actions
    // =========================================================================

    /**
     * Set the category filter. Triggers a new database query for items
     * matching the selected category.
     *
     * @param category The category to filter by, or null for "All"
     */
    public void setSelectedCategory(String category) {
        selectedCategory.setValue(category);
        // Clear search when changing category
        currentSearchQuery = "";
        reloadItems();
    }

    /**
     * Set the search query. Triggers a search query against the database.
     * When a search query is active, it takes priority over category filtering.
     *
     * @param query The search text, or empty string to clear the search
     */
    public void setSearchQuery(String query) {
        currentSearchQuery = query != null ? query.trim() : "";
        reloadItems();
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.currentSortOrder = sortOrder;
        reloadItems();
    }

    public void setFavoritesOnly(boolean favoritesOnly) {
        this.isFavoritesOnly = favoritesOnly;
        reloadItems();
    }

    public void toggleFavorite(WardrobeItem item) {
        item.setFavorite(!item.isFavorite());
        Disposable disposable = repository.updateItem(item)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> { /* Database update triggers Flowable refresh */ },
                        Throwable::printStackTrace
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Reload items based on the current search query and category filter.
     * Search takes priority — if a search query is active, category filter
     * is ignored.
     */
    private void reloadItems() {
        // Dispose the previous items subscription before creating a new one
        if (currentItemsDisposable != null && !currentItemsDisposable.isDisposed()) {
            currentItemsDisposable.dispose();
        }

        if (!currentSearchQuery.isEmpty()) {
            // Search mode — search across all categories
            loadSearchResults(currentSearchQuery);
        } else if (selectedCategory.getValue() == null) {
            // No filter — load all items
            loadAllItems();
        } else {
            // Filter by category
            loadItemsByCategory(selectedCategory.getValue());
        }
    }

    /**
     * Delete a wardrobe item from the database.
     * The UI will auto-update via the Flowable subscription.
     *
     * @param item The WardrobeItem to delete
     */
    public void deleteItem(WardrobeItem item) {
        Disposable disposable = repository.deleteItem(item)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> { /* Success — Flowable auto-refreshes the list */ },
                        throwable -> {
                            // Error handling — in production, show a Snackbar
                            throwable.printStackTrace();
                        }
                );
        compositeDisposable.add(disposable);
    }

    public void insertItem(WardrobeItem item) {
        Disposable disposable = repository.insertItem(item)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> { /* Success — Flowable auto-refreshes the list */ },
                        throwable -> {
                            throwable.printStackTrace();
                        }
                );
        compositeDisposable.add(disposable);
    }

    // =========================================================================
    // PRIVATE — Data Loading Methods
    // =========================================================================

    /**
     * Subscribe to ALL wardrobe items from the database.
     * Room's Flowable automatically re-emits when data changes.
     */
    private void loadAllItems() {
        currentItemsDisposable = repository.getAllItems()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        items -> wardrobeItems.setValue(filterAndSortItems(items)),
                        throwable -> {
                            throwable.printStackTrace();
                            wardrobeItems.setValue(new ArrayList<>());
                        }
                );
        compositeDisposable.add(currentItemsDisposable);
    }

    /**
     * Subscribe to wardrobe items filtered by category.
     *
     * @param category The category to filter by
     */
    private void loadItemsByCategory(String category) {
        currentItemsDisposable = repository.getItemsByCategory(category)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        items -> wardrobeItems.setValue(filterAndSortItems(items)),
                        throwable -> {
                            throwable.printStackTrace();
                            wardrobeItems.setValue(new ArrayList<>());
                        }
                );
        compositeDisposable.add(currentItemsDisposable);
    }

    /**
     * Subscribe to the list of distinct categories.
     * Used to populate category filter chips dynamically.
     */
    private void loadCategories() {
        Disposable disposable = repository.getAllCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        categoryList -> categories.setValue(categoryList),
                        throwable -> {
                            throwable.printStackTrace();
                            categories.setValue(new ArrayList<>());
                        }
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Subscribe to search results matching the given query.
     *
     * @param query The search term to match against category and fabric type
     */
    private void loadSearchResults(String query) {
        currentItemsDisposable = repository.searchItems(query)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        items -> wardrobeItems.setValue(filterAndSortItems(items)),
                        throwable -> {
                            throwable.printStackTrace();
                            wardrobeItems.setValue(new ArrayList<>());
                        }
                );
        compositeDisposable.add(currentItemsDisposable);
    }

    // =========================================================================
    // WEATHER OPERATIONS (Weather Feature)
    // =========================================================================

    /**
     * Helper to filter wardrobe items in-memory based on weather allowed categories,
     * favorites filter, and then sort them accordingly.
     */
    private List<WardrobeItem> filterAndSortItems(List<WardrobeItem> originalList) {
        if (originalList == null) return new ArrayList<>();

        List<String> allowedCats = allowedCategoriesByWeather.getValue();
        List<WardrobeItem> list = new ArrayList<>();

        // 1. Filter by Weather and Favorites
        for (WardrobeItem item : originalList) {
            // Check favorites filter
            if (isFavoritesOnly && !item.isFavorite()) {
                continue;
            }
            // Check weather filter
            if (allowedCats != null && !allowedCats.isEmpty()) {
                boolean match = false;
                for (String allowedCat : allowedCats) {
                    if (item.getCategory().equalsIgnoreCase(allowedCat)) {
                        match = true;
                        break;
                    }
                }
                if (!match) continue;
            }
            list.add(item);
        }

        // 2. Sort the items
        switch (currentSortOrder) {
            case ALPHABETICAL:
                list.sort((o1, o2) -> {
                    String c1 = o1.getCategory() != null ? o1.getCategory() : "";
                    String c2 = o2.getCategory() != null ? o2.getCategory() : "";
                    return c1.compareToIgnoreCase(c2);
                });
                break;
            case PRICE_HIGH:
                list.sort((o1, o2) -> Double.compare(o2.getPurchasePrice(), o1.getPurchasePrice()));
                break;
            case PRICE_LOW:
                list.sort((o1, o2) -> Double.compare(o1.getPurchasePrice(), o2.getPurchasePrice()));
                break;
            case NEWEST:
            default:
                list.sort((o1, o2) -> Long.compare(o2.getDateAdded(), o1.getDateAdded()));
                break;
        }
        return list;
    }

    /**
     * Start observing all items in database to build and cache category count stats dynamically.
     */
    private void startObservingAllItemsForCounts() {
        Disposable disposable = repository.getAllItems()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        items -> {
                            java.util.Map<String, Integer> counts = new java.util.HashMap<>();
                            int favCount = 0;
                            for (WardrobeItem item : items) {
                                String cat = item.getCategory();
                                counts.put(cat, counts.getOrDefault(cat, 0) + 1);
                                if (item.isFavorite()) {
                                    favCount++;
                                }
                            }
                            counts.put("Favorites ❤️", favCount);
                            categoryCounts.setValue(counts);
                        },
                        Throwable::printStackTrace
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Fetch current temperature for a city from OpenWeatherMap API using Retrofit.
     */
    public io.reactivex.rxjava3.core.Single<Double> fetchWeatherByCityName(String city, String apiKey) {
        isWeatherLoading.postValue(true);
        return RetrofitClient.getInstance().getWeatherApiService()
                .getCurrentWeather(city, "metric", apiKey)
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(response -> {
                    isWeatherLoading.postValue(false);
                    String desc = (response.getWeather() != null && !response.getWeather().isEmpty()) 
                            ? response.getWeather().get(0).getDescription() : "Clear";
                    setTemperatureAndFilter(response.getMain().getTemp(), desc, response.getCityName());
                })
                .doOnError(throwable -> isWeatherLoading.postValue(false))
                .map(response -> response.getMain().getTemp());
    }

    /**
     * Fetch current temperature for coordinates from OpenWeatherMap API using Retrofit.
     */
    public io.reactivex.rxjava3.core.Single<Double> fetchWeatherByCoords(double lat, double lon, String apiKey) {
        isWeatherLoading.postValue(true);
        return RetrofitClient.getInstance().getWeatherApiService()
                .getCurrentWeatherByCoords(lat, lon, "metric", apiKey)
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(response -> {
                    isWeatherLoading.postValue(false);
                    String desc = (response.getWeather() != null && !response.getWeather().isEmpty()) 
                            ? response.getWeather().get(0).getDescription() : "Clear";
                    setTemperatureAndFilter(response.getMain().getTemp(), desc, response.getCityName());
                })
                .doOnError(throwable -> isWeatherLoading.postValue(false))
                .map(response -> response.getMain().getTemp());
    }

    /**
     * Set temperature, update allowed categories from database rules, and refresh grid.
     */
    public void setTemperatureAndFilter(double temp, String description, String location) {
        currentTemperature.postValue(temp);
        weatherDescription.postValue(description);
        locationName.postValue(location);

        Disposable disposable = repository.getRulesForTemperature(temp)
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        rules -> {
                            List<String> allowedCats = new ArrayList<>();
                            for (StylingOntology rule : rules) {
                                if (rule.getAllowedCategories() != null) {
                                    String[] parts = rule.getAllowedCategories().split(",");
                                    for (String part : parts) {
                                        String trimmed = part.trim();
                                        if (!trimmed.isEmpty() && !allowedCats.contains(trimmed)) {
                                            allowedCats.add(trimmed);
                                        }
                                    }
                                }
                            }
                            allowedCategoriesByWeather.setValue(allowedCats);
                            reloadItems();
                        },
                        throwable -> {
                            throwable.printStackTrace();
                            allowedCategoriesByWeather.setValue(null);
                            reloadItems();
                        }
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Reset/clear the weather filter to show all items again.
     */
    public void clearWeatherFilter() {
        currentTemperature.postValue(null);
        weatherDescription.postValue("");
        locationName.postValue("");
        allowedCategoriesByWeather.setValue(null);
        reloadItems();
    }

    // =========================================================================
    // LIFECYCLE — Resource Cleanup
    // =========================================================================

    /**
     * Called when the ViewModel is no longer used and will be destroyed.
     *
     * <p>Disposes ALL active RxJava subscriptions to prevent:
     * <ul>
     *   <li>Memory leaks from database observers</li>
     *   <li>Callbacks to destroyed UI components</li>
     *   <li>Unnecessary background database queries</li>
     * </ul></p>
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

    // =========================================================================
    // VIEWMODEL FACTORY
    // =========================================================================

    /**
     * Factory class for creating WardrobeViewModel instances.
     *
     * <p><b>Why is a Factory needed?</b>
     * ViewModels with constructor parameters (like our Repository)
     * cannot be created by the default {@link ViewModelProvider.Factory}.
     * This custom factory tells the ViewModelProvider how to instantiate
     * our ViewModel with the required Repository dependency.</p>
     *
     * <p><b>Usage in Fragment:</b>
     * <pre>
     *   WardrobeViewModel.Factory factory =
     *       new WardrobeViewModel.Factory(getApplication());
     *   viewModel = new ViewModelProvider(this, factory)
     *       .get(WardrobeViewModel.class);
     * </pre></p>
     */
    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;

        public Factory(Application application) {
            this.application = application;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(WardrobeViewModel.class)) {
                WardrobeRepository repository = new WardrobeRepository(application);
                return (T) new WardrobeViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: "
                    + modelClass.getName());
        }
    }
}
