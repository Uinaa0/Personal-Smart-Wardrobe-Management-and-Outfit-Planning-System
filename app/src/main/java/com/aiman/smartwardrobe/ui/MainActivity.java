package com.aiman.smartwardrobe.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.databinding.ActivityMainBinding;
import com.aiman.smartwardrobe.ui.analytics.AnalyticsFragment;
import com.aiman.smartwardrobe.ui.auth.ProfileFragment;
import com.aiman.smartwardrobe.ui.stylist.StylistFragment;
import com.aiman.smartwardrobe.ui.wardrobe.WardrobeFragment;

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
 *   <li><b>Stylist (Modules 2 & 3):</b> The Fit Stylist canvas for
 *       manual outfit composition and the Smart Stylist auto-generator.
 *       <i>(Placeholder — to be implemented in future modules.)</i></li>
 *   <li><b>Analytics (Module 4):</b> The Analytics Dashboard with
 *       Cost-Per-Wear metrics and wardrobe insights.
 *       <i>(Placeholder — to be implemented in future modules.)</i></li>
 * </ol>
 *
 * <p><b>Fragment Management:</b>
 * Each bottom navigation tab is associated with a Fragment. When the user
 * taps a tab, the current fragment is replaced with the corresponding
 * module's fragment. The {@code FragmentManager} handles the transaction.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    // =========================================================================
    // VIEW BINDING
    // =========================================================================

    /** Generated ViewBinding class for activity_main.xml */
    private ActivityMainBinding binding;

    // =========================================================================
    // ACTIVITY LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the bottom navigation
        setupBottomNavigation();

        // Load the default fragment (Wardrobe) on first launch
        if (savedInstanceState == null) {
            loadFragment(new WardrobeFragment());
        }
    }

    // =========================================================================
    // NAVIGATION SETUP
    // =========================================================================

    /**
     * Configure the BottomNavigationView to switch fragments.
     *
     * <p>Each menu item in {@code bottom_nav_menu.xml} is mapped to a
     * Fragment class. When the user taps a tab, the corresponding
     * fragment is loaded into the container.</p>
     */
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_wardrobe) {
                // Module 1: Digital Inventory
                fragment = new WardrobeFragment();

            } else if (itemId == R.id.nav_stylist) {
                // Modules 2 & 3: Fit Stylist + Smart Stylist
                fragment = new StylistFragment();

            } else if (itemId == R.id.nav_analytics) {
                // Module 4: Analytics Dashboard
                fragment = new AnalyticsFragment();

            } else if (itemId == R.id.nav_profile) {
                // Profile & Settings
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
     }

     /**
      * Replace the current fragment in the container.
      *
      * @param fragment The fragment to display
      */
     private void loadFragment(Fragment fragment) {
         getSupportFragmentManager()
                 .beginTransaction()
                 .replace(R.id.fragment_container, fragment)
                 .commit();
     }

     // =========================================================================
     // PLACEHOLDER FRAGMENT — For Future Modules
     // =========================================================================

     /**
      * Temporary placeholder fragment for unimplemented modules.
      * Displays a title and description message.
      * Will be replaced with actual module fragments in future development.
      */
     public static class PlaceholderFragment extends Fragment {
         private String title;
         private String message;

         /**
          * Required empty public constructor for Fragment instantiation/recreation by the system.
          */
         public PlaceholderFragment() {
         }

         /**
          * Factory method to construct the fragment with arguments.
          *
          * @param title   The screen title
          * @param message The details message
          * @return A configured PlaceholderFragment instance
          */
         public static PlaceholderFragment newInstance(String title, String message) {
             PlaceholderFragment fragment = new PlaceholderFragment();
             Bundle args = new Bundle();
             args.putString("title", title);
             args.putString("message", message);
             fragment.setArguments(args);
             return fragment;
         }

         @Override
         public void onCreate(android.os.Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             if (getArguments() != null) {
                 title = getArguments().getString("title");
                 message = getArguments().getString("message");
             }
         }

         @Override
         public android.view.View onCreateView(
                 android.view.LayoutInflater inflater,
                 android.view.ViewGroup container,
                 Bundle savedInstanceState) {

             // Simple placeholder layout created programmatically
             android.widget.LinearLayout layout = new android.widget.LinearLayout(
                     requireContext());
             layout.setOrientation(android.widget.LinearLayout.VERTICAL);
             layout.setGravity(android.view.Gravity.CENTER);
             layout.setPadding(64, 64, 64, 64);

             android.widget.TextView titleView = new android.widget.TextView(
                     requireContext());
             titleView.setText(title);
             titleView.setTextSize(24);
             titleView.setGravity(android.view.Gravity.CENTER);
             titleView.setTextColor(getResources().getColor(
                     com.google.android.material.R.color.material_on_surface_emphasis_high_type,
                     requireContext().getTheme()));

             android.widget.TextView messageView = new android.widget.TextView(
                     requireContext());
             messageView.setText(message);
             messageView.setTextSize(16);
             messageView.setGravity(android.view.Gravity.CENTER);
             messageView.setPadding(0, 32, 0, 0);

             layout.addView(titleView);
             layout.addView(messageView);
             return layout;
         }
     }
}
