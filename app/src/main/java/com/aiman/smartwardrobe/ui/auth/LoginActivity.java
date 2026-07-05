package com.aiman.smartwardrobe.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.data.SmartWardrobeDatabase;
import com.aiman.smartwardrobe.data.entity.UserProfile;
import com.aiman.smartwardrobe.databinding.ActivityLoginBinding;
import com.aiman.smartwardrobe.ui.MainActivity;
import com.google.android.material.snackbar.Snackbar;
import android.widget.Toast;

/**
 * ============================================================================
 * LoginActivity — User Authentication Screen
 * ============================================================================
 *
 * <p>Provides a premium login interface where users sign in with their
 * email and password. Credentials are validated against SharedPreferences
 * where the account was created at sign-up.</p>
 *
 * <p>On successful login, the user is taken to {@link MainActivity}.
 * If already logged in (session persists), this screen is bypassed.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class LoginActivity extends AppCompatActivity {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** SharedPreferences file name for user account data. */
    public static final String PREFS_AUTH = "smart_wardrobe_auth";

    /** Key for the stored email. */
    public static final String KEY_EMAIL = "user_email";

    /** Key for the stored password (hashed in production; plain for demo). */
    public static final String KEY_PASSWORD = "user_password";

    /** Key for the stored display name. */
    public static final String KEY_NAME = "user_name";

    /** Key tracking whether the user is currently logged in. */
    public static final String KEY_LOGGED_IN = "is_logged_in";

    // =========================================================================
    // VIEW BINDING
    // =========================================================================

    private ActivityLoginBinding binding;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If user is already logged in, jump straight to MainActivity
        if (isUserLoggedIn()) {
            goToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    // =========================================================================
    // SETUP
    // =========================================================================

    private void setupClickListeners() {
        // Sign In button
        binding.buttonLogin.setOnClickListener(v -> {
            hideKeyboard();
            attemptLogin();
        });

        // Navigate to Sign Up screen
        binding.textSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        // Forgot password (placeholder)
        binding.textForgotPassword.setOnClickListener(v -> {
            Snackbar.make(binding.getRoot(),
                    "Password reset is not yet available.",
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    // =========================================================================
    // LOGIN LOGIC
    // =========================================================================

    /**
     * Validates input fields and authenticates the user against stored credentials.
     */
    private void attemptLogin() {
        // Clear previous errors
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);

        String email = getEmail();
        String password = getPassword();

        // Validate email
        if (TextUtils.isEmpty(email)) {
            binding.layoutEmail.setError("Email is required");
            binding.layoutEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.setError("Enter a valid email address");
            binding.layoutEmail.requestFocus();
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            binding.layoutPassword.setError("Password is required");
            binding.layoutPassword.requestFocus();
            return;
        }

        // Check credentials against SharedPreferences
        SharedPreferences prefs = getAuthPrefs();
        String storedEmail = prefs.getString(KEY_EMAIL, null);
        String storedPassword = prefs.getString(KEY_PASSWORD, null);

        if (storedEmail == null) {
            // No account exists yet — prompt to sign up
            Snackbar.make(binding.getRoot(),
                    "No account found. Please create an account first.",
                    Snackbar.LENGTH_LONG)
                    .setAction("Sign Up", v -> {
                        Intent intent = new Intent(this, SignUpActivity.class);
                        startActivity(intent);
                    })
                    .show();
            return;
        }

        if (!email.equalsIgnoreCase(storedEmail)) {
            binding.layoutEmail.setError("Email not found");
            binding.layoutEmail.requestFocus();
            return;
        }

        if (!HashUtils.hashPassword(password).equals(storedPassword)) {
            binding.layoutPassword.setError("Incorrect password");
            binding.layoutPassword.requestFocus();
            return;
        }

        // Login successful — set user ID and redirect to Main
        long userId = prefs.getLong("user_id_" + email, -1);
        if (userId == -1) {
            UserProfile profile = new UserProfile(prefs.getString(KEY_NAME, "User"), "{}");
            SmartWardrobeDatabase.getInstance(getApplicationContext()).userProfileDao().insertProfile(profile)
                    .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                    .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                    .subscribe(
                            newId -> {
                                prefs.edit()
                                        .putLong("user_id_" + email, newId)
                                        .putLong("logged_in_user_id", newId)
                                        .putBoolean(KEY_LOGGED_IN, true)
                                        .apply();
                                showSuccess("Welcome back!");
                                goToMain();
                            },
                            throwable -> {
                                throwable.printStackTrace();
                                Toast.makeText(this, "Session initialization failed", Toast.LENGTH_SHORT).show();
                            }
                    );
        } else {
            prefs.edit()
                    .putLong("logged_in_user_id", userId)
                    .putBoolean(KEY_LOGGED_IN, true)
                    .apply();
            showSuccess("Welcome back!");
            goToMain();
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String getEmail() {
        return binding.editEmail.getText() != null
                ? binding.editEmail.getText().toString().trim()
                : "";
    }

    private String getPassword() {
        return binding.editPassword.getText() != null
                ? binding.editPassword.getText().toString()
                : "";
    }

    private boolean isUserLoggedIn() {
        return getAuthPrefs().getBoolean(KEY_LOGGED_IN, false);
    }

    private SharedPreferences getAuthPrefs() {
        return getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showSuccess(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
