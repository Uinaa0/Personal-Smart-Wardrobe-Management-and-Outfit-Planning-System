package com.aiman.smartwardrobe.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.migration.Migration;

import com.aiman.smartwardrobe.data.dao.CalendarEventDao;
import com.aiman.smartwardrobe.data.dao.StylingOntologyDao;
import com.aiman.smartwardrobe.data.dao.UserProfileDao;
import com.aiman.smartwardrobe.data.dao.WardrobeDao;
import com.aiman.smartwardrobe.data.entity.CalendarEvent;
import com.aiman.smartwardrobe.data.entity.StylingOntology;
import com.aiman.smartwardrobe.data.entity.UserProfile;
import com.aiman.smartwardrobe.data.entity.WardrobeItem;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import static com.aiman.smartwardrobe.data.ClothingCategory.*;

/**
 * ============================================================================
 * SmartWardrobeDatabase — The Room Database Singleton
 * ============================================================================
 *
 * <p>This class is the <b>main access point</b> to the SQLite database.
 * It serves as the central hub that connects all Entity classes to their
 * corresponding DAO interfaces.</p>
 *
 * <p><b>Singleton Pattern:</b>
 * Only ONE instance of the database should exist throughout the app's
 * lifecycle. The double-checked locking pattern in {@link #getInstance(Context)}
 * ensures thread-safe, lazy initialization. This is critical because:
 * <ul>
 *   <li>Creating a database instance is expensive (file I/O)</li>
 *   <li>Multiple instances could cause data corruption</li>
 *   <li>Room enforces a single-writer model for consistency</li>
 * </ul></p>
 *
 * <p><b>Architecture Position:</b>
 * <pre>
 *   UI Layer (Fragment/Activity)
 *       ↓
 *   ViewModel (LiveData)
 *       ↓
 *   Repository (Business Logic + Threading)
 *       ↓
 *   DAO (Database Operations)               ← Accessed via this class
 *       ↓
 *   SmartWardrobeDatabase (THIS CLASS)      ← Room @Database
 *       ↓
 *   SQLite (On-disk storage)
 * </pre></p>
 *
 * <p><b>Database Configuration:</b></p>
 * <ul>
 *   <li><b>entities:</b> All 4 entity classes registered</li>
 *   <li><b>version:</b> 1 (initial schema version)</li>
 *   <li><b>exportSchema:</b> false (schema JSON not exported;
 *       set to true for production apps to track migrations)</li>
 * </ul>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
@Database(
    entities = {
        UserProfile.class,
        WardrobeItem.class,
        CalendarEvent.class,
        StylingOntology.class
    },
    version = 4,
    exportSchema = false
)
public abstract class SmartWardrobeDatabase extends RoomDatabase {

    // =========================================================================
    // SINGLETON INSTANCE
    // =========================================================================

    /**
     * The single instance of the database.
     * Marked {@code volatile} to ensure visibility across threads.
     *
     * <p><b>Why volatile?</b> In the double-checked locking pattern,
     * without volatile, a thread might see a partially constructed
     * object due to instruction reordering by the JVM. The volatile
     * keyword prevents this optimization and ensures the instance is
     * fully initialized before it becomes visible to other threads.</p>
     */
    private static volatile SmartWardrobeDatabase INSTANCE;

    /** Database file name on disk. */
    private static final String DATABASE_NAME = "smart_wardrobe_database";

    // =========================================================================
    // ABSTRACT DAO ACCESSORS
    // =========================================================================
    // Room generates the concrete implementations of these methods.
    // Each method returns a DAO instance that provides access to a
    // specific entity's database operations.
    // =========================================================================

    /** @return DAO for wardrobe item operations (CRUD + queries) */
    public abstract WardrobeDao wardrobeDao();

    /** @return DAO for calendar event / wear history operations */
    public abstract CalendarEventDao calendarEventDao();

    /** @return DAO for user profile operations */
    public abstract UserProfileDao userProfileDao();

    /** @return DAO for styling ontology rule operations */
    public abstract StylingOntologyDao stylingOntologyDao();

    // =========================================================================
    // SINGLETON ACCESSOR
    // =========================================================================

    /**
     * Get the singleton database instance.
     *
     * <p>Uses the <b>double-checked locking</b> pattern for thread-safe,
     * lazy initialization:
     * <ol>
     *   <li>First check (without lock): Fast path — if instance exists,
     *       return it immediately without synchronization overhead.</li>
     *   <li>Synchronized block: Only entered on first creation.</li>
     *   <li>Second check (with lock): Prevents duplicate creation if two
     *       threads passed the first check simultaneously.</li>
     * </ol></p>
     *
     * <p><b>Pre-seeded Data:</b> On first creation, the database is
     * pre-populated with default styling rules via the
     * {@link #sRoomDatabaseCallback} callback. This ensures the Smart
     * Stylist has rules to work with immediately.</p>
     *
     * @param context Application context (NOT Activity context, to avoid
     *                memory leaks — Room holds a reference to this context)
     * @return The singleton SmartWardrobeDatabase instance
     */
    /**
     * Migration from database version 3 to 4.
     * Adds the email and password columns to the user_profile table.
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_profile ADD COLUMN email TEXT");
            database.execSQL("ALTER TABLE user_profile ADD COLUMN password TEXT");
        }
    };

    @SuppressWarnings("deprecation") // fallbackToDestructiveMigration() is acceptable during FYP development
    public static SmartWardrobeDatabase getInstance(Context context) {
        if (INSTANCE == null) {                          // First check (no lock)
            synchronized (SmartWardrobeDatabase.class) { // Acquire lock
                if (INSTANCE == null) {                  // Second check (with lock)
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SmartWardrobeDatabase.class,
                            DATABASE_NAME
                    )
                    // Add proper migrations to preserve user data
                    .addMigrations(MIGRATION_3_4)
                    // Callback to pre-seed the database with default styling rules
                    .addCallback(sRoomDatabaseCallback)
                    // WARNING: fallbackToDestructiveMigration() destroys all data
                    // when the schema version changes. This is acceptable during
                    // development but MUST be replaced with proper Migration
                    // objects (e.g., Migration(1, 2) with ALTER TABLE statements)
                    // before any production release to preserve user data.
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    // =========================================================================
    // DATABASE CALLBACK — Pre-seed Default Styling Rules
    // =========================================================================

    /**
     * Room callback that fires when the database is first created.
     *
     * <p>This callback pre-populates the STYLING_ONTOLOGY table with
     * default rules so that the Smart Stylist (Module 3) has a working
     * knowledge base from the very first app launch.</p>
     *
     * <p><b>Thread Safety:</b> The callback runs the insert on a
     * background thread via {@code Executors.newSingleThreadExecutor()}
     * because Room does not allow database operations on the main thread.</p>
     */
    private static final RoomDatabase.Callback sRoomDatabaseCallback =
            new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);

                    // Pre-seed styling rules on a background thread
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (INSTANCE != null) {
                            StylingOntologyDao dao = INSTANCE.stylingOntologyDao();

                            // Define default styling rules
                            List<StylingOntology> defaultRules = Arrays.asList(
                                new StylingOntology(
                                    "Casual Hot",
                                    T_SHIRT + "," + SHORTS + "," + SNEAKERS + "," + DRESS + "," + SKIRT,
                                    45.0
                                ),
                                new StylingOntology(
                                    "Casual Warm",
                                    T_SHIRT + "," + SHIRT + "," + JEANS + "," + PANTS + "," + SNEAKERS + "," + SHOES,
                                    28.0
                                ),
                                new StylingOntology(
                                    "Casual Cool",
                                    HOODIE + "," + SWEATER + "," + SHIRT + "," + JEANS + "," + PANTS + "," + BOOTS + "," + SNEAKERS,
                                    18.0
                                ),
                                new StylingOntology(
                                    "Formal",
                                    SHIRT + "," + PANTS + "," + DRESS + "," + SHOES,
                                    35.0
                                ),
                                new StylingOntology(
                                    "Winter",
                                    JACKET + "," + SWEATER + "," + HOODIE + "," + JEANS + "," + PANTS + "," + BOOTS,
                                    10.0
                                )
                            );

                            // Insert all rules — blockingAwait() is safe here
                            // because we're already on a background thread
                            dao.insertAllRules(defaultRules).blockingAwait();
                        }
                    });
                }
            };
}
