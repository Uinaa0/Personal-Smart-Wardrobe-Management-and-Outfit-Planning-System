package com.aiman.smartwardrobe.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================================
 * ClothingCategory — Centralized Clothing Category Constants
 * ============================================================================
 *
 * <p>Provides a single source of truth for all clothing category string
 * constants used throughout the application. Previously these strings were
 * hardcoded across multiple files (StylistViewModel, SmartWardrobeDatabase,
 * WardrobeFragment), where a typo in any location would silently break
 * category filtering.</p>
 *
 * <p><b>Usage:</b> Replace all raw category string literals with references
 * to the constants defined here.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public final class ClothingCategory {

    // =========================================================================
    // INDIVIDUAL CATEGORY CONSTANTS
    // =========================================================================

    public static final String T_SHIRT = "T-Shirt";
    public static final String SHIRT = "Shirt";
    public static final String JACKET = "Jacket";
    public static final String HOODIE = "Hoodie";
    public static final String SWEATER = "Sweater";

    public static final String PANTS = "Pants";
    public static final String JEANS = "Jeans";
    public static final String SHORTS = "Shorts";
    public static final String SKIRT = "Skirt";
    public static final String DRESS = "Dress";

    public static final String SHOES = "Shoes";
    public static final String SNEAKERS = "Sneakers";
    public static final String BOOTS = "Boots";

    public static final String ACCESSORIES = "Accessories";

    // =========================================================================
    // GROUPED CATEGORY LISTS
    // =========================================================================

    /** Categories classified as tops (chest slot). */
    public static final List<String> TOPS = Collections.unmodifiableList(
            Arrays.asList(T_SHIRT, SHIRT, JACKET, HOODIE, SWEATER));

    /** Categories classified as bottoms (legs slot). */
    public static final List<String> BOTTOMS = Collections.unmodifiableList(
            Arrays.asList(PANTS, JEANS, SHORTS, SKIRT, DRESS));

    /** Categories classified as footwear (feet slot). */
    public static final List<String> FOOTWEAR = Collections.unmodifiableList(
            Arrays.asList(SHOES, SNEAKERS, BOOTS));

    /** Categories classified as head accessories. */
    public static final List<String> HEAD = Collections.unmodifiableList(
            Arrays.asList(ACCESSORIES));

    // =========================================================================
    // PRIVATE CONSTRUCTOR
    // =========================================================================

    /** Prevent instantiation — this is a constants-only class. */
    private ClothingCategory() {
        throw new AssertionError("ClothingCategory is a constants class and cannot be instantiated.");
    }
}
