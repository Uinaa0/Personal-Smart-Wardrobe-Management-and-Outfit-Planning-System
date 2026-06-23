package com.aiman.smartwardrobe.ui.wardrobe;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.aiman.smartwardrobe.data.entity.WardrobeItem;
import com.aiman.smartwardrobe.data.repository.WardrobeRepository;

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
        this.compositeDisposable = new CompositeDisposable();

        // Start observing data from the database
        loadAllItems();
        loadCategories();
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

        // Dispose the previous items subscription before creating a new one
        if (currentItemsDisposable != null && !currentItemsDisposable.isDisposed()) {
            currentItemsDisposable.dispose();
        }

        if (category == null) {
            // No filter — load all items
            loadAllItems();
        } else {
            // Filter by category
            loadItemsByCategory(category);
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
                        items -> wardrobeItems.setValue(items),
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
                        items -> wardrobeItems.setValue(items),
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
