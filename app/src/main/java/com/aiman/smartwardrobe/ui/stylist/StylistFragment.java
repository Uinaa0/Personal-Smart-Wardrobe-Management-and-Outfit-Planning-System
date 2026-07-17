package com.aiman.smartwardrobe.ui.stylist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.aiman.smartwardrobe.data.entity.WardrobeItem;
import com.aiman.smartwardrobe.databinding.FragmentStylistBinding;
import com.aiman.smartwardrobe.ui.auth.LoginActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * StylistFragment — Module 2: The Direct Gesture-Controlled dressing room.
 * Implements a paper-doll mannequin where users can swipe to browse clothing.
 *
 * This version includes a background removal algorithm that detects whether
 * an item has a white or black background and makes it transparent, ensuring
 * all clothing items sit cleanly on the app's light UI background.
 *
 * @author Aiman — Final Year Project
 */
public class StylistFragment extends Fragment {

    private static final String TAG = "StylistFragment";
    private static final String DEFAULT_CITY = "Kuala Lumpur";

    private String getWeatherApiKey() {
        if (getContext() == null) return "";
        return getContext().getSharedPreferences(LoginActivity.PREFS_AUTH, Context.MODE_PRIVATE)
                .getString("weather_api_key", "");
    }

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
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
        binding = null;
    }

    private void setupViewModel() {
        StylistViewModel.Factory factory = new StylistViewModel.Factory(
                requireActivity().getApplication());
        viewModel = new ViewModelProvider(this, factory).get(StylistViewModel.class);
    }

    private void setupSlotListeners() {
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
            if (!viewModel.hasSelections()) return;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirm Outfit")
                    .setMessage("Log this outfit combination to your wear history? This updates your Cost-Per-Wear analytics.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Log Wear", (dialog, which) -> saveWearHistory())
                    .show();
        });

        binding.buttonGenerateOutfit.setOnClickListener(v -> triggerOutfitGeneration());
        binding.buttonShare.setOnClickListener(v -> shareOutfit());
    }



    private void saveWearHistory() {
        Disposable disposable = viewModel.logWearEvent()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Snackbar.make(binding.getRoot(), "Outfit wear logged!", Snackbar.LENGTH_LONG).show();
                            viewModel.resetCanvas();
                        },
                        throwable -> Toast.makeText(requireContext(), "Error saving wear: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
        compositeDisposable.add(disposable);
    }

    private void observeOutfitState() {
        viewModel.getSelectedHead().observe(getViewLifecycleOwner(), item -> updateCanvasLayer(item, binding.imgLayerHead));
        viewModel.getSelectedTop().observe(getViewLifecycleOwner(), item -> updateCanvasLayer(item, binding.imgLayerChest));
        viewModel.getSelectedBottom().observe(getViewLifecycleOwner(), item -> updateCanvasLayer(item, binding.imgLayerLegs));
        viewModel.getSelectedShoes().observe(getViewLifecycleOwner(), item -> updateCanvasLayer(item, binding.imgLayerFeet));

        viewModel.getActiveLayerName().observe(getViewLifecycleOwner(), layer -> updateInfoPanelText());

        viewModel.getIsWeatherLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.buttonGenerateOutfit.setEnabled(!isLoading);
            binding.buttonGenerateOutfit.setText(isLoading ? "Loading..." : "Auto Outfit");
        });

    }

    private void updateInfoPanelText() {
        String activeLayer = viewModel.getActiveLayerName().getValue();
        if (activeLayer == null || activeLayer.equals("None")) {
            binding.textActiveLayerTitle.setText("Active Layer: None");
            binding.textActiveItemDetails.setText("Tap on the mannequin to select an item");
            return;
        }

        binding.textActiveLayerTitle.setText("Active Layer: " + activeLayer);
        WardrobeItem currentItem = null;
        if (activeLayer.equals("HEAD")) currentItem = viewModel.getSelectedHead().getValue();
        else if (activeLayer.equals("CHEST")) currentItem = viewModel.getSelectedTop().getValue();
        else if (activeLayer.equals("LEGS")) currentItem = viewModel.getSelectedBottom().getValue();
        else if (activeLayer.equals("FEET")) currentItem = viewModel.getSelectedShoes().getValue();

        if (currentItem != null) {
            String details = currentItem.getCategory() + " (" + currentItem.getFabricType() + " • $"
                    + String.format(Locale.US, "%.2f", currentItem.getPurchasePrice()) + ")";
            binding.textActiveItemDetails.setText(details);
        } else {
            binding.textActiveItemDetails.setText("Empty • Swipe horizontally to browse options");
        }
    }

    private void updateActionsVisibility() {
        binding.buttonLogWear.setVisibility(viewModel.hasSelections() ? View.VISIBLE : View.GONE);
    }

    private void updateCanvasLayer(@Nullable WardrobeItem item, ImageView layerView) {
        updateInfoPanelText();
        updateActionsVisibility();

        // Guard: Prevent Glide crash if fragment is detached during async image load
        if (!isAdded() || binding == null) return;

        if (item != null && item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(new File(item.getImagePath()))
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource,
                                @Nullable Transition<? super Bitmap> transition) {
                            if (!isAdded() || compositeDisposable == null) {
                                layerView.setImageBitmap(resource);
                                return;
                            }
                            Disposable d = Single.fromCallable(() -> removeBackground(resource))
                                    .subscribeOn(Schedulers.computation())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            processed -> layerView.setImageBitmap(processed),
                                            throwable -> layerView.setImageBitmap(resource));
                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            layerView.setImageDrawable(null);
                        }
                    });
        } else {
            Glide.with(this).clear(layerView);
            layerView.setImageDrawable(null);
        }
    }

    /**
     * Programmatically removes either white OR black backgrounds from a Bitmap.
     * 1. Detects background type by sampling corner pixels.
     * 2. Uses BFS flood fill to identify background regions.
     * 3. Makes background transparent so it blends perfectly into the white app background.
     */
    private static Bitmap removeBackground(Bitmap src) {
        if (src == null) return null;
        int width = src.getWidth();
        int height = src.getHeight();

        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        // --- STEP 1: Detect if background is White or Black ---
        int[] corners = {pixels[0], pixels[width - 1], pixels[pixels.length - width], pixels[pixels.length - 1]};
        int lightCount = 0;
        for (int color : corners) {
            if (isLightColor(color)) lightCount++;
        }
        boolean targetIsWhite = lightCount >= 2;

        boolean[] isBackground = new boolean[width * height];
        ArrayDeque<int[]> queue = new ArrayDeque<>();

        // --- STEP 2: Seed BFS from all border pixels ---
        for (int x = 0; x < width; x++) {
            seedBorder(x, 0, width, pixels, isBackground, queue, targetIsWhite);
            seedBorder(x, height - 1, width, pixels, isBackground, queue, targetIsWhite);
        }
        for (int y = 1; y < height - 1; y++) {
            seedBorder(0, y, width, pixels, isBackground, queue, targetIsWhite);
            seedBorder(width - 1, y, width, pixels, isBackground, queue, targetIsWhite);
        }

        // --- STEP 3: 8-directional BFS flood fill ---
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
                    if (!isBackground[nIdx] && matchesTargetBackground(pixels[nIdx], targetIsWhite)) {
                        isBackground[nIdx] = true;
                        pixels[nIdx] = Color.TRANSPARENT;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }

        // Safety check: if more than 95% is removed, return original
        int bgCount = 0;
        for (boolean bg : isBackground) if (bg) bgCount++;
        if (bgCount > (int) (pixels.length * 0.95)) return src;

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    private static void seedBorder(int x, int y, int width, int[] pixels, boolean[] isBackground, ArrayDeque<int[]> queue, boolean targetIsWhite) {
        int idx = y * width + x;
        if (matchesTargetBackground(pixels[idx], targetIsWhite)) {
            isBackground[idx] = true;
            pixels[idx] = Color.TRANSPARENT;
            queue.add(new int[]{x, y});
        }
    }

    private static boolean matchesTargetBackground(int color, boolean targetIsWhite) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        if (targetIsWhite) {
            return r > 225 && g > 225 && b > 225; // Matches white/light backgrounds
        } else {
            return r < 45 && g < 45 && b < 45; // Matches black/dark backgrounds
        }
    }

    private static boolean isLightColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (r + g + b) / 3 > 128;
    }

    private void triggerOutfitGeneration() {
        String apiKey = getWeatherApiKey();
        if (apiKey.isEmpty()) {
            Log.w(TAG, "OpenWeatherMap API key is not configured. Falling back to weather simulator.");
            showWeatherSimulatorDialog("Weather API Key not configured. Using simulator.");
        } else {
            Disposable disposable = viewModel.fetchTemperature(DEFAULT_CITY, apiKey)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            temp -> {
                                viewModel.generateSmartOutfit(temp);
                                Snackbar.make(binding.getRoot(), "Weather: " + temp + "°C", Snackbar.LENGTH_SHORT).show();
                            },
                            throwable -> {
                                Log.e(TAG, "Weather API call failed", throwable);
                                showWeatherSimulatorDialog("Network fetch failed. Using simulator.");
                            });
            compositeDisposable.add(disposable);
        }
    }

    private void shareOutfit() {
        View canvas = binding.layoutMannequinDoll;
        if (canvas.getWidth() == 0 || canvas.getHeight() == 0) {
            Snackbar.make(binding.getRoot(), "Canvas layout is not ready", Snackbar.LENGTH_SHORT).show();
            return;
        }
        Bitmap bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(bitmap);
        canvas.draw(c);

        try {
            java.io.File cachePath = new java.io.File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs();
            java.io.File file = new java.io.File(cachePath, "outfit.png");
            try (java.io.FileOutputStream stream = new java.io.FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            }

            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, requireContext().getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setType("image/png");
                startActivity(Intent.createChooser(shareIntent, "Share your outfit"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(binding.getRoot(), "Failed to share outfit", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void showWeatherSimulatorDialog(String reason) {
        String[] options = {"Cold (< 15°C)", "Mild (15°C - 25°C)", "Hot (> 25°C)"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Weather Simulator")
                .setItems(options, (dialog, which) -> {
                    double temp = (which == 0) ? 10.0 : (which == 1) ? 20.0 : 32.0;
                    viewModel.setWeatherDescription("Simulated (" + options[which] + ")");
                    viewModel.generateSmartOutfit(temp);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class MannequinTouchListener implements View.OnTouchListener {
        private final StylistViewModel.SlotType slotType;
        private final GestureDetector gestureDetector;

        public MannequinTouchListener(StylistViewModel.SlotType slotType) {
            this.slotType = slotType;
            this.gestureDetector = new GestureDetector(requireContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(@NonNull MotionEvent e) { return true; }
                        @Override
                        public boolean onSingleTapUp(@NonNull MotionEvent e) {
                            viewModel.selectFirstItemIfNoneWorn(slotType);
                            return true;
                        }
                        @Override
                        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float vX, float vY) {
                            if (e1 == null) return false;
                            float dX = e2.getX() - e1.getX();
                            if (Math.abs(dX) > 100) {
                                if (dX > 0) viewModel.nextItem(slotType); else viewModel.prevItem(slotType);
                                return true;
                            }
                            return false;
                        }
                    });
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) viewModel.setActiveLayer(slotType);
            return gestureDetector.onTouchEvent(event);
        }
    }
}
