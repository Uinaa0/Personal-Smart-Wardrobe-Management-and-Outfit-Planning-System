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
import com.aiman.smartwardrobe.databinding.ActivitySignupBinding;
import com.aiman.smartwardrobe.ui.MainActivity;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * ============================================================================
 * SignUpActivity — User Registration Screen
 * ============================================================================
 *
 * <p>Allows new users to create an account by entering their full name,
 * email, password, and confirming the password. Account data is stored
 * securely in {@link SharedPreferences} for demo purposes.</p>
 *
 * <p>On successful registration, the user is logged in automatically
 * and taken directly to {@link MainActivity}.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class SignUpActivity extends AppCompatActivity {

    // =========================================================================
    // VIEW BINDING
    // =========================================================================

    private ActivitySignupBinding binding;

    /** Manages all RxJava subscriptions — disposed in onDestroy(). */
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private SessionManager session;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        session = SessionManager.getInstance(this);

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
        // Create Account button
        binding.buttonSignUp.setOnClickListener(v -> {
            hideKeyboard();
            attemptSignUp();
        });

        // Navigate back to Login
        binding.textSignIn.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    // =========================================================================
    // SIGN UP LOGIC
    // =========================================================================

    /**
     * Validates all input fields and registers the new user account.
     */
    private void attemptSignUp() {
        // Clear previous errors
        binding.layoutName.setError(null);
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);
        binding.layoutConfirmPassword.setError(null);

        String name = getName();
        String email = getEmail();
        String password = getPassword();
        String confirmPassword = getConfirmPassword();

        // Validate name
        if (TextUtils.isEmpty(name)) {
            binding.layoutName.setError("Full name is required");
            binding.layoutName.requestFocus();
            return;
        }
        if (name.length() < 2) {
            binding.layoutName.setError("Name must be at least 2 characters");
            binding.layoutName.requestFocus();
            return;
        }

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
        if (password.length() < 6) {
            binding.layoutPassword.setError("Password must be at least 6 characters");
            binding.layoutPassword.requestFocus();
            return;
        }

        // Validate confirm password
        if (!password.equals(confirmPassword)) {
            binding.layoutConfirmPassword.setError("Passwords do not match");
            binding.layoutConfirmPassword.requestFocus();
            return;
        }

        // Validate terms checkbox
        if (!binding.checkboxTerms.isChecked()) {
            Snackbar.make(binding.getRoot(),
                    "Please agree to the Terms & Privacy Policy",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Check if account already exists in Room Database
        compositeDisposable.add(
                SmartWardrobeDatabase.getInstance(getApplicationContext()).userProfileDao().getUserByEmail(email)
                        .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                        .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                        .subscribe(
                                existingProfile -> {
                                    binding.layoutEmail.setError("An account with this email already exists");
                                    binding.layoutEmail.requestFocus();
                                },
                                throwable -> {
                                    throwable.printStackTrace();
                                    Snackbar.make(binding.getRoot(), "Check failed: " + throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                                },
                                () -> {
                                    // Account does not exist, proceed with sign up
                                    performSignUp(name, email, password);
                                }
                        )
        );
    }

    private void performSignUp(String name, String email, String password) {
        SharedPreferences prefs = getAuthPrefs();
        UserProfile profile = new UserProfile(name, email, HashUtils.hashPassword(password), "{}");
        compositeDisposable.add(
                SmartWardrobeDatabase.getInstance(getApplicationContext()).userProfileDao().insertProfile(profile)
                        .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                        .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                        .subscribe(
                                userId -> {
                                    prefs.edit()
                                            .putString(LoginActivity.KEY_NAME, name)
                                            .putString(LoginActivity.KEY_EMAIL, email)
                                            .putString(LoginActivity.KEY_PASSWORD, HashUtils.hashPassword(password))
                                            .apply();
                                    session.setLoggedIn(email, userId);

                                    Snackbar.make(binding.getRoot(), "Account created! Welcome, " + name + "!", Snackbar.LENGTH_SHORT).show();
                                    goToMain();
                                },
                                throwable -> {
                                    throwable.printStackTrace();
                                    Snackbar.make(binding.getRoot(), "Registration failed: " + throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                        )
        );
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String getName() {
        return binding.editName.getText() != null
                ? binding.editName.getText().toString().trim()
                : "";
    }

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

    private String getConfirmPassword() {
        return binding.editConfirmPassword.getText() != null
                ? binding.editConfirmPassword.getText().toString()
                : "";
    }

    private SharedPreferences getAuthPrefs() {
        return getSharedPreferences(LoginActivity.PREFS_AUTH, Context.MODE_PRIVATE);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
