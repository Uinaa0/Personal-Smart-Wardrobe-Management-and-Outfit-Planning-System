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
 * <p>
 * This interface defines all database operations for wardrobe items.
 * Room's annotation processor generates the concrete implementation of
 * this interface at compile time. The generated code handles:
 * <ul>
 * <li>SQL query execution</li>
 * <li>Cursor-to-object mapping</li>
 * <li>Thread safety</li>
 * <li>RxJava3 stream creation</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>RxJava 3 Return Types Explained:</b>
 * </p>
 * <ul>
 * <li>{@link Flowable} — A <b>reactive stream</b> that emits data
 * continuously. Room automatically re-executes the query and emits
 * a new list whenever ANY row in the observed table changes. This
 * enables <b>real-time UI updates</b> without manual refresh logic.
 * Flowable also supports backpressure (unlike Observable).</li>
 *
 * <li>{@link Single} — Emits exactly <b>one value</b> then completes.
 * Used for one-shot queries where you need a single result
 * (e.g., getting an item by ID, counting total items).</li>
 *
 * <li>{@link Completable} — Signals <b>completion or error</b> without
 * emitting any data. Used for write operations (insert, update,
 * delete) where you only need to know if the operation succeeded.</li>
 * </ul>
 *
 * <p>
 * <b>Architecture Pattern:</b> This DAO sits at the bottom of the
 * Repository Pattern stack:
 * 
 * <pre>
 *   UI (Fragment) → ViewModel → Repository → DAO → Room → SQLite
 * </pre>
 * </p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.entity.WardrobeItem
 * @see com.aiman.smartwardrobe.data.repository.WardrobeRepository
 */
@Dao
public interface WardrobeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertItem(WardrobeItem item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertAllItems(List<WardrobeItem> items);

    @Update
    Completable updateItem(WardrobeItem item);

    @Delete
    Completable deleteItem(WardrobeItem item);

    @Query("SELECT * FROM wardrobe_item WHERE user_id = :userId ORDER BY date_added DESC")
    Flowable<List<WardrobeItem>> getAllItems(long userId);

    @Query("SELECT * FROM wardrobe_item WHERE category = :category AND user_id = :userId ORDER BY date_added DESC")
    Flowable<List<WardrobeItem>> getItemsByCategory(String category, long userId);

    @Query("SELECT * FROM wardrobe_item WHERE item_id = :itemId")
    Single<WardrobeItem> getItemById(long itemId);

    @Query("SELECT COUNT(*) FROM wardrobe_item WHERE user_id = :userId")
    Single<Integer> getItemCount(long userId);

    @Query("SELECT DISTINCT category FROM wardrobe_item WHERE user_id = :userId ORDER BY category ASC")
    Flowable<List<String>> getAllCategories(long userId);

    @Query("SELECT * FROM wardrobe_item WHERE category IN (:categories) AND user_id = :userId ORDER BY date_added DESC")
    Flowable<List<WardrobeItem>> getItemsByCategories(List<String> categories, long userId);

    @Query("SELECT * FROM wardrobe_item WHERE category IN (:categories) AND user_id = :userId ORDER BY date_added DESC")
    Single<List<WardrobeItem>> getItemsByCategoriesSingle(List<String> categories, long userId);

    @Query("SELECT COALESCE(SUM(purchase_price), 0) FROM wardrobe_item WHERE user_id = :userId")
    Single<Double> getTotalValue(long userId);

    @Query("SELECT category, COUNT(*) AS count FROM wardrobe_item WHERE user_id = :userId GROUP BY category ORDER BY count DESC")
    Single<List<CategoryCount>> getCategoryDistribution(long userId);

    @Query("SELECT * FROM wardrobe_item WHERE (category LIKE '%' || :query || '%' " +
            "OR fabric_type LIKE '%' || :query || '%') AND user_id = :userId ORDER BY date_added DESC")
    Flowable<List<WardrobeItem>> searchItems(String query, long userId);
}
