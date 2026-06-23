package com.aiman.smartwardrobe.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.aiman.smartwardrobe.data.entity.CategoryCount;
import com.aiman.smartwardrobe.data.entity.WardrobeItem;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * ============================================================================
 * WardrobeDao — Data Access Object for the WARDROBE_ITEM Table
 * ============================================================================
 *
 * <p>This interface defines all database operations for wardrobe items.
 * Room's annotation processor generates the concrete implementation of
 * this interface at compile time. The generated code handles:
 * <ul>
 *   <li>SQL query execution</li>
 *   <li>Cursor-to-object mapping</li>
 *   <li>Thread safety</li>
 *   <li>RxJava3 stream creation</li>
 * </ul></p>
 *
 * <p><b>RxJava 3 Return Types Explained:</b></p>
 * <ul>
 *   <li>{@link Flowable} — A <b>reactive stream</b> that emits data
 *       continuously. Room automatically re-executes the query and emits
 *       a new list whenever ANY row in the observed table changes. This
 *       enables <b>real-time UI updates</b> without manual refresh logic.
 *       Flowable also supports backpressure (unlike Observable).</li>
 *
 *   <li>{@link Single} — Emits exactly <b>one value</b> then completes.
 *       Used for one-shot queries where you need a single result
 *       (e.g., getting an item by ID, counting total items).</li>
 *
 *   <li>{@link Completable} — Signals <b>completion or error</b> without
 *       emitting any data. Used for write operations (insert, update,
 *       delete) where you only need to know if the operation succeeded.</li>
 * </ul>
 *
 * <p><b>Architecture Pattern:</b> This DAO sits at the bottom of the
 * Repository Pattern stack:
 * <pre>
 *   UI (Fragment) → ViewModel → Repository → DAO → Room → SQLite
 * </pre></p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.entity.WardrobeItem
 * @see com.aiman.smartwardrobe.data.repository.WardrobeRepository
 */
@Dao
public interface WardrobeDao {

    // =========================================================================
    // INSERT OPERATIONS
    // =========================================================================

    /**
     * Insert a single wardrobe item into the database.
     *
     * <p>{@code OnConflictStrategy.REPLACE}: If an item with the same
     * primary key already exists, the old record is replaced with the new
     * one. This effectively serves as an "upsert" (update-or-insert).</p>
     *
     * <p><b>Threading:</b> Room enforces that this operation runs on a
     * background thread. The Completable is subscribed on
     * {@code Schedulers.io()} in the Repository layer.</p>
     *
     * @param item The WardrobeItem to insert
     * @return A Completable that signals when the insert is complete
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertItem(WardrobeItem item);

    /**
     * Insert multiple wardrobe items in a single database transaction.
     * Useful for batch imports or restoring from backup.
     *
     * <p>All items are inserted atomically — if any insert fails,
     * the entire transaction is rolled back.</p>
     *
     * @param items The list of WardrobeItems to insert
     * @return A Completable that signals when all inserts are complete
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertAllItems(List<WardrobeItem> items);

    // =========================================================================
    // UPDATE OPERATIONS
    // =========================================================================

    /**
     * Update an existing wardrobe item's metadata.
     * Room matches the record by its primary key ({@code item_id}).
     *
     * <p>Used when the user edits an item's category, color, fabric,
     * or price after initial creation.</p>
     *
     * @param item The WardrobeItem with updated field values
     * @return A Completable that signals when the update is complete
     */
    @Update
    Completable updateItem(WardrobeItem item);

    // =========================================================================
    // DELETE OPERATIONS
    // =========================================================================

    /**
     * Delete a wardrobe item from the database.
     *
     * <p><b>Cascading Delete:</b> Due to the {@code CASCADE} foreign key
     * defined on {@link com.aiman.smartwardrobe.data.entity.CalendarEvent},
     * deleting a wardrobe item will automatically delete ALL associated
     * wear history records (CalendarEvent rows) for that item.</p>
     *
     * @param item The WardrobeItem to delete
     * @return A Completable that signals when the deletion is complete
     */
    @Delete
    Completable deleteItem(WardrobeItem item);

    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================

    /**
     * Retrieve ALL wardrobe items, ordered by most recently added first.
     *
     * <p><b>Reactive Behavior:</b> Returns a {@link Flowable} which means
     * Room will automatically re-emit the complete, updated list whenever
     * ANY row in the wardrobe_item table is inserted, updated, or deleted.
     * The UI (RecyclerView) subscribes to this Flowable and updates in
     * real-time without manual refresh calls.</p>
     *
     * <p><b>SQL:</b> {@code SELECT * FROM wardrobe_item ORDER BY date_added DESC}</p>
     *
     * @return Flowable emitting the complete list of wardrobe items
     */
    @Query("SELECT * FROM wardrobe_item ORDER BY date_added DESC")
    Flowable<List<WardrobeItem>> getAllItems();

    /**
     * Retrieve wardrobe items filtered by a specific clothing category.
     *
     * <p>Used by the category filter chips in the wardrobe grid UI.
     * When the user taps a chip (e.g., "T-Shirt"), only items matching
     * that category are displayed.</p>
     *
     * @param category The category to filter by (e.g., "T-Shirt", "Jeans")
     * @return Flowable emitting items matching the specified category
     */
    @Query("SELECT * FROM wardrobe_item WHERE category = :category ORDER BY date_added DESC")
    Flowable<List<WardrobeItem>> getItemsByCategory(String category);

    /**
     * Retrieve a single wardrobe item by its unique ID.
     *
     * <p>Used when navigating to an item detail screen or when the
     * Smart Stylist needs to look up specific items by ID.</p>
     *
     * @param itemId The unique identifier of the item
     * @return Single emitting the matching WardrobeItem
     */
    @Query("SELECT * FROM wardrobe_item WHERE item_id = :itemId")
    Single<WardrobeItem> getItemById(long itemId);

    /**
     * Get the total count of items in the wardrobe.
     *
     * <p>Used for:
     * <ul>
     *   <li>Analytics dashboard statistics</li>
     *   <li>Empty-state UI decisions (show placeholder when count = 0)</li>
     * </ul></p>
     *
     * @return Single emitting the total number of wardrobe items
     */
    @Query("SELECT COUNT(*) FROM wardrobe_item")
    Single<Integer> getItemCount();

    /**
     * Get all distinct categories that have at least one item.
     *
     * <p>Used to dynamically populate the category filter chips in the
     * wardrobe grid UI. Only categories that actually contain items are
     * shown (e.g., if the user has no "Dress" items, "Dress" won't
     * appear as a filter chip).</p>
     *
     * @return Flowable emitting the list of distinct category strings
     */
    @Query("SELECT DISTINCT category FROM wardrobe_item ORDER BY category ASC")
    Flowable<List<String>> getAllCategories();

    /**
     * Retrieve wardrobe items matching a list of category names.
     * Used by the Fit Stylist to fetch all "Tops", "Bottoms", or "Shoes" in one query.
     *
     * @param categories List of category strings (e.g. ["T-Shirt", "Shirt", "Sweater"])
     * @return Flowable emitting the list of items matching any of the specified categories
     */
    @Query("SELECT * FROM wardrobe_item WHERE category IN (:categories) ORDER BY date_added DESC")
    Flowable<List<WardrobeItem>> getItemsByCategories(List<String> categories);

    /**
     * Retrieve a static snapshot of wardrobe items matching a list of categories.
     * Used by the Smart Stylist generation algorithm for one-shot filtering.
     *
     * @param categories List of category strings to retrieve
     * @return Single emitting the snapshot list of matching items
     */
    @Query("SELECT * FROM wardrobe_item WHERE category IN (:categories) ORDER BY date_added DESC")
    Single<List<WardrobeItem>> getItemsByCategoriesSingle(List<String> categories);

    // =========================================================================
    // ANALYTICS QUERIES (Module 4)
    // =========================================================================

    /**
     * Get the total monetary value of all items in the wardrobe.
     *
     * <p><b>Used in Analytics Dashboard (Module 4):</b>
     * Displayed as the "Total Value" summary card.</p>
     *
     * @return Single emitting the sum of all purchase prices
     */
    @Query("SELECT COALESCE(SUM(purchase_price), 0) FROM wardrobe_item")
    Single<Double> getTotalValue();

    /**
     * Get the distribution of items across categories.
     *
     * <p>Returns a list of {@link CategoryCount} POJOs, each containing
     * a category name and the number of items in that category.
     * Results are ordered by count descending (largest category first).</p>
     *
     * <p><b>Used in Analytics Dashboard (Module 4):</b>
     * Rendered as horizontal distribution bars.</p>
     *
     * @return Single emitting the list of category counts
     */
    @Query("SELECT category, COUNT(*) AS count FROM wardrobe_item GROUP BY category ORDER BY count DESC")
    Single<List<CategoryCount>> getCategoryDistribution();
}
