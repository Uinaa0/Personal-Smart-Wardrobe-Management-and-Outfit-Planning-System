package com.aiman.smartwardrobe.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.aiman.smartwardrobe.data.entity.UserProfile;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * ============================================================================
 * UserProfileDao — Data Access Object for the USER_PROFILE Table
 * ============================================================================
 *
 * <p>Handles database operations for user profile management.
 * In the current single-user design, there is only one UserProfile
 * record. The {@code getCurrentUser()} method uses {@code LIMIT 1}
 * to retrieve this single profile.</p>
 *
 * <p>For future multi-user support, additional query methods
 * (e.g., getUserById, getAllUsers) can be added here without
 * changing the existing API.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.entity.UserProfile
 */
@Dao
public interface UserProfileDao {

    // =========================================================================
    // INSERT / UPDATE OPERATIONS
    // =========================================================================

    /**
     * Insert a new user profile into the database.
     * Uses REPLACE strategy so that re-inserting a profile with the
     * same primary key will update the existing record.
     *
     * @param profile The UserProfile to insert
     * @return A Completable that signals when the insert is complete
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Single<Long> insertProfile(UserProfile profile);

    /**
     * Update an existing user profile.
     * Room matches the profile by its primary key (user_id).
     *
     * <p>Used when the user changes their name or updates their
     * preference settings (stored as a JSON string).</p>
     *
     * @param profile The UserProfile with updated field values
     * @return A Completable that signals when the update is complete
     */
    @Update
    Completable updateProfile(UserProfile profile);

    // =========================================================================
    // QUERY OPERATIONS
    // =========================================================================

    /**
     * Retrieve the current (and only) user profile.
     *
     * <p>Uses {@code LIMIT 1} because the current app design supports
     * a single user. This returns a {@link Single} that emits exactly
     * one UserProfile or an error if no profile exists yet.</p>
     *
     * @return Single emitting the current UserProfile
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    Single<UserProfile> getCurrentUser();

    /**
     * Retrieve a user profile by their user ID.
     *
     * @param userId The ID of the user
     * @return Single emitting the UserProfile
     */
    @Query("SELECT * FROM user_profile WHERE user_id = :userId LIMIT 1")
    Single<UserProfile> getUserById(long userId);

    /**
     * Retrieve a user profile by email address.
     * Used during authentication to verify credentials.
     *
     * @param email The email address to search for
     * @return Maybe emitting the UserProfile if found, or empty if not found
     */
    @Query("SELECT * FROM user_profile WHERE email = :email LIMIT 1")
    io.reactivex.rxjava3.core.Maybe<UserProfile> getUserByEmail(String email);
}
