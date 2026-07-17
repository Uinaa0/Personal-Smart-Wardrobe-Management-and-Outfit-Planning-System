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

import io.reactivex.rxjava3.disposables.CompositeDisposable;

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
 * @version 1.1
 */
public class LoginActivity extends AppCompatActivity {

    // =========================================================================
    // CONSTANTS (kept for backward compatibility with other files)
    // =========================================================================

    /** SharedPreferences file name for user account data. */
    public static final String PREFS_AUTH = SessionManager.PREFS_AUTH;

    /** Key for the stored email. */
    public static final String KEY_EMAIL = SessionManager.KEY_EMAIL;

    /** Key for the stored password (hashed with PBKDF2). */
    public static final String KEY_PASSWORD = SessionManager.KEY_PASSWORD;

    /** Key for the stored display name. */
    public static final String KEY_NAME = SessionManager.KEY_NAME;

    /** Key tracking whether the user is currently logged in. */
    public static final String KEY_LOGGED_IN = SessionManager.KEY_LOGGED_IN;

    // =========================================================================
    // VIEW BINDING & SESSION
    // =========================================================================

    private ActivityLoginBinding binding;
    private SessionManager session;

    /** Manages all RxJava subscriptions — disposed in onDestroy(). */
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = SessionManager.getInstance(this);

        // If user is already logged in, ensure user ID is initialized then jump to Main
        if (session.isLoggedIn()) {
            compositeDisposable.add(
                    session.ensureUserIdInitialized(this::goToMain)
            );
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
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
        // Check credentials against Room Database
        compositeDisposable.add(
                SmartWardrobeDatabase.getInstance(getApplicationContext())
                        .userProfileDao().getUserByEmail(email)
                        .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                        .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                        .subscribe(
                                userProfile -> {
                                    if (HashUtils.verifyPassword(password, userProfile.getPassword())) {
                                        // Save back to SharedPreferences for UI display convenience
                                        session.getPrefs().edit()
                                                .putString(KEY_EMAIL, userProfile.getEmail())
                                                .putString(KEY_NAME, userProfile.getUsername())
                                                .putString(KEY_PASSWORD, userProfile.getPassword())
                                                .apply();
                                        session.setLoggedIn(userProfile.getEmail(), userProfile.getUserId());
                                        showSuccess("Welcome back!");
                                        goToMain();
                                    } else {
                                        binding.layoutPassword.setError("Incorrect password");
                                        binding.layoutPassword.requestFocus();
                                    }
                                },
                                throwable -> {
                                    throwable.printStackTrace();
                                    Snackbar.make(binding.getRoot(), "Login failed: " + throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                                },
                                () -> {
                                    // User profile not found in Room
                                    binding.layoutEmail.setError("Email not found");
                                    binding.layoutEmail.requestFocus();
                                }
                        )
        );
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

    private SharedPreferences getAuthPrefs() {
        return session.getPrefs();
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
