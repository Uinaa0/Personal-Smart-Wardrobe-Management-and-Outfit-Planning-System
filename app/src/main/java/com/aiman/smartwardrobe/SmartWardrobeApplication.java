package com.aiman.smartwardrobe;

import android.app.Application;

import com.aiman.smartwardrobe.data.SmartWardrobeDatabase;

/**
 * ============================================================================
 * SmartWardrobeApplication — Custom Application Class
 * ============================================================================
 *
 * <p>
 * This class extends {@link Application} and serves as the entry point
 * for application-wide initialization. It is instantiated ONCE when the
 * app process starts, before any Activity or Service is created.
 * </p>
 *
 * <p>
 * <b>Registered in AndroidManifest.xml:</b>
 * {@code android:name=".SmartWardrobeApplication"}
 * </p>
 *
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 * <li>Eagerly initialize the Room database singleton to avoid
 * first-access latency on the UI thread.</li>
 * <li>Serve as a global context provider for components that
 * need Application-level context (e.g., Repository).</li>
 * </ul>
 * </p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class SmartWardrobeApplication extends Application {

    /**
     * Called when the application is starting, before any activity,
     * service, or receiver objects have been created.
     *
     * <p>
     * We initialize the database here to:
     * <ol>
     * <li>Trigger the singleton creation early</li>
     * <li>Run the pre-seeding callback (styling rules) in the background</li>
     * <li>Ensure the database is ready when the first Activity launches</li>
     * </ol>
     * </p>
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Room database singleton eagerly.
        // This triggers the database creation callback which pre-seeds
        // the StylingOntology table with default rules.
        SmartWardrobeDatabase.getInstance(this);
    }
}
