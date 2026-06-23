package com.aiman.smartwardrobe.data.entity;

import androidx.room.ColumnInfo;

/**
 * ============================================================================
 * ItemWearStats — POJO for Item Wear Statistics Query Results
 * ============================================================================
 *
 * <p>This class is <b>not</b> a Room @Entity — it is a plain POJO used to
 * receive the results of a JOIN query between {@code wardrobe_item} and
 * {@code calendar_event} tables.</p>
 *
 * <p><b>Key Metric — Cost-Per-Wear (CPW):</b>
 * <pre>
 *   CPW = purchasePrice / wearCount
 * </pre>
 * A lower CPW indicates better value — the item has been worn many times
 * relative to its cost.</p>
 *
 * <p><b>Used by:</b> Analytics Dashboard (Module 4) for the "Most Worn"
 * and "Least Worn" item lists.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.dao.CalendarEventDao#getMostWornItems(int)
 * @see com.aiman.smartwardrobe.data.dao.CalendarEventDao#getLeastWornItems(int)
 */
public class ItemWearStats {

    /** The unique identifier of the wardrobe item. */
    @ColumnInfo(name = "item_id")
    private long itemId;

    /** The clothing category (e.g., "T-Shirt", "Jeans"). */
    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "color_hex")
    private String colorHex;

    @ColumnInfo(name = "fabric_type")
    private String fabricType;

    /** Local file URI pointing to the item's image. */
    @ColumnInfo(name = "image_path")
    private String imagePath;

    /** The purchase price of the item. */
    @ColumnInfo(name = "purchase_price")
    private double purchasePrice;

    /** The total number of times this item has been worn. */
    @ColumnInfo(name = "wear_count")
    private int wearCount;

    // =========================================================================
    // COMPUTED PROPERTY
    // =========================================================================

    /**
     * Calculate the Cost-Per-Wear metric.
     *
     * @return The cost per wear, or the full purchase price if never worn
     */
    public double getCostPerWear() {
        if (wearCount <= 0) {
            return purchasePrice;
        }
        return purchasePrice / wearCount;
    }

    // =========================================================================
    // GETTERS AND SETTERS
    // =========================================================================

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getFabricType() {
        return fabricType;
    }

    public void setFabricType(String fabricType) {
        this.fabricType = fabricType;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public int getWearCount() {
        return wearCount;
    }

    public void setWearCount(int wearCount) {
        this.wearCount = wearCount;
    }
}
