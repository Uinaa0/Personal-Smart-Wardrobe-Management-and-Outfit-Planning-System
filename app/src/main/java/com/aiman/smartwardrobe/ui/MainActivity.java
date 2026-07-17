package com.aiman.smartwardrobe.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.databinding.ActivityMainBinding;
import com.aiman.smartwardrobe.ui.analytics.AnalyticsFragment;
import com.aiman.smartwardrobe.ui.auth.ProfileFragment;
import com.aiman.smartwardrobe.ui.auth.SessionManager;
import com.aiman.smartwardrobe.ui.stylist.StylistFragment;
import com.aiman.smartwardrobe.ui.wardrobe.WardrobeFragment;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * ============================================================================
 * MainActivity — The Main Entry Point of the Smart Wardrobe App
 * ============================================================================
 *
 * <p>This activity serves as the host for the app's primary navigation.
 * It uses a Material Design 3 {@code BottomNavigationView} to switch
 * between the three main modules:</p>
 *
 * <ol>
 *   <li><b>Wardrobe (Module 1):</b> The Digital Inventory — a grid view
 *       of all clothing items with filtering capabilities.</li>
 *   <li><b>Stylist (Modules 2 &amp; 3):</b> The Fit Stylist canvas for
 *       manual outfit composition and the Smart Stylist auto-generator.</li>
 *   <li><b>Analytics (Module 4):</b> The Analytics Dashboard with
 *       Cost-Per-Wear metrics and wardrobe insights.</li>
 *   <li><b>Profile:</b> User settings, notification preferences, and
 *       account management.</li>
 * </ol>
 *
 * <p><b>Fragment Management:</b>
 * Fragments are cached using show/hide transactions instead of replace.
 * This preserves scroll position, loaded data, and ViewModel state when
 * switching between tabs — eliminating unnecessary data reloads.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.1
 */
public class MainActivity extends AppCompatActivity {

    // =========================================================================
    // VIEW BINDING
    // =========================================================================

    /** Generated ViewBinding class for activity_main.xml */
    private ActivityMainBinding binding;

    // =========================================================================
    // FRAGMENT CACHE (Fix #11 — avoid recreation on tab switch)
    // =========================================================================

    private Fragment wardrobeFragment;
    private Fragment stylistFragment;
    private Fragment analyticsFragment;
    private Fragment profileFragment;
    private Fragment activeFragment;

    // =========================================================================
    // RXJAVA DISPOSABLE MANAGEMENT (Fix #3 — prevent leaks)
    // =========================================================================

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // =========================================================================
    // ACTIVITY LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure active user ID is initialized using the centralized SessionManager
        SessionManager session = SessionManager.getInstance(this);
        if (session.isLoggedIn()) {
            compositeDisposable.add(
                    session.ensureUserIdInitialized(null)
            );
        }

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize fragments on first launch
        if (savedInstanceState == null) {
            initializeFragments();
        }

        // Set up the bottom navigation
        setupBottomNavigation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    // =========================================================================
    // FRAGMENT INITIALIZATION
    // =========================================================================

    /**
     * Creates all fragment instances and adds them to the FragmentManager.
     * Only the wardrobe fragment is shown initially; all others are hidden.
     *
     * <p>This approach (add + show/hide) is more efficient than replace()
     * because it preserves each fragment's view state and ViewModel when
     * switching tabs.</p>
     */
    private void initializeFragments() {
        wardrobeFragment = new WardrobeFragment();
        stylistFragment = new StylistFragment();
        analyticsFragment = new AnalyticsFragment();
        profileFragment = new ProfileFragment();
        activeFragment = wardrobeFragment;

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                .add(R.id.fragment_container, analyticsFragment, "analytics").hide(analyticsFragment)
                .add(R.id.fragment_container, stylistFragment, "stylist").hide(stylistFragment)
                .add(R.id.fragment_container, wardrobeFragment, "wardrobe")
                .commit();
    }

    // =========================================================================
    // NAVIGATION SETUP
    // =========================================================================

    /**
     * Configure the BottomNavigationView to switch fragments using
     * show/hide transactions instead of replace.
     *
     * <p>Each menu item in {@code bottom_nav_menu.xml} is mapped to a
     * cached Fragment instance. When the user taps a tab, the current
     * fragment is hidden and the target fragment is shown.</p>
     */
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment target = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_wardrobe) {
                target = wardrobeFragment;
            } else if (itemId == R.id.nav_stylist) {
                target = stylistFragment;
            } else if (itemId == R.id.nav_analytics) {
                target = analyticsFragment;
            } else if (itemId == R.id.nav_profile) {
                target = profileFragment;
            }

            if (target != null && target != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(activeFragment)
                        .show(target)
                        .commit();
                activeFragment = target;
                return true;
            }
            return target != null;
        });
     }
}
