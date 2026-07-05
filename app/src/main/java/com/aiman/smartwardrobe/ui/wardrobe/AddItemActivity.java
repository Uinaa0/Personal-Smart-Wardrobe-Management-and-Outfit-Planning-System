package com.aiman.smartwardrobe.ui.wardrobe;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.data.entity.WardrobeItem;
import com.aiman.smartwardrobe.data.repository.WardrobeRepository;
import com.aiman.smartwardrobe.databinding.ActivityAddItemBinding;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ============================================================================
 * AddItemActivity — Form for Adding a New Wardrobe Item
 * ============================================================================
 *
 * <p>This activity provides a comprehensive form for the user to photograph
 * and catalogue a new clothing item into their digital wardrobe. The form
 * captures all the metadata fields defined in the {@link WardrobeItem}
 * entity.</p>
 *
 * <p><b>Form Fields:</b></p>
 * <ul>
 *   <li><b>Image:</b> Picked from the device gallery. The image is copied
 *       to the app's internal storage and the local path is saved.</li>
 *   <li><b>Category:</b> Dropdown spinner (e.g., T-Shirt, Jeans, etc.)</li>
 *   <li><b>Color:</b> Selectable color palette with predefined colors</li>
 *   <li><b>Fabric Type:</b> Dropdown spinner (e.g., Cotton, Denim, etc.)</li>
 *   <li><b>Purchase Price:</b> Numeric input field</li>
 * </ul>
 *
 * <p><b>Image Handling:</b>
 * When the user selects an image from the gallery, the app copies it to
 * the internal storage directory ({@code files/wardrobe_images/}).
 * This ensures the image remains accessible even if the original file
 * is moved or deleted from the gallery.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 * @see com.aiman.smartwardrobe.data.entity.WardrobeItem
 */
public class AddItemActivity extends AppCompatActivity {

    // =========================================================================
    // VIEW BINDING & DATA
    // =========================================================================

    private ActivityAddItemBinding binding;
    private WardrobeRepository repository;
    private CompositeDisposable compositeDisposable;

    /** The path to the selected image (after copying to internal storage) */
    private String selectedImagePath = null;

    /** The selected color hex string */
    private String selectedColorHex = "#000000";

    // =========================================================================
    // EDIT MODE STATE
    // =========================================================================

    /** Intent extra key for passing item ID to enable edit mode */
    public static final String EXTRA_ITEM_ID = "extra_item_id";

    /** Whether the activity is in edit mode (true) or add mode (false) */
    private boolean isEditMode = false;

    /** The ID of the item being edited (only valid when isEditMode == true) */
    private long editItemId = -1;

    /** The original dateAdded timestamp (preserved during edit) */
    private long originalDateAdded = 0;

    /** The original userId (preserved during edit) */
    private long originalUserId = 0;

    /** The original favorite status (preserved during edit) */
    private boolean originalIsFavorite = false;

    // =========================================================================
    // PREDEFINED COLOR PALETTE
    // =========================================================================

    /**
     * A curated palette of common clothing colors.
     * Each entry is a hex color string that maps to a tappable circle
     * in the color picker grid.
     */
    private static final String[] COLOR_PALETTE = {
            "#000000", // Black
            "#FFFFFF", // White
            "#1C1C1C", // Charcoal
            "#808080", // Gray
            "#C0C0C0", // Silver
            "#000080", // Navy
            "#4169E1", // Royal Blue
            "#87CEEB", // Sky Blue
            "#8B0000", // Dark Red
            "#FF0000", // Red
            "#FF69B4", // Hot Pink
            "#800020", // Burgundy
            "#006400", // Dark Green
            "#228B22", // Forest Green
            "#98FB98", // Pale Green
            "#556B2F", // Olive
            "#F5F5DC", // Beige
            "#D2B48C", // Tan
            "#8B4513", // Saddle Brown
            "#FFD700", // Gold
            "#FF8C00", // Dark Orange
            "#FF6347", // Tomato
            "#800080", // Purple
            "#E6E6FA", // Lavender
    };

    // =========================================================================
    // ACTIVITY RESULT LAUNCHER — Image Picker
    // =========================================================================

    /**
     * Modern ActivityResult API for handling the image picker result.
     *
     * <p>This replaces the deprecated {@code onActivityResult()} method.
     * When the user picks an image from the gallery, this callback:
     * <ol>
     *   <li>Receives the image URI</li>
     *   <li>Copies the image to internal storage</li>
     *   <li>Displays the image preview</li>
     *   <li>Stores the local path for database insertion</li>
     * </ol></p>
     */
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                handleImageSelected(imageUri);
                            }
                        }
                    }
            );

    // =========================================================================
    // ACTIVITY LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new WardrobeRepository(getApplication());
        compositeDisposable = new CompositeDisposable();

        // Determine if we're in edit mode
        editItemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);
        isEditMode = editItemId != -1;

        setupToolbar();
        setupCategorySpinner();
        setupFabricSpinner();
        setupColorPicker();
        setupImagePicker();
        setupSaveButton();

        if (isEditMode) {
            loadItemForEditing();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    // =========================================================================
    // SETUP METHODS
    // =========================================================================

    /**
     * Configure the Material Toolbar with a back navigation button.
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            // Set title based on mode
            getSupportActionBar().setTitle(
                    isEditMode ? R.string.title_edit_item : R.string.title_add_item);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Set up the Category dropdown with predefined clothing categories.
     * Uses an AutoCompleteTextView inside a TextInputLayout for
     * Material Design 3 exposed dropdown styling.
     */
    private void setupCategorySpinner() {
        String[] categories = getResources()
                .getStringArray(R.array.clothing_categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        binding.dropdownCategory.setAdapter(adapter);
    }

    /**
     * Set up the Fabric Type dropdown with predefined fabric options.
     */
    private void setupFabricSpinner() {
        String[] fabrics = getResources()
                .getStringArray(R.array.fabric_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                fabrics
        );
        binding.dropdownFabric.setAdapter(adapter);
    }

    /**
     * Set up the color picker — a grid of tappable color circles.
     *
     * <p>Each circle is a small View with a circular background drawable
     * tinted with one of the {@link #COLOR_PALETTE} colors. Tapping a
     * color selects it and shows a check mark on the selected circle.</p>
     */
    private void setupColorPicker() {
        binding.colorPickerGrid.removeAllViews();

        for (String colorHex : COLOR_PALETTE) {
            // Create a color circle view
            View colorView = getLayoutInflater().inflate(
                    R.layout.item_color_circle, binding.colorPickerGrid, false);

            // Set the circle color
            View circle = colorView.findViewById(R.id.view_color_circle);
            GradientDrawable drawable = (GradientDrawable) circle.getBackground();
            drawable.setColor(Color.parseColor(colorHex));

            // Add white border for light colors
            int color = Color.parseColor(colorHex);
            if (Color.red(color) > 200 && Color.green(color) > 200
                    && Color.blue(color) > 200) {
                drawable.setStroke(2, Color.LTGRAY);
            }

            // Check mark for selected color
            View checkMark = colorView.findViewById(R.id.view_check_mark);

            // Select the first color by default
            if (colorHex.equals(selectedColorHex)) {
                checkMark.setVisibility(View.VISIBLE);
            }

            // Handle color selection
            colorView.setOnClickListener(v -> {
                selectedColorHex = colorHex;

                // Update check marks — hide all, show selected
                for (int i = 0; i < binding.colorPickerGrid.getChildCount(); i++) {
                    View child = binding.colorPickerGrid.getChildAt(i);
                    View mark = child.findViewById(R.id.view_check_mark);
                    if (mark != null) {
                        mark.setVisibility(View.GONE);
                    }
                }
                checkMark.setVisibility(View.VISIBLE);

                // Update the color preview text
                binding.textSelectedColor.setText(colorHex);
            });

            binding.colorPickerGrid.addView(colorView);
        }
    }

    /**
     * Set up the image picker — launches the system gallery when tapped.
     */
    private void setupImagePicker() {
        binding.cardImagePicker.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
    }

    /**
     * Set up the Save button with form validation.
     */
    private void setupSaveButton() {
        // Set button text based on mode
        binding.buttonSave.setText(
                isEditMode ? R.string.button_update_item : R.string.button_save_item);

        binding.buttonSave.setOnClickListener(v -> {
            if (validateForm()) {
                saveItem();
            }
        });
    }

    // =========================================================================
    // EDIT MODE — Load Existing Item Data
    // =========================================================================

    /**
     * Load the existing item from the database and pre-fill all form fields.
     * Called only when the activity is in edit mode.
     */
    private void loadItemForEditing() {
        Disposable disposable = repository.getItemById(editItemId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        item -> {
                            // Preserve original metadata
                            originalDateAdded = item.getDateAdded();
                            originalUserId = item.getUserId();

                            // Pre-fill category dropdown
                            binding.dropdownCategory.setText(item.getCategory(), false);

                            // Pre-fill fabric dropdown
                            binding.dropdownFabric.setText(item.getFabricType(), false);

                            // Pre-fill price
                            binding.editPrice.setText(String.valueOf(item.getPurchasePrice()));

                            // Pre-fill name
                            if (item.getName() != null) {
                                binding.editName.setText(item.getName());
                            }

                            originalIsFavorite = item.isFavorite();

                            // Pre-fill color selection
                            selectedColorHex = item.getColorHex();
                            binding.textSelectedColor.setText(selectedColorHex);
                            updateColorPickerSelection();

                            // Pre-fill image
                            if (item.getImagePath() != null) {
                                selectedImagePath = item.getImagePath();
                                java.io.File imageFile = new java.io.File(selectedImagePath);
                                if (imageFile.exists()) {
                                    Glide.with(AddItemActivity.this)
                                            .load(imageFile)
                                            .centerCrop()
                                            .into(binding.imagePreview);
                                    binding.imagePreview.setVisibility(View.VISIBLE);
                                    binding.textImagePlaceholder.setVisibility(View.GONE);
                                }
                            }
                        },
                        throwable -> {
                            throwable.printStackTrace();
                            Toast.makeText(this, "Error loading item",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                );
        compositeDisposable.add(disposable);
    }

    /**
     * Update the color picker grid to show the check mark on the currently
     * selected color. Called during edit mode item loading.
     */
    private void updateColorPickerSelection() {
        for (int i = 0; i < binding.colorPickerGrid.getChildCount(); i++) {
            View child = binding.colorPickerGrid.getChildAt(i);
            View checkMark = child.findViewById(R.id.view_check_mark);
            if (checkMark != null) {
                // Show check mark only on the matching color
                if (i < COLOR_PALETTE.length
                        && COLOR_PALETTE[i].equalsIgnoreCase(selectedColorHex)) {
                    checkMark.setVisibility(View.VISIBLE);
                } else {
                    checkMark.setVisibility(View.GONE);
                }
            }
        }
    }

    // =========================================================================
    // IMAGE HANDLING
    // =========================================================================

    /**
     * Handle the selected image: copy to internal storage and show preview.
     *
     * <p><b>Why copy to internal storage?</b>
     * The URI returned by the gallery picker may be a content:// URI
     * that could become invalid if the original image is moved or deleted.
     * Copying to internal storage ensures the image remains accessible
     * for the lifetime of the app.</p>
     *
     * @param imageUri The URI of the selected image from the gallery
     */
    private void handleImageSelected(Uri imageUri) {
        try {
            // Create the wardrobe images directory if it doesn't exist
            File imageDir = new File(getFilesDir(), "wardrobe_images");
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }

            // Generate a unique filename using the current timestamp
            String fileName = "item_" + System.currentTimeMillis() + ".png";
            File destFile = new File(imageDir, fileName);

            // Copy the image from the content URI to internal storage
            try (InputStream inputStream = getContentResolver()
                    .openInputStream(imageUri);
                 OutputStream outputStream = new FileOutputStream(destFile)) {

                if (inputStream == null) {
                    Toast.makeText(this, "Failed to read image",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // Store the local path for database insertion
            selectedImagePath = destFile.getAbsolutePath();

            // Show the image preview using Glide
            Glide.with(this)
                    .load(destFile)
                    .centerCrop()
                    .into(binding.imagePreview);

            // Show the image, hide the placeholder text
            binding.imagePreview.setVisibility(View.VISIBLE);
            binding.textImagePlaceholder.setVisibility(View.GONE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // FORM VALIDATION
    // =========================================================================

    /**
     * Validate all form fields before saving.
     *
     * @return true if all required fields are filled, false otherwise
     */
    private boolean validateForm() {
        boolean isValid = true;

        // Validate category
        if (TextUtils.isEmpty(binding.dropdownCategory.getText())) {
            binding.layoutCategory.setError("Please select a category");
            isValid = false;
        } else {
            binding.layoutCategory.setError(null);
        }

        // Validate fabric type
        if (TextUtils.isEmpty(binding.dropdownFabric.getText())) {
            binding.layoutFabric.setError("Please select a fabric type");
            isValid = false;
        } else {
            binding.layoutFabric.setError(null);
        }

        // Validate price
        String priceText = binding.editPrice.getText().toString().trim();
        if (TextUtils.isEmpty(priceText)) {
            binding.layoutPrice.setError("Please enter a price");
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(priceText);
                if (price < 0) {
                    binding.layoutPrice.setError("Price cannot be negative");
                    isValid = false;
                } else {
                    binding.layoutPrice.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.layoutPrice.setError("Invalid price format");
                isValid = false;
            }
        }

        // Validate image (optional but recommended)
        if (selectedImagePath == null) {
            Toast.makeText(this, "Tip: Add a photo for best results!",
                    Toast.LENGTH_SHORT).show();
        }

        return isValid;
    }

    // =========================================================================
    // SAVE ITEM TO DATABASE
    // =========================================================================

    private void saveItem() {
        // Extract values from the form
        String category = binding.dropdownCategory.getText().toString().trim();
        String fabricType = binding.dropdownFabric.getText().toString().trim();
        double price = Double.parseDouble(
                binding.editPrice.getText().toString().trim());
        String name = binding.editName.getText() != null ? binding.editName.getText().toString().trim() : "";

        if (isEditMode) {
            // UPDATE existing item — preserve original ID and dateAdded
            WardrobeItem item = new WardrobeItem(
                    category,
                    selectedColorHex,
                    fabricType,
                    price,
                    selectedImagePath,
                    originalDateAdded  // Keep original timestamp
            );
            item.setItemId(editItemId);    // Set the existing ID
            item.setUserId(originalUserId); // Preserve user association
            item.setName(name);
            item.setFavorite(originalIsFavorite);

            Disposable disposable = repository.updateItem(item)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Toast.makeText(this,
                                        category + " updated!",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            throwable -> {
                                Toast.makeText(this,
                                        "Error updating item: " + throwable.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                throwable.printStackTrace();
                            }
                    );
            compositeDisposable.add(disposable);
        } else {
            // INSERT new item
            WardrobeItem item = new WardrobeItem(
                    category,
                    selectedColorHex,
                    fabricType,
                    price,
                    selectedImagePath,
                    System.currentTimeMillis()
            );
            item.setName(name);

            Disposable disposable = repository.insertItem(item)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Toast.makeText(this,
                                        category + " added to your wardrobe!",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            throwable -> {
                                Toast.makeText(this,
                                        "Error saving item: " + throwable.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                throwable.printStackTrace();
                            }
                    );
            compositeDisposable.add(disposable);
        }
    }
}
