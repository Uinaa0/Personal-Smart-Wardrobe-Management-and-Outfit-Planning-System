package com.aiman.smartwardrobe.data.repository;

import android.app.Application;

import com.aiman.smartwardrobe.data.SmartWardrobeDatabase;
import com.aiman.smartwardrobe.data.dao.CalendarEventDao;
import com.aiman.smartwardrobe.data.dao.StylingOntologyDao;
import com.aiman.smartwardrobe.data.dao.WardrobeDao;
import com.aiman.smartwardrobe.data.entity.CalendarEvent;
import com.aiman.smartwardrobe.data.entity.CategoryCount;
import com.aiman.smartwardrobe.data.entity.ItemWearStats;
import com.aiman.smartwardrobe.data.entity.StylingOntology;
import com.aiman.smartwardrobe.data.entity.WardrobeItem;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ============================================================================
 * WardrobeRepository — The Single Source of Truth for Wardrobe Data
 * ============================================================================
 *
 * <p>
 * This class implements the <b>Repository Pattern</b>, which is a core
 * component of Android's recommended MVVM architecture. The Repository
 * serves as a <b>clean API layer</b> between the ViewModel and the
 * data sources (Room database, and in future, network APIs).
 * </p>
 *
 * <p>
 * <b>Why Use a Repository?</b>
 * </p>
 * <ol>
 * <li><b>Abstraction:</b> The ViewModel doesn't need to know whether
 * data comes from a local database, network API, or cache. It
 * simply calls repository methods.</li>
 * <li><b>Single Source of Truth:</b> The Repository decides which data
 * source to use and how to merge/sync them.</li>
 * <li><b>Threading Management:</b> All RxJava scheduling (which thread
 * to run on) is handled here, keeping the ViewModel clean.</li>
 * <li><b>Testability:</b> The Repository can be mocked in unit tests,
 * allowing ViewModels to be tested without a real database.</li>
 * </ol>
 *
 * <p>
 * <b>Threading Strategy:</b>
 * All database operations are subscribed on {@code Schedulers.io()}
 * (a thread pool optimized for I/O-bound work like database and network
 * operations). The ViewModel/UI layer observes on
 * {@code AndroidSchedulers.mainThread()} to safely update the UI.
 * </p>
 *
 * <p>
 * <b>Architecture Position:</b>
 * 
 * <pre>
 *   UI (Fragment)
 *       ↓ observes LiveData
 *   ViewModel
 *       ↓ calls repository methods
 *   WardrobeRepository (THIS CLASS)    ← Manages threading & data source
 *       ↓ delegates to DAO
 *   WardrobeDao / CalendarEventDao
 *       ↓
 *   Room → SQLite
 * </pre>
 * </p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.dao.WardrobeDao
 * @see com.aiman.smartwardrobe.ui.wardrobe.WardrobeViewModel
 */
public class WardrobeRepository {

    // =========================================================================
    // DATA SOURCES (DAOs)
    // =========================================================================

    /** DAO for wardrobe item CRUD operations */
    private final WardrobeDao wardrobeDao;

    /** DAO for calendar event (wear history) operations */
    private final CalendarEventDao calendarEventDao;

    /** DAO for styling ontology rules */
    private final StylingOntologyDao stylingOntologyDao;

    // =========================================================================
    // CACHED DATA — Reactive Streams from Room
    // =========================================================================

    /**
     * A continuously-updating stream of ALL wardrobe items.
     * Room's Flowable automatically re-emits the full list whenever
     * the underlying table changes (insert/update/delete).
     *
     * <p>
     * This stream is cached at the Repository level so that multiple
     * observers (e.g., multiple ViewModels or fragments) share the same
     * database query rather than creating redundant queries.
     * </p>
     */
    private final Flowable<List<WardrobeItem>> allItems;

    /**
     * A continuously-updating stream of distinct category names.
     * Used to dynamically populate category filter chips in the UI.
     */
    private final Flowable<List<String>> allCategories;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Initialize the repository with a reference to the application context.
     *
     * <p>
     * The Application context (not Activity context) is used to obtain
     * the database instance. This prevents memory leaks because the
     * Repository may outlive any single Activity.
     * </p>
     *
     * @param application The Application instance, used to get the
     *                    database singleton
     */
    public WardrobeRepository(Application application) {
        // Obtain the database singleton and extract DAOs
        SmartWardrobeDatabase database = SmartWardrobeDatabase.getInstance(application);
        this.wardrobeDao = database.wardrobeDao();
        this.calendarEventDao = database.calendarEventDao();
        this.stylingOntologyDao = database.stylingOntologyDao();

        // Initialize cached Flowable streams
        // These queries are NOT executed here — they're lazy and only
        // run when something subscribes to them.
        this.allItems = wardrobeDao.getAllItems()
                .subscribeOn(Schedulers.io());

        this.allCategories = wardrobeDao.getAllCategories()
                .subscribeOn(Schedulers.io());
    }

    // =========================================================================
    // WARDROBE ITEM — READ OPERATIONS
    // =========================================================================

    /**
     * Get a reactive stream of ALL wardrobe items.
     *
     * <p>
     * The returned Flowable automatically emits a new list whenever
     * the wardrobe_item table changes. The ViewModel converts this to
     * LiveData for the UI to observe.
     * </p>
     *
     * @return Flowable emitting the complete list of wardrobe items,
     *         ordered by date_added DESC (most recent first)
     */
    public Flowable<List<WardrobeItem>> getAllItems() {
        return allItems;
    }

    /**
     * Get a reactive stream of wardrobe items filtered by category.
     *
     * @param category The category to filter by (e.g., "T-Shirt")
     * @return Flowable emitting items matching the specified category
     */
    public Flowable<List<WardrobeItem>> getItemsByCategory(String category) {
        return wardrobeDao.getItemsByCategory(category)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get a single wardrobe item by its ID.
     *
     * @param itemId The unique identifier of the item
     * @return Single emitting the matching WardrobeItem
     */
    public Single<WardrobeItem> getItemById(long itemId) {
        return wardrobeDao.getItemById(itemId)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get a reactive stream of all distinct category names.
     *
     * @return Flowable emitting the list of distinct categories
     */
    public Flowable<List<String>> getAllCategories() {
        return allCategories;
    }

    /**
     * Get the total number of items in the wardrobe.
     *
     * @return Single emitting the item count
     */
    public Single<Integer> getItemCount() {
        return wardrobeDao.getItemCount()
                .subscribeOn(Schedulers.io());
    }

    // =========================================================================
    // WARDROBE ITEM — WRITE OPERATIONS
    // =========================================================================

    /**
     * Insert a new wardrobe item into the database.
     *
     * <p>
     * The insert runs on {@code Schedulers.io()} (background thread).
     * The caller should observe on {@code AndroidSchedulers.mainThread()}
     * if they need to update the UI upon completion.
     * </p>
     *
     * @param item The WardrobeItem to insert
     * @return Completable that signals completion or error
     */
    public Completable insertItem(WardrobeItem item) {
        return wardrobeDao.insertItem(item)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Update an existing wardrobe item's metadata.
     *
     * @param item The WardrobeItem with updated values
     * @return Completable that signals completion or error
     */
    public Completable updateItem(WardrobeItem item) {
        return wardrobeDao.updateItem(item)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Delete a wardrobe item from the database.
     * Associated CalendarEvent records are automatically deleted
     * due to the CASCADE foreign key constraint.
     *
     * @param item The WardrobeItem to delete
     * @return Completable that signals completion or error
     */
    public Completable deleteItem(WardrobeItem item) {
        return wardrobeDao.deleteItem(item)
                .subscribeOn(Schedulers.io());
    }

    // =========================================================================
    // CALENDAR EVENT — WEAR HISTORY OPERATIONS
    // =========================================================================

    /**
     * Log a wear event — records that an item was worn today.
     *
     * @param itemId The ID of the wardrobe item that was worn
     * @return Completable that signals completion or error
     */
    public Completable logWearEvent(long itemId) {
        CalendarEvent event = new CalendarEvent(
                itemId,
                System.currentTimeMillis() // Current timestamp
        );
        return calendarEventDao.insertEvent(event)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get the total number of times a specific item has been worn.
     * Used for the Cost-Per-Wear calculation in Module 4.
     *
     * @param itemId The ID of the wardrobe item
     * @return Single emitting the wear count
     */
    public Single<Integer> getWearCount(long itemId) {
        return calendarEventDao.getWearCount(itemId)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get the IDs of items worn in the last N days.
     * Used by the Smart Stylist (Module 3) to exclude recently worn items.
     *
     * @param days Number of days to look back
     * @return Single emitting a list of recently worn item IDs
     */
    public Single<List<Long>> getRecentlyWornItemIds(int days) {
        // Calculate the epoch timestamp for N days ago
        long sinceDate = System.currentTimeMillis()
                - ((long) days * 24 * 60 * 60 * 1000);
        return calendarEventDao.getRecentlyWornItemIds(sinceDate)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get a reactive stream of wardrobe items filtered by multiple category names.
     *
     * @param categories List of category strings to filter by
     * @return Flowable emitting the list of matching items
     */
    public Flowable<List<WardrobeItem>> getItemsByCategories(List<String> categories) {
        return wardrobeDao.getItemsByCategories(categories)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get all styling rules valid for the current temperature.
     *
     * @param currentTemp The temperature in Celsius
     * @return Single emitting the list of rules
     */
    public Single<List<StylingOntology>> getRulesForTemperature(double currentTemp) {
        return stylingOntologyDao.getRulesForTemperature(currentTemp)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get a static one-shot list of wardrobe items matching any of the specified
     * categories.
     *
     * @param categories List of category strings
     * @return Single emitting the snapshot list of matching items
     */
    public Single<List<WardrobeItem>> getItemsByCategoriesSingle(List<String> categories) {
        return wardrobeDao.getItemsByCategoriesSingle(categories)
                .subscribeOn(Schedulers.io());
    }

    // =========================================================================
    // ANALYTICS OPERATIONS (Module 4)
    // =========================================================================

    /**
     * Get the total monetary value of all wardrobe items.
     *
     * @return Single emitting the sum of all purchase prices
     */
    public Single<Double> getTotalValue() {
        return wardrobeDao.getTotalValue()
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get the distribution of items across clothing categories.
     *
     * @return Single emitting a list of CategoryCount POJOs
     */
    public Single<List<CategoryCount>> getCategoryDistribution() {
        return wardrobeDao.getCategoryDistribution()
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get the most-worn items ranked by wear count.
     *
     * @param limit Maximum number of items to return
     * @return Single emitting the list of item wear statistics
     */
    public Single<List<ItemWearStats>> getMostWornItems(int limit) {
        return calendarEventDao.getMostWornItems(limit)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get the least-worn items ranked by wear count.
     *
     * @param limit Maximum number of items to return
     * @return Single emitting the list of item wear statistics
     */
    public Single<List<ItemWearStats>> getLeastWornItems(int limit) {
        return calendarEventDao.getLeastWornItems(limit)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Get the total number of wear events logged across ALL wardrobe items.
     * Used by the Profile stats card to display the overall "Wears" count.
     *
     * @return Single emitting the total wear event count
     */
    public Single<Integer> getTotalWearCount() {
        return calendarEventDao.getTotalWearCount()
                .subscribeOn(Schedulers.io());
    }
}
