package com.aiman.smartwardrobe.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aiman.smartwardrobe.data.entity.CalendarEvent;
import com.aiman.smartwardrobe.data.entity.ItemWearStats;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * ============================================================================
 * CalendarEventDao — Data Access Object for the CALENDAR_EVENT Table
 * ============================================================================
 *
 * <p>Handles all database operations related to the wear history log.
 * This DAO is used by both the Smart Stylist (Module 3) for recency
 * filtering and the Analytics Dashboard (Module 4) for Cost-Per-Wear
 * calculations.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.entity.CalendarEvent
 */
@Dao
public interface CalendarEventDao {

    // =========================================================================
    // INSERT OPERATIONS
    // =========================================================================

    /**
     * Log a new wear event — records that an item was worn on a specific date.
     *
     * <p>Called when the user confirms an outfit selection in the
     * Fit Stylist (Module 2) or when the Smart Stylist auto-generates
     * an outfit and the user "wears" it.</p>
     *
     * @param event The CalendarEvent containing itemId and dateWorn
     * @return A Completable that signals when the insert is complete
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertEvent(CalendarEvent event);

    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================

    /**
     * Get all wear events for a specific wardrobe item.
     * Ordered by most recent wear date first.
     *
     * <p>Used in the item detail screen to show the wear history
     * timeline for a particular clothing item.</p>
     *
     * @param itemId The ID of the wardrobe item
     * @return Flowable emitting the list of wear events for the item
     */
    @Query("SELECT * FROM calendar_event WHERE item_id = :itemId ORDER BY date_worn DESC")
    Flowable<List<CalendarEvent>> getEventsForItem(long itemId);

    /**
     * Get the total number of times a specific item has been worn.
     *
     * <p><b>Used in Analytics Dashboard (Module 4):</b>
     * This count is the denominator in the Cost-Per-Wear formula:
     * <pre>
     *   CPW = WardrobeItem.purchasePrice / getWearCount(itemId)
     * </pre></p>
     *
     * @param itemId The ID of the wardrobe item
     * @return Single emitting the total wear count
     */
    @Query("SELECT COUNT(*) FROM calendar_event WHERE item_id = :itemId")
    Single<Integer> getWearCount(long itemId);

    /**
     * Get the IDs of all items that have been worn since a given date.
     *
     * <p><b>Critical for Smart Stylist (Module 3) — Recency Filter:</b>
     * The algorithm calls this with a timestamp from 3 days ago to get
     * a list of recently worn item IDs, which are then EXCLUDED from
     * outfit recommendations to promote variety.</p>
     *
     * <p>Usage in Repository:
     * <pre>
     *   long threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L);
     *   getRecentlyWornItemIds(threeDaysAgo);
     * </pre></p>
     *
     * @param sinceDate The cutoff timestamp (epoch millis) — items worn
     *                  after this date are considered "recently worn"
     * @return Single emitting a list of item IDs worn since the cutoff
     */
    @Query("SELECT DISTINCT item_id FROM calendar_event WHERE date_worn >= :sinceDate")
    Single<List<Long>> getRecentlyWornItemIds(long sinceDate);

    /**
     * Get all wear events across all items, ordered by most recent first.
     *
     * <p>Used for the overall wear history view and analytics
     * calculations that span the entire wardrobe.</p>
     *
     * @return Flowable emitting all calendar events
     */
    @Query("SELECT * FROM calendar_event ORDER BY date_worn DESC")
    Flowable<List<CalendarEvent>> getAllEvents();

    /**
     * Get the total number of wear events across ALL wardrobe items.
     *
     * <p><b>Used in Profile stats card:</b>
     * Displayed as the "Wears" counter to show how many times the user
     * has logged wearing any item from their wardrobe.</p>
     *
     * @return Single emitting the total wear count across all items
     */
    @Query("SELECT COUNT(*) FROM calendar_event")
    Single<Integer> getTotalWearCount();

    // =========================================================================
    // ANALYTICS QUERIES (Module 4)
    // =========================================================================

    /**
     * Get the most-worn items ranked by wear count (descending).
     *
     * <p>Performs an INNER JOIN between {@code wardrobe_item} and
     * {@code calendar_event}, grouping by item to count total wears.
     * Only items with at least one wear event are included.</p>
     *
     * <p><b>Used in Analytics Dashboard (Module 4):</b>
     * Displayed in the "Most Worn" items section.</p>
     *
     * @param limit Maximum number of items to return
     * @return Single emitting the list of item wear statistics
     */
    @Query("SELECT w.item_id, w.category, w.color_hex, w.fabric_type, w.image_path, w.purchase_price, " +
           "COUNT(c.event_id) AS wear_count " +
           "FROM wardrobe_item w " +
           "INNER JOIN calendar_event c ON w.item_id = c.item_id " +
           "GROUP BY w.item_id " +
           "ORDER BY wear_count DESC " +
           "LIMIT :limit")
    Single<List<ItemWearStats>> getMostWornItems(int limit);

    /**
     * Get the least-worn items ranked by wear count (ascending).
     *
     * <p>Performs a LEFT JOIN to include items with zero wear events.
     * Items that have never been worn appear with {@code wear_count = 0}.</p>
     *
     * <p><b>Used in Analytics Dashboard (Module 4):</b>
     * Displayed in the "Least Worn" items section to highlight
     * underutilized clothing with high Cost-Per-Wear.</p>
     *
     * @param limit Maximum number of items to return
     * @return Single emitting the list of item wear statistics
     */
    @Query("SELECT w.item_id, w.category, w.color_hex, w.fabric_type, w.image_path, w.purchase_price, " +
           "COUNT(c.event_id) AS wear_count " +
           "FROM wardrobe_item w " +
           "LEFT JOIN calendar_event c ON w.item_id = c.item_id " +
           "GROUP BY w.item_id " +
           "ORDER BY wear_count ASC " +
           "LIMIT :limit")
    Single<List<ItemWearStats>> getLeastWornItems(int limit);
}
