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
import com.aiman.smartwardrobe.databinding.ActivitySignupBinding;
import com.aiman.smartwardrobe.ui.MainActivity;
import com.google.android.material.snackbar.Snackbar;

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

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
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

        // Check if account already exists
        SharedPreferences prefs = getAuthPrefs();
        String existingEmail = prefs.getString(LoginActivity.KEY_EMAIL, null);
        if (existingEmail != null && existingEmail.equalsIgnoreCase(email)) {
            binding.layoutEmail.setError("An account with this email already exists");
            binding.layoutEmail.requestFocus();
            return;
        }

        // Save new account
        prefs.edit()
                .putString(LoginActivity.KEY_NAME, name)
                .putString(LoginActivity.KEY_EMAIL, email)
                .putString(LoginActivity.KEY_PASSWORD, password)
                .putBoolean(LoginActivity.KEY_LOGGED_IN, true)
                .apply();

        // Navigate to main app
        Snackbar.make(binding.getRoot(), "Account created! Welcome, " + name + "!", Snackbar.LENGTH_SHORT).show();
        goToMain();
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
