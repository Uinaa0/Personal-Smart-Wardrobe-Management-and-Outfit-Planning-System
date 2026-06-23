package com.aiman.smartwardrobe.ui.stylist;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.aiman.smartwardrobe.R;
import com.aiman.smartwardrobe.data.entity.WardrobeItem;
import com.aiman.smartwardrobe.databinding.FragmentStylistBinding;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * ============================================================================
 * StylistFragment — Module 2: The Direct Gesture-Controlled dressing room
 * ============================================================================
 *
 * <p>Implements the paper-doll dressing room canvas. Renders overlapping transparent
 * PNGs for Head, Chest, Legs, and Feet. Uses GestureDetectorCompat on invisible
 * touch zones to handle single tap (select first item) and horizontal swipes
 * (cycle previous/next items) directly on the mannequin.</p>
 *
 * @author Aiman — Final Year Project
 * @version 2.0
 */
public class StylistFragment extends Fragment {

    private static final String OWM_API_KEY = ""; // Insert OpenWeatherMap API Key here for live weather updates
    private static final String DEFAULT_CITY = "Kuala Lumpur";

    private FragmentStylistBinding binding;
    private StylistViewModel viewModel;
    private CompositeDisposable compositeDisposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStylistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        compositeDisposable = new CompositeDisposable();

        setupViewModel();
        setupSlotListeners();
        setupActionListeners();
        observeOutfitState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.clear();
        binding = null;
    }

    private void setupViewModel() {
        StylistViewModel.Factory factory = new StylistViewModel.Factory(
                requireActivity().getApplication());
        viewModel = new ViewModelProvider(this, factory).get(StylistViewModel.class);
    }

    private void setupSlotListeners() {
        // Attach direct swipe/tap gesture listeners to each of the 4 invisible touch zones
        binding.touchZoneHead.setOnTouchListener(new MannequinTouchListener(StylistViewModel.SlotType.HEAD));
        binding.touchZoneChest.setOnTouchListener(new MannequinTouchListener(StylistViewModel.SlotType.CHEST));
        binding.touchZoneLegs.setOnTouchListener(new MannequinTouchListener(StylistViewModel.SlotType.LEGS));
        binding.touchZoneFeet.setOnTouchListener(new MannequinTouchListener(StylistViewModel.SlotType.FEET));
    }

    private void setupActionListeners() {
        binding.buttonResetCanvas.setOnClickListener(v -> {
            if (viewModel.hasSelections()) {
                viewModel.resetCanvas();
                Snackbar.make(binding.getRoot(), "Canvas reset", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.buttonLogWear.setOnClickListener(v -> {
            if (!viewModel.hasSelections()) {
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirm Outfit")
                    .setMessage("Log this outfit combination to your wear history? This updates your Cost-Per-Wear analytics.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Log Wear", (dialog, which) -> saveWearHistory())
                    .show();
        });

        binding.buttonGenerateOutfit.setOnClickListener(v -> triggerOutfitGeneration());
    }

    private void saveWearHistory() {
        Disposable disposable = viewModel.logWearEvent()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Snackbar.make(binding.getRoot(),
                                    "Outfit wear logged! CPW updated in Analytics.",
                                    Snackbar.LENGTH_LONG).show();
                            viewModel.resetCanvas();
                        },
                        throwable -> {
                            Toast.makeText(requireContext(),
                                    "Error saving wear: " + throwable.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            throwable.printStackTrace();
                        }
                );
        compositeDisposable.add(disposable);
    }

    private void observeOutfitState() {
        // Observe Selected Head Item (Accessories)
        viewModel.getSelectedHead().observe(getViewLifecycleOwner(), item -> {
            updateCanvasLayer(item, binding.imgLayerHead);
            updateInfoPanelText();
            updateActionsVisibility();
        });

        // Observe Selected Chest Item (Tops)
        viewModel.getSelectedTop().observe(getViewLifecycleOwner(), item -> {
            updateCanvasLayer(item, binding.imgLayerChest);
            updateInfoPanelText();
            updateActionsVisibility();
        });

        // Observe Selected Legs Item (Bottoms)
        viewModel.getSelectedBottom().observe(getViewLifecycleOwner(), item -> {
            updateCanvasLayer(item, binding.imgLayerLegs);
            updateInfoPanelText();
            updateActionsVisibility();
        });

        // Observe Selected Feet Item (Shoes)
        viewModel.getSelectedShoes().observe(getViewLifecycleOwner(), item -> {
            updateCanvasLayer(item, binding.imgLayerFeet);
            updateInfoPanelText();
            updateActionsVisibility();
        });

        // Observe Active Layer Name to update details panel header
        viewModel.getActiveLayerName().observe(getViewLifecycleOwner(), layer -> {
            updateInfoPanelText();
        });

        // Observe Weather Loading State
        viewModel.getIsWeatherLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                binding.buttonGenerateOutfit.setEnabled(false);
                binding.buttonGenerateOutfit.setText("Loading...");
            } else {
                binding.buttonGenerateOutfit.setEnabled(true);
                binding.buttonGenerateOutfit.setText("Auto Outfit");
            }
        });

        // Observe Recommendation Alerts & Status Banner
        viewModel.getRecommendationAlert().observe(getViewLifecycleOwner(), alert -> {
            if (alert != null && !alert.isEmpty()) {
                binding.cardRecommendationStatus.setVisibility(View.VISIBLE);
                binding.textRecommendationDetails.setText(alert);

                String desc = viewModel.getWeatherDescription().getValue();
                String weatherText = "Weather: " + (desc != null && !desc.isEmpty() ? desc : "Unknown");
                binding.textWeatherInfo.setText(weatherText);
            } else {
                binding.cardRecommendationStatus.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Updates the text labels in the bottom info card depending on which layer is currently active
     * and what clothing item is loaded onto it.
     */
    private void updateInfoPanelText() {
        String activeLayer = viewModel.getActiveLayerName().getValue();
        if (activeLayer == null || activeLayer.equals("None")) {
            binding.textActiveLayerTitle.setText("Active Layer: None");
            binding.textActiveItemDetails.setText("Tap on the mannequin to select an item");
            return;
        }

        binding.textActiveLayerTitle.setText("Active Layer: " + activeLayer);
        WardrobeItem currentItem = null;

        if (activeLayer.equals("HEAD")) {
            currentItem = viewModel.getSelectedHead().getValue();
        } else if (activeLayer.equals("CHEST")) {
            currentItem = viewModel.getSelectedTop().getValue();
        } else if (activeLayer.equals("LEGS")) {
            currentItem = viewModel.getSelectedBottom().getValue();
        } else if (activeLayer.equals("FEET")) {
            currentItem = viewModel.getSelectedShoes().getValue();
        }

        if (currentItem != null) {
            String details = currentItem.getCategory() + " (" + currentItem.getFabricType() + " • $" + String.format(java.util.Locale.US, "%.2f", currentItem.getPurchasePrice()) + ")";
            binding.textActiveItemDetails.setText(details);
        } else {
            binding.textActiveItemDetails.setText("Empty • Swipe horizontally to browse options");
        }
    }

    private void updateCanvasLayer(@Nullable WardrobeItem item, ImageView layerView) {
        if (item != null && item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(new File(item.getImagePath()))
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                                    Object model, com.bumptech.glide.request.target.Target<android.graphics.Bitmap> target,
                                                    boolean isFirstResource) {
                            layerView.setImageDrawable(null);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.Bitmap resource, Object model,
                                                       com.bumptech.glide.request.target.Target<android.graphics.Bitmap> target,
                                                       com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull android.graphics.Bitmap resource,
                                                    @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                            if (compositeDisposable != null) {
                                Disposable d = io.reactivex.rxjava3.core.Single.fromCallable(() -> removeWhiteBackground(resource))
                                        .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.computation())
                                        .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                                        .subscribe(
                                                processed -> layerView.setImageBitmap(processed),
                                                throwable -> layerView.setImageBitmap(resource)
                                        );
                                compositeDisposable.add(d);
                            } else {
                                layerView.setImageBitmap(resource);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                            layerView.setImageDrawable(null);
                        }
                    });
        } else {
            Glide.with(this).clear(layerView);
            layerView.setImageDrawable(null);
        }
    }

    /**
     * Programmatically removes white/off-white/light-gray backgrounds from a Bitmap.
     * Uses a 3-phase approach for clean, artifact-free results:
     * <ol>
     *   <li>BFS flood fill from border pixels (8-directional) to identify background regions</li>
     *   <li>Clear all identified background pixels to transparent</li>
     *   <li>Edge feathering to smooth the transition between clothing and removed background</li>
     * </ol>
     * <p>Catches standard white backgrounds, off-white tones, and checkerboard
     * transparency patterns that some image editors bake into exported PNGs.</p>
     */
    private static android.graphics.Bitmap removeWhiteBackground(android.graphics.Bitmap src) {
        if (src == null) return null;
        int width = src.getWidth();
        int height = src.getHeight();

        // Batch-read all pixels into an array for 10x faster processing
        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        boolean[] isBackground = new boolean[width * height];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();

        // Phase 1: Seed BFS from all border pixels that match light-background criteria
        for (int x = 0; x < width; x++) {
            seedBorder(x, 0, width, pixels, isBackground, queue);
            seedBorder(x, height - 1, width, pixels, isBackground, queue);
        }
        for (int y = 1; y < height - 1; y++) {
            seedBorder(0, y, width, pixels, isBackground, queue);
            seedBorder(width - 1, y, width, pixels, isBackground, queue);
        }

        // Phase 2: 8-directional BFS flood fill for better diagonal edge coverage
        int[] dxArr = {1, -1, 0, 0, 1, -1, 1, -1};
        int[] dyArr = {0, 0, 1, -1, 1, -1, -1, 1};

        while (!queue.isEmpty()) {
            int[] pt = queue.poll();
            int cx = pt[0], cy = pt[1];

            for (int d = 0; d < 8; d++) {
                int nx = cx + dxArr[d];
                int ny = cy + dyArr[d];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int nIdx = ny * width + nx;
                    if (!isBackground[nIdx] && isLightBackground(pixels[nIdx])) {
                        isBackground[nIdx] = true;
                        pixels[nIdx] = android.graphics.Color.TRANSPARENT;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }

        // Phase 3: Edge feathering — soften boundary between clothing and removed background
        int[] output = pixels.clone();
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int idx = y * width + x;
                if (isBackground[idx]) continue;

                // Count how many of the 8 neighbors are background
                int bgCount = 0;
                for (int d = 0; d < 8; d++) {
                    if (isBackground[(y + dyArr[d]) * width + (x + dxArr[d])]) bgCount++;
                }

                if (bgCount >= 2) {
                    int color = pixels[idx];
                    int a = (color >> 24) & 0xFF;
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    int brightness = (r + g + b) / 3;

                    // For light-colored edge pixels, fade alpha proportionally
                    if (brightness > 170) {
                        float bgRatio = bgCount / 8.0f;
                        float brightFactor = Math.min(1.0f, (brightness - 170f) / 85.0f);
                        int newAlpha = Math.max(0, (int) (a * (1.0f - bgRatio * brightFactor)));
                        output[idx] = (newAlpha << 24) | (r << 16) | (g << 8) | b;
                    }
                }
            }
        }

        android.graphics.Bitmap result = android.graphics.Bitmap.createBitmap(width, height,
                android.graphics.Bitmap.Config.ARGB_8888);
        result.setPixels(output, 0, width, 0, 0, width, height);
        return result;
    }

    /**
     * Determines whether a pixel color should be treated as background.
     * Catches white, near-white, light gray, and checkerboard transparency patterns.
     */
    private static boolean isLightBackground(int color) {
        int a = (color >> 24) & 0xFF;
        if (a < 50) return true;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // White / near-white (all channels above 200)
        if (r > 200 && g > 200 && b > 200) return true;

        // Light achromatic gray — catches checkerboard transparency patterns
        // (standard transparency grid uses #CCCCCC / #FFFFFF alternating)
        int brightness = (r + g + b) / 3;
        int maxCh = Math.max(r, Math.max(g, b));
        int minCh = Math.min(r, Math.min(g, b));
        if (brightness > 180 && (maxCh - minCh) < 30) return true;

        return false;
    }

    /**
     * Seeds a border pixel into the BFS queue if it matches light-background criteria.
     */
    private static void seedBorder(int x, int y, int width,
                                    int[] pixels, boolean[] isBackground,
                                    java.util.ArrayDeque<int[]> queue) {
        int idx = y * width + x;
        if (isBackground[idx]) return;
        if (isLightBackground(pixels[idx])) {
            isBackground[idx] = true;
            int a = (pixels[idx] >> 24) & 0xFF;
            if (a >= 50) {
                pixels[idx] = android.graphics.Color.TRANSPARENT;
            }
            queue.add(new int[]{x, y});
        }
    }

    private void updateActionsVisibility() {
        boolean hasSelected = viewModel.hasSelections();
        binding.buttonLogWear.setVisibility(hasSelected ? View.VISIBLE : View.GONE);
    }

    private void triggerOutfitGeneration() {
        if (OWM_API_KEY.isEmpty()) {
            showWeatherSimulatorDialog("OpenWeatherMap API Key is not configured. Please select a temperature range to simulate.");
        } else {
            Disposable disposable = viewModel.fetchTemperature(DEFAULT_CITY, OWM_API_KEY)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            temp -> {
                                viewModel.generateSmartOutfit(temp);
                                Snackbar.make(binding.getRoot(), "Live weather retrieved: " + String.format(java.util.Locale.US, "%.1f", temp) + "°C", Snackbar.LENGTH_SHORT).show();
                            },
                            throwable -> {
                                showWeatherSimulatorDialog("Network fetch failed: " + throwable.getMessage() + "\nFalling back to weather simulator.");
                            }
                    );
            compositeDisposable.add(disposable);
        }
    }

    private void showWeatherSimulatorDialog(String reason) {
        Toast.makeText(requireContext(), reason, Toast.LENGTH_LONG).show();

        String[] options = {
                "Cold (< 15°C) — Simulate 10°C",
                "Mild (15°C - 25°C) — Simulate 20°C",
                "Hot (> 25°C) — Simulate 32°C"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Weather to Simulate")
                .setItems(options, (dialog, which) -> {
                    double simulatedTemp;
                    String statusDesc;
                    if (which == 0) {
                        simulatedTemp = 10.0;
                        statusDesc = "Simulated Cold Weather (<15°C)";
                    } else if (which == 1) {
                        simulatedTemp = 20.0;
                        statusDesc = "Simulated Mild Weather (15-25°C)";
                    } else {
                        simulatedTemp = 32.0;
                        statusDesc = "Simulated Hot Weather (>25°C)";
                    }
                    viewModel.setWeatherDescription(statusDesc);
                    viewModel.generateSmartOutfit(simulatedTemp);
                    Snackbar.make(binding.getRoot(), "Generating outfit for " + simulatedTemp + "°C...", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Custom OnTouchListener that forwards events to GestureDetectorCompat.
     * Manages active layer highlights and horizontal swipe/tap interactions on mannequin segments.
     */
    private class MannequinTouchListener implements View.OnTouchListener {
        private final StylistViewModel.SlotType slotType;
        private final GestureDetectorCompat gestureDetector;

        public MannequinTouchListener(StylistViewModel.SlotType slotType) {
            this.slotType = slotType;
            this.gestureDetector = new GestureDetectorCompat(requireContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        private static final int SWIPE_THRESHOLD = 100;
                        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                        @Override
                        public boolean onDown(@NonNull MotionEvent e) {
                            return true; // Must return true to detect flings
                        }

                        @Override
                        public boolean onSingleTapUp(@NonNull MotionEvent e) {
                            viewModel.selectFirstItemIfNoneWorn(slotType);
                            return true;
                        }

                        @Override
                        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2,
                                               float velocityX, float velocityY) {
                            if (e1 == null) return false;
                            float diffX = e2.getX() - e1.getX();
                            float diffY = e2.getY() - e1.getY();
                            if (Math.abs(diffX) > Math.abs(diffY)) {
                                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                    if (diffX > 0) {
                                        viewModel.nextItem(slotType);
                                    } else {
                                        viewModel.prevItem(slotType);
                                    }
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                viewModel.setActiveLayer(slotType);
            }
            return gestureDetector.onTouchEvent(event);
        }
    }
}
