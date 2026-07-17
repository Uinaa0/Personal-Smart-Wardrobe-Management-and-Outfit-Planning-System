package com.aiman.smartwardrobe.ui.analytics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.aiman.smartwardrobe.data.entity.CategoryCount;
import com.aiman.smartwardrobe.data.entity.ItemWearStats;
import com.aiman.smartwardrobe.data.entity.WearHistoryEntry;
import com.aiman.smartwardrobe.data.repository.WardrobeRepository;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * ============================================================================
 * AnalyticsViewModel — MVVM ViewModel for the Analytics Dashboard
 * ============================================================================
 *
 * <p>This ViewModel manages the UI state for the Analytics Dashboard
 * (Module 4). It fetches wardrobe statistics from the Repository and
 * exposes them as {@link LiveData} for the Fragment to observe.</p>
 *
 * <p><b>Key Metrics:</b></p>
 * <ul>
 *   <li><b>Total Items:</b> Count of all wardrobe items</li>
 *   <li><b>Total Value:</b> Sum of all purchase prices</li>
 *   <li><b>Average CPW:</b> Mean Cost-Per-Wear across all items</li>
 *   <li><b>Category Distribution:</b> Item count per category</li>
 *   <li><b>Most/Least Worn:</b> Items ranked by wear frequency</li>
 * </ul>
 *
 * <p><b>Data Flow:</b>
 * <pre>
 *   Room DB (SQLite)
 *       ↓ Single (one-shot queries)
 *   WardrobeRepository
 *       ↓ Single (on Schedulers.io)
 *   AnalyticsViewModel (THIS CLASS)
 *       ↓ LiveData (lifecycle-aware)
 *   AnalyticsFragment (observes LiveData)
 *       ↓ updates
 *   Summary Cards + Category Bars + Item Lists
 * </pre></p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.repository.WardrobeRepository
 * @see com.aiman.smartwardrobe.ui.analytics.AnalyticsFragment
 */
public class AnalyticsViewModel extends ViewModel {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** Maximum number of items to show in the most/least worn lists. */
    private static final int TOP_ITEMS_LIMIT = 5;

    /** Maximum number of entries to show in the wear history list. */
    private static final int WEAR_HISTORY_LIMIT = 50;

    // =========================================================================
    // DATA SOURCES
    // =========================================================================

    /** The repository that provides data from Room database */
    private final WardrobeRepository repository;

    // =========================================================================
    // LIVEDATA — Observable UI State
    // =========================================================================

    private final MutableLiveData<Integer> totalItems;
    private final MutableLiveData<Double> totalValue;
    private final MutableLiveData<Double> averageCpw;
    private final MutableLiveData<List<CategoryCount>> categoryDistribution;
    private final MutableLiveData<List<ItemWearStats>> mostWornItems;
    private final MutableLiveData<List<ItemWearStats>> leastWornItems;
    private final MutableLiveData<List<WearHistoryEntry>> wearHistory;

    // =========================================================================
    // RXJAVA SUBSCRIPTION MANAGEMENT
    // =========================================================================

    private final CompositeDisposable compositeDisposable;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Initialize the ViewModel with a WardrobeRepository.
     *
     * @param repository The repository providing wardrobe data
     */
    public AnalyticsViewModel(WardrobeRepository repository) {
        this.repository = repository;
        this.totalItems = new MutableLiveData<>(0);
        this.totalValue = new MutableLiveData<>(0.0);
        this.averageCpw = new MutableLiveData<>(0.0);
        this.categoryDistribution = new MutableLiveData<>(new ArrayList<>());
        this.mostWornItems = new MutableLiveData<>(new ArrayList<>());
        this.leastWornItems = new MutableLiveData<>(new ArrayList<>());
        this.wearHistory = new MutableLiveData<>(new ArrayList<>());
        this.compositeDisposable = new CompositeDisposable();

        // Load all analytics data
        loadAnalyticsData();
    }

    // =========================================================================
    // PUBLIC API — LiveData Getters (Read-Only)
    // =========================================================================

    public LiveData<Integer> getTotalItems() {
        return totalItems;
    }

    public LiveData<Double> getTotalValue() {
        return totalValue;
    }

    public LiveData<Double> getAverageCpw() {
        return averageCpw;
    }

    public LiveData<List<CategoryCount>> getCategoryDistribution() {
        return categoryDistribution;
    }

    public LiveData<List<ItemWearStats>> getMostWornItems() {
        return mostWornItems;
    }

    public LiveData<List<ItemWearStats>> getLeastWornItems() {
        return leastWornItems;
    }

    public LiveData<List<WearHistoryEntry>> getWearHistory() {
        return wearHistory;
    }

    // =========================================================================
    // PUBLIC API — Actions
    // =========================================================================

    /**
     * Refresh all analytics data from the database.
     * Called when the fragment resumes or the user pulls to refresh.
     */
    public void refreshData() {
        loadAnalyticsData();
    }

    // =========================================================================
    // PRIVATE — Data Loading Methods
    // =========================================================================

    /**
     * Load all analytics metrics from the database.
     * Each metric is loaded independently via separate RxJava subscriptions.
     */
    private void loadAnalyticsData() {
        loadTotalItems();
        loadTotalValue();
        loadCategoryDistribution();
        loadMostWornItems();
        loadLeastWornItems();
        loadAverageCpw();
        loadWearHistory();
    }

    /**
     * Load the total number of wardrobe items.
     */
    private void loadTotalItems() {
        Disposable disposable = repository.getItemCount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        count -> totalItems.setValue(count),
                        throwable -> {
                            throwable.printStackTrace();
                            totalItems.setValue(0);
                        }
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Load the total monetary value of all wardrobe items.
     */
    private void loadTotalValue() {
        Disposable disposable = repository.getTotalValue()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        value -> totalValue.setValue(value),
                        throwable -> {
                            throwable.printStackTrace();
                            totalValue.setValue(0.0);
                        }
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Load category distribution data and compute average CPW.
     */
    private void loadCategoryDistribution() {
        Disposable disposable = repository.getCategoryDistribution()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        distribution -> categoryDistribution.setValue(distribution),
                        throwable -> {
                            throwable.printStackTrace();
                            categoryDistribution.setValue(new ArrayList<>());
                        }
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Load the most-worn items and use them to compute average CPW.
     */
    private void loadMostWornItems() {
        Disposable disposable = repository.getMostWornItems(TOP_ITEMS_LIMIT)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        items -> mostWornItems.setValue(items),
                        throwable -> {
                            throwable.printStackTrace();
                            mostWornItems.setValue(new ArrayList<>());
                        }
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Load the least-worn items.
     */
    private void loadLeastWornItems() {
        Disposable disposable = repository.getLeastWornItems(TOP_ITEMS_LIMIT)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        items -> leastWornItems.setValue(items),
                        throwable -> {
                            throwable.printStackTrace();
                            leastWornItems.setValue(new ArrayList<>());
                        }
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Compute the average Cost-Per-Wear from a list of item stats.
     *
     * @param items The list of items with wear statistics
     */
    private void computeAverageCpw(List<ItemWearStats> items) {
        if (items == null || items.isEmpty()) {
            averageCpw.setValue(0.0);
            return;
        }

        double totalCpw = 0;
        int countWithWears = 0;
        for (ItemWearStats item : items) {
            if (item.getWearCount() > 0) {
                totalCpw += item.getCostPerWear();
                countWithWears++;
            }
        }

        if (countWithWears > 0) {
            averageCpw.setValue(totalCpw / countWithWears);
        } else {
            averageCpw.setValue(0.0);
        }
    }

    /**
     * Load average CPW from ALL wardrobe items (not just the top 5).
     *
     * <p>This was previously computed from only the 5 most-worn items,
     * which gave a misleadingly low average. Now it queries every item
     * with a LEFT JOIN to include unworn items (treated as infinite CPW
     * and excluded from the average).</p>
     */
    private void loadAverageCpw() {
        Disposable disposable = repository.getAllItemWearStats()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::computeAverageCpw,
                        throwable -> {
                            throwable.printStackTrace();
                            averageCpw.setValue(0.0);
                        }
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Load the wear history log — a chronological list of wear events
     * with joined item metadata.
     */
    private void loadWearHistory() {
        Disposable disposable = repository.getWearHistory(WEAR_HISTORY_LIMIT)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        entries -> wearHistory.setValue(entries),
                        throwable -> {
                            throwable.printStackTrace();
                            wearHistory.setValue(new ArrayList<>());
                        }
                );
        compositeDisposable.add(disposable);
    }

    // =========================================================================
    // LIFECYCLE — Resource Cleanup
    // =========================================================================

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

    // =========================================================================
    // VIEWMODEL FACTORY
    // =========================================================================

    /**
     * Factory class for creating AnalyticsViewModel instances.
     * Required because the ViewModel has a constructor parameter (Repository).
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
            if (modelClass.isAssignableFrom(AnalyticsViewModel.class)) {
                WardrobeRepository repository = WardrobeRepository.getInstance(application);
                return (T) new AnalyticsViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: "
                    + modelClass.getName());
        }
    }
}
