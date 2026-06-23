package com.aiman.smartwardrobe.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aiman.smartwardrobe.data.entity.StylingOntology;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * ============================================================================
 * StylingOntologyDao — Data Access Object for the STYLING_ONTOLOGY Table
 * ============================================================================
 *
 * <p>
 * Provides database access for the styling rules that power the
 * Smart Stylist outfit recommendation engine (Module 3).
 * </p>
 *
 * <p>
 * <b>Key Queries:</b>
 * <ul>
 * <li>{@link #getRulesForTemperature(double)} — The primary query used
 * by the Smart Stylist to find rules valid for the current weather.</li>
 * <li>{@link #getRulesByDressCode(String)} — Used when the user has
 * specified a dress code preference.</li>
 * </ul>
 * </p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.entity.StylingOntology
 */
@Dao
public interface StylingOntologyDao {

    // =========================================================================
    // INSERT OPERATIONS
    // =========================================================================

    /**
     * Insert a single styling rule into the database.
     *
     * @param rule The StylingOntology rule to insert
     * @return A Completable that signals when the insert is complete
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertRule(StylingOntology rule);

    /**
     * Insert multiple styling rules in a single transaction.
     *
     * <p>
     * Used for pre-seeding the database with default styling rules
     * when the app is first installed. See
     * {@link com.aiman.smartwardrobe.data.SmartWardrobeDatabase} for
     * the pre-seeded rule definitions.
     * </p>
     *
     * @param rules The list of StylingOntology rules to insert
     * @return A Completable that signals when all inserts are complete
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertAllRules(List<StylingOntology> rules);

    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================

    /**
     * Get all styling rules for a specific dress code.
     *
     * <p>
     * Used when the user has selected a dress code preference
     * (e.g., "Formal") and wants outfit suggestions filtered by that
     * specific context.
     * </p>
     *
     * @param dressCode The dress code to filter by (e.g., "Casual", "Formal")
     * @return Single emitting a list of matching rules
     */
    @Query("SELECT * FROM styling_ontology WHERE dress_code = :dressCode")
    Single<List<StylingOntology>> getRulesByDressCode(String dressCode);

    /**
     * Get all styling rules valid for the current temperature.
     *
     * <p>
     * <b>This is the primary query used by the Smart Stylist algorithm.</b>
     * It returns all rules whose {@code max_temperature} is greater than
     * or equal to the current temperature. The algorithm then uses the
     * {@code allowed_categories} from these rules to filter wardrobe items.
     * </p>
     *
     * <p>
     * Example: If currentTemp = 32°C, this query returns rules like
     * "Casual Hot" (max_temp=45) but NOT "Winter" (max_temp=10).
     * </p>
     *
     * @param currentTemp The current temperature in Celsius
     * @return Single emitting a list of rules valid for this temperature
     */
    @Query("SELECT * FROM styling_ontology WHERE max_temperature >= :currentTemp")
    Single<List<StylingOntology>> getRulesForTemperature(double currentTemp);

    /**
     * Get all styling rules in the database.
     *
     * <p>
     * Used for the rules management screen where users can view
     * and potentially customize the styling rules.
     * </p>
     *
     * @return Flowable emitting all styling rules
     */
    @Query("SELECT * FROM styling_ontology")
    Flowable<List<StylingOntology>> getAllRules();
}
