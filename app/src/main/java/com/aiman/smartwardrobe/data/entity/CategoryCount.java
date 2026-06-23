package com.aiman.smartwardrobe.data.entity;

import androidx.room.ColumnInfo;

/**
 * ============================================================================
 * CategoryCount — POJO for Category Distribution Query Results
 * ============================================================================
 *
 * <p>This class is <b>not</b> a Room @Entity — it does not map to its own
 * database table. Instead, it is a plain POJO used to receive the results
 * of a GROUP BY aggregation query on the {@code wardrobe_item} table.</p>
 *
 * <p><b>Query:</b>
 * <pre>
 *   SELECT category, COUNT(*) AS count
 *   FROM wardrobe_item
 *   GROUP BY category
 *   ORDER BY count DESC
 * </pre></p>
 *
 * <p><b>Used by:</b> Analytics Dashboard (Module 4) to render the
 * category distribution bar chart.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.dao.WardrobeDao#getCategoryDistribution()
 */
public class CategoryCount {

    /** The clothing category name (e.g., "T-Shirt", "Jeans"). */
    @ColumnInfo(name = "category")
    private String category;

    /** The number of items in this category. */
    @ColumnInfo(name = "count")
    private int count;

    // =========================================================================
    // GETTERS AND SETTERS
    // =========================================================================

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
