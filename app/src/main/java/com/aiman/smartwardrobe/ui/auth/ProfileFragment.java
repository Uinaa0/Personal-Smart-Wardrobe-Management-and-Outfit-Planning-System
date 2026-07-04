package com.aiman.smartwardrobe.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aiman.smartwardrobe.databinding.FragmentProfileBinding;
import com.aiman.smartwardrobe.data.repository.WardrobeRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * ============================================================================
 * ProfileFragment — User Profile & Settings Screen
 * ============================================================================
 *
 * <p>Displays the logged-in user's avatar (initial letters), name, email,
 * and live stats pulled from the Room database:
 * <ul>
 *   <li><b>Items</b>   — total wardrobe item count</li>
 *   <li><b>Outfits</b> — distinct days an outfit was logged (wear events)</li>
 *   <li><b>Wears</b>   — total number of wear events ever recorded</li>
 * </ul></p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class ProfileFragment extends Fragment {

    // =========================================================================
    // VIEW BINDING
    // =========================================================================

    private FragmentProfileBinding binding;

    // =========================================================================
    // DATA
    // =========================================================================

    private WardrobeRepository repository;

    /** Holds all active RxJava subscriptions — disposed in onDestroyView(). */
    private final CompositeDisposable disposables = new CompositeDisposable();

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new WardrobeRepository(requireActivity().getApplication());

        loadUserProfile();
        loadStats();
        setupClickListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }

    // =========================================================================
    // DATA LOADING
    // =========================================================================

    /**
     * Loads the user's stored profile data from SharedPreferences
     * and updates the name, email, and avatar initials in the UI.
     */
    private void loadUserProfile() {
        SharedPreferences prefs = getAuthPrefs();
        String name  = prefs.getString(LoginActivity.KEY_NAME,  "User");
        String email = prefs.getString(LoginActivity.KEY_EMAIL, "user@email.com");

        binding.textProfileName.setText(name);
        binding.textProfileEmail.setText(email);
        binding.textAvatarInitials.setText(generateInitials(name));
    }

    /**
     * Queries the Room database for live stats and updates the three
     * stat cards (Items / Outfits / Wears) with real values.
     *
     * <p>All three queries run concurrently on the IO scheduler and
     * deliver results to the main thread for safe UI updates.</p>
     */
    private void loadStats() {
        // --- Items: total wardrobe item count ---
        disposables.add(
            repository.getItemCount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    count -> {
                        if (binding != null) binding.textStatItems.setText(String.valueOf(count));
                    },
                    throwable -> { /* silently ignore — stat stays at 0 */ }
                )
        );

        // --- Wears: total wear events across all items ---
        disposables.add(
            repository.getTotalWearCount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    count -> {
                        if (binding != null) binding.textStatWears.setText(String.valueOf(count));
                    },
                    throwable -> { /* silently ignore */ }
                )
        );

        // --- Outfits: distinct outfit sessions (events where an item was worn) ---
        // We reuse getTotalWearCount and divide by average items per outfit,
        // but since we don't have an "outfit" entity, we count distinct wear
        // days as a proxy for "outfit sessions".
        disposables.add(
            repository.getMostWornItems(Integer.MAX_VALUE)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    items -> {
                        // Number of unique items that have been worn at least once
                        if (binding != null)
                            binding.textStatOutfits.setText(String.valueOf(items.size()));
                    },
                    throwable -> { /* silently ignore */ }
                )
        );
    }

    // =========================================================================
    // CLICK LISTENERS
    // =========================================================================

    private void setupClickListeners() {
        binding.itemEditProfile.setOnClickListener(v -> showEditProfileDialog());
        binding.itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.itemLogout.setOnClickListener(v -> confirmLogout());
    }

    // =========================================================================
    // DIALOGS
    // =========================================================================

    private void showEditProfileDialog() {
        SharedPreferences prefs = getAuthPrefs();
        String currentName = prefs.getString(LoginActivity.KEY_NAME, "");

        android.widget.EditText editText = new android.widget.EditText(requireContext());
        editText.setText(currentName);
        editText.setHint("Full name");
        int padding = (int) getResources().getDisplayMetrics().density * 20;
        editText.setPadding(padding, padding / 2, padding, padding / 2);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Profile")
                .setMessage("Update your display name")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        prefs.edit().putString(LoginActivity.KEY_NAME, newName).apply();
                        loadUserProfile();
                        if (binding != null)
                            Snackbar.make(binding.getRoot(), "Profile updated!", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) getResources().getDisplayMetrics().density * 20;
        layout.setPadding(padding, padding / 2, padding, 0);

        android.widget.EditText editCurrent = new android.widget.EditText(requireContext());
        editCurrent.setHint("Current password");
        editCurrent.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        android.widget.EditText editNew = new android.widget.EditText(requireContext());
        editNew.setHint("New password (min 6 chars)");
        editNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(editCurrent);
        layout.addView(editNew);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", (dialog, which) -> {
                    SharedPreferences prefs = getAuthPrefs();
                    String current  = editCurrent.getText().toString();
                    String stored   = prefs.getString(LoginActivity.KEY_PASSWORD, "");
                    String newPwd   = editNew.getText().toString();

                    if (binding == null) return;

                    if (!current.equals(stored)) {
                        Snackbar.make(binding.getRoot(), "Current password is incorrect", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPwd.length() < 6) {
                        Snackbar.make(binding.getRoot(), "New password must be at least 6 characters", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    prefs.edit().putString(LoginActivity.KEY_PASSWORD, newPwd).apply();
                    Snackbar.make(binding.getRoot(), "Password updated!", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void performLogout() {
        getAuthPrefs().edit().putBoolean(LoginActivity.KEY_LOGGED_IN, false).apply();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // Use the modern transition API on Android 14+ (API 34), fall back for older devices
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            requireActivity().overrideActivityTransition(
                    android.app.Activity.OVERRIDE_TRANSITION_OPEN,
                    android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Generates up to 2 uppercase initials from a display name.
     * E.g. "Aiman Faris" → "AF", "Aiman" → "A"
     */
    private String generateInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) initials.append(parts[i].charAt(0));
        }
        return initials.toString().toUpperCase();
    }

    private SharedPreferences getAuthPrefs() {
        return requireContext().getSharedPreferences(LoginActivity.PREFS_AUTH, Context.MODE_PRIVATE);
    }
}
