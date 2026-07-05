package com.aiman.smartwardrobe.data.entity;

import androidx.room.ColumnInfo;

/**
 * ============================================================================
 * WearHistoryEntry — POJO for Wear History Query Results
 * ============================================================================
 *
 * <p>This class is <b>not</b> a Room @Entity — it is a plain POJO used to
 * receive the results of a JOIN query between {@code calendar_event} and
 * {@code wardrobe_item} tables.</p>
 *
 * <p>Each entry represents a single wear event along with the clothing
 * item's metadata (category, fabric, color, image) for display in the
 * Wear History tab on the Analytics screen.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.dao.CalendarEventDao#getWearHistory(int)
 */
public class WearHistoryEntry {

    /** The unique identifier of the wear event. */
    @ColumnInfo(name = "event_id")
    private long eventId;

    /** The unique identifier of the wardrobe item that was worn. */
    @ColumnInfo(name = "item_id")
    private long itemId;

    /** The epoch timestamp (milliseconds) when the item was worn. */
    @ColumnInfo(name = "date_worn")
    private long dateWorn;

    /** The clothing category (e.g., "T-Shirt", "Jeans"). */
    @ColumnInfo(name = "category")
    private String category;

    /** The fabric type (e.g., "Cotton", "Denim"). */
    @ColumnInfo(name = "fabric_type")
    private String fabricType;

    /** The hex color string of the item. */
    @ColumnInfo(name = "color_hex")
    private String colorHex;

    /** Local file path pointing to the item's image. */
    @ColumnInfo(name = "image_path")
    private String imagePath;

    // =========================================================================
    // GETTERS AND SETTERS
    // =========================================================================

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public long getDateWorn() {
        return dateWorn;
    }

    public void setDateWorn(long dateWorn) {
        this.dateWorn = dateWorn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFabricType() {
        return fabricType;
    }

    public void setFabricType(String fabricType) {
        this.fabricType = fabricType;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
