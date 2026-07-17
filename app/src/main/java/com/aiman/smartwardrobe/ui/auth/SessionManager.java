package com.aiman.smartwardrobe.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.aiman.smartwardrobe.data.SmartWardrobeDatabase;
import com.aiman.smartwardrobe.data.entity.UserProfile;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ============================================================================
 * SessionManager — Centralized Authentication & Session State Manager
 * ============================================================================
 *
 * <p>Consolidates all session-related logic (login state, user ID resolution,
 * SharedPreferences access) into a single class, eliminating the duplicated
 * user ID initialization blocks that were previously scattered across
 * LoginActivity, MainActivity, and WardrobeRepository.</p>
 *
 * <p><b>Why this class exists:</b>
 * Before this refactor, the same ~20-line user ID resolution block was
 * copy-pasted in 3 different files. Any bug fix had to be applied in
 * all locations, violating the DRY (Don't Repeat Yourself) principle.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class SessionManager {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** SharedPreferences file name for user account data. */
    public static final String PREFS_AUTH = "smart_wardrobe_auth";

    /** Key for the stored email. */
    public static final String KEY_EMAIL = "user_email";

    /** Key for the stored password (hashed with PBKDF2). */
    public static final String KEY_PASSWORD = "user_password";

    /** Key for the stored display name. */
    public static final String KEY_NAME = "user_name";

    /** Key tracking whether the user is currently logged in. */
    public static final String KEY_LOGGED_IN = "is_logged_in";

    /** Key for the active user's Room database ID. */
    public static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";

    // =========================================================================
    // SINGLETON
    // =========================================================================

    private static volatile SessionManager instance;
    private final Context appContext;

    private SessionManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Get the singleton SessionManager instance.
     *
     * @param context Any context (Application context will be extracted)
     * @return The singleton SessionManager
     */
    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    // =========================================================================
    // SESSION STATE
    // =========================================================================

    /**
     * Check if a user is currently logged in.
     *
     * @return true if a user session is active
     */
    public boolean isLoggedIn() {
        return getPrefs().getBoolean(KEY_LOGGED_IN, false);
    }

    /**
     * Get the logged-in user's Room database ID.
     *
     * @return The user ID, or -1 if not set
     */
    public long getLoggedInUserId() {
        return getPrefs().getLong(KEY_LOGGED_IN_USER_ID, -1);
    }

    /**
     * Get the logged-in user's display name.
     *
     * @return The display name, or "User" if not set
     */
    public String getUserName() {
        return getPrefs().getString(KEY_NAME, "User");
    }

    /**
     * Get the logged-in user's email.
     *
     * @return The email address, or "user@email.com" if not set
     */
    public String getUserEmail() {
        return getPrefs().getString(KEY_EMAIL, "user@email.com");
    }

    // =========================================================================
    // USER ID RESOLUTION
    // =========================================================================

    /**
     * Ensures the active user has a valid Room database user ID.
     *
     * <p>This method consolidates the user ID resolution logic that was
     * previously duplicated in LoginActivity and MainActivity. It handles
     * three scenarios:</p>
     * <ol>
     *   <li><b>ID already set:</b> No action needed.</li>
     *   <li><b>ID exists for email but not in active key:</b> Copy it.</li>
     *   <li><b>No ID exists at all:</b> Create a new UserProfile in Room.</li>
     * </ol>
     *
     * @param callback Optional callback to invoke when the user ID is ready
     * @return A Disposable that the caller MUST add to a CompositeDisposable
     */
    public Disposable ensureUserIdInitialized(Runnable callback) {
        SharedPreferences prefs = getPrefs();
        long userId = prefs.getLong(KEY_LOGGED_IN_USER_ID, -1);

        if (userId != -1) {
            // User ID already set — invoke callback immediately
            if (callback != null) callback.run();
            return Disposable.empty();
        }

        // Attempt to find user ID by email
        String email = prefs.getString(KEY_EMAIL, "user@email.com");
        long storedUserId = prefs.getLong("user_id_" + email, -1);

        if (storedUserId != -1) {
            // Found an existing ID — save it as the active user
            prefs.edit().putLong(KEY_LOGGED_IN_USER_ID, storedUserId).apply();
            if (callback != null) callback.run();
            return Disposable.empty();
        }

        // No ID exists — create a new UserProfile in Room
        String name = prefs.getString(KEY_NAME, "User");
        UserProfile profile = new UserProfile(name, "{}");

        return SmartWardrobeDatabase.getInstance(appContext)
                .userProfileDao()
                .insertProfile(profile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        newId -> {
                            prefs.edit()
                                    .putLong("user_id_" + email, newId)
                                    .putLong(KEY_LOGGED_IN_USER_ID, newId)
                                    .apply();
                            if (callback != null) callback.run();
                        },
                        throwable -> {
                            throwable.printStackTrace();
                            // Still invoke callback so navigation doesn't hang
                            if (callback != null) callback.run();
                        }
                );
    }

    // =========================================================================
    // LOGIN / LOGOUT
    // =========================================================================

    /**
     * Mark the session as logged in with the given user ID.
     *
     * @param email  The user's email
     * @param userId The Room database user ID
     */
    public void setLoggedIn(String email, long userId) {
        getPrefs().edit()
                .putLong("user_id_" + email, userId)
                .putLong(KEY_LOGGED_IN_USER_ID, userId)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    /**
     * Clear the login state (but preserve account credentials).
     */
    public void logout() {
        getPrefs().edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_LOGGED_IN_USER_ID)
                .apply();
    }

    // =========================================================================
    // INTERNAL
    // =========================================================================

    /**
     * Get the SharedPreferences instance for authentication data.
     *
     * @return The auth SharedPreferences
     */
    public SharedPreferences getPrefs() {
        return appContext.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE);
    }
}
