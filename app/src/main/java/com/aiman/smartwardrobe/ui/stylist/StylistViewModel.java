package com.aiman.smartwardrobe.ui.stylist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.aiman.smartwardrobe.data.entity.WardrobeItem;
import com.aiman.smartwardrobe.data.repository.WardrobeRepository;
import com.aiman.smartwardrobe.data.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ============================================================================
 * StylistViewModel — MVVM ViewModel for the Manual Fit Stylist Screen
 * ============================================================================
 *
 * <p>
 * Manages the selection state of the wardrobe items (Top, Bottom, Shoes)
 * currently loaded onto the canvas. Coordinates fetching filtered list options,
 * logs wear events to Room, fetches current local weather from OpenWeatherMap,
 * and executes the automated Smart Stylist outfit generator algorithm.
 * </p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class StylistViewModel extends AndroidViewModel {

    public enum SlotType {
        HEAD, CHEST, LEGS, FEET
    }

    private final WardrobeRepository repository;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // Selection States
    private final MutableLiveData<WardrobeItem> selectedHead = new MutableLiveData<>(null);
    private final MutableLiveData<WardrobeItem> selectedTop = new MutableLiveData<>(null); // Maps to CHEST
    private final MutableLiveData<WardrobeItem> selectedBottom = new MutableLiveData<>(null); // Maps to LEGS
    private final MutableLiveData<WardrobeItem> selectedShoes = new MutableLiveData<>(null); // Maps to FEET

    // Active Layer State
    private final MutableLiveData<String> activeLayerName = new MutableLiveData<>("None");

    // Weather & Recommendation State
    private final MutableLiveData<Boolean> isWeatherLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> weatherDescription = new MutableLiveData<>("");
    private final MutableLiveData<String> recommendationAlert = new MutableLiveData<>("");

    // Predefined category groups matching strings in arrays.xml
    public static final List<String> CATEGORIES_TOP = Arrays.asList(
            "T-Shirt", "Shirt", "Jacket", "Hoodie", "Sweater");
    public static final List<String> CATEGORIES_BOTTOM = Arrays.asList(
            "Pants", "Jeans", "Shorts", "Skirt", "Dress");
    public static final List<String> CATEGORIES_SHOES = Arrays.asList(
            "Shoes", "Sneakers", "Boots");

    // Lists of items loaded from the database for swipe navigation
    private List<WardrobeItem> headItems = new ArrayList<>();
    private List<WardrobeItem> chestItems = new ArrayList<>();
    private List<WardrobeItem> legsItems = new ArrayList<>();
    private List<WardrobeItem> feetItems = new ArrayList<>();

    // Current index in each category list
    private int headIndex = -1;
    private int chestIndex = -1;
    private int legsIndex = -1;
    private int feetIndex = -1;

    public StylistViewModel(@NonNull Application application) {
        super(application);
        this.repository = new WardrobeRepository(application);

        // Load items reactively from Room to populate our lists
        Disposable dHead = repository.getItemsByCategories(Arrays.asList("Accessories"))
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    this.headItems = list;
                    updateIndexFromSelected(SlotType.HEAD);
                }, Throwable::printStackTrace);

        Disposable dChest = repository.getItemsByCategories(CATEGORIES_TOP)
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    this.chestItems = list;
                    updateIndexFromSelected(SlotType.CHEST);
                }, Throwable::printStackTrace);

        Disposable dLegs = repository.getItemsByCategories(CATEGORIES_BOTTOM)
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    this.legsItems = list;
                    updateIndexFromSelected(SlotType.LEGS);
                }, Throwable::printStackTrace);

        Disposable dFeet = repository.getItemsByCategories(CATEGORIES_SHOES)
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    this.feetItems = list;
                    updateIndexFromSelected(SlotType.FEET);
                }, Throwable::printStackTrace);

        compositeDisposable.addAll(dHead, dChest, dLegs, dFeet);
    }

    private void updateIndexFromSelected(SlotType slot) {
        switch (slot) {
            case HEAD:
                headIndex = findItemIndex(headItems, selectedHead.getValue());
                break;
            case CHEST:
                chestIndex = findItemIndex(chestItems, selectedTop.getValue());
                break;
            case LEGS:
                legsIndex = findItemIndex(legsItems, selectedBottom.getValue());
                break;
            case FEET:
                feetIndex = findItemIndex(feetItems, selectedShoes.getValue());
                break;
        }
    }

    private int findItemIndex(List<WardrobeItem> list, WardrobeItem target) {
        if (target == null || list == null)
            return -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getItemId() == target.getItemId()) {
                return i;
            }
        }
        return -1;
    }

    // --- GETTERS ---

    public LiveData<WardrobeItem> getSelectedHead() {
        return selectedHead;
    }

    public LiveData<WardrobeItem> getSelectedTop() {
        return selectedTop;
    }

    public LiveData<WardrobeItem> getSelectedBottom() {
        return selectedBottom;
    }

    public LiveData<WardrobeItem> getSelectedShoes() {
        return selectedShoes;
    }

    public LiveData<String> getActiveLayerName() {
        return activeLayerName;
    }

    public LiveData<Boolean> getIsWeatherLoading() {
        return isWeatherLoading;
    }

    public LiveData<String> getWeatherDescription() {
        return weatherDescription;
    }

    public LiveData<String> getRecommendationAlert() {
        return recommendationAlert;
    }

    public void setWeatherDescription(String description) {
        weatherDescription.postValue(description);
    }

    // --- GESTURE & SELECTION ACTIONS ---

    public void setActiveLayer(SlotType slot) {
        activeLayerName.setValue(slot.name());
    }

    public void selectFirstItemIfNoneWorn(SlotType slot) {
        setActiveLayer(slot);
        switch (slot) {
            case HEAD:
                if (selectedHead.getValue() != null) {
                    // Already has a selection — clear it (toggle off)
                    selectedHead.setValue(null);
                    headIndex = -1;
                } else if (!headItems.isEmpty()) {
                    headIndex = 0;
                    selectedHead.setValue(headItems.get(0));
                }
                break;
            case CHEST:
                if (selectedTop.getValue() != null) {
                    selectedTop.setValue(null);
                    chestIndex = -1;
                } else if (!chestItems.isEmpty()) {
                    chestIndex = 0;
                    selectedTop.setValue(chestItems.get(0));
                }
                break;
            case LEGS:
                if (selectedBottom.getValue() != null) {
                    selectedBottom.setValue(null);
                    legsIndex = -1;
                } else if (!legsItems.isEmpty()) {
                    legsIndex = 0;
                    selectedBottom.setValue(legsItems.get(0));
                }
                break;
            case FEET:
                if (selectedShoes.getValue() != null) {
                    selectedShoes.setValue(null);
                    feetIndex = -1;
                } else if (!feetItems.isEmpty()) {
                    feetIndex = 0;
                    selectedShoes.setValue(feetItems.get(0));
                }
                break;
        }
    }

    public void nextItem(SlotType slot) {
        setActiveLayer(slot);
        switch (slot) {
            case HEAD:
                if (headItems.isEmpty())
                    return;
                // Cycle: -1 (empty) -> 0 -> 1 -> ... -> size-1 -> -1 (empty)
                headIndex = headIndex + 1;
                if (headIndex >= headItems.size())
                    headIndex = -1;
                selectedHead.setValue(headIndex >= 0 ? headItems.get(headIndex) : null);
                break;
            case CHEST:
                if (chestItems.isEmpty())
                    return;
                chestIndex = chestIndex + 1;
                if (chestIndex >= chestItems.size())
                    chestIndex = -1;
                selectedTop.setValue(chestIndex >= 0 ? chestItems.get(chestIndex) : null);
                break;
            case LEGS:
                if (legsItems.isEmpty())
                    return;
                legsIndex = legsIndex + 1;
                if (legsIndex >= legsItems.size())
                    legsIndex = -1;
                selectedBottom.setValue(legsIndex >= 0 ? legsItems.get(legsIndex) : null);
                break;
            case FEET:
                if (feetItems.isEmpty())
                    return;
                feetIndex = feetIndex + 1;
                if (feetIndex >= feetItems.size())
                    feetIndex = -1;
                selectedShoes.setValue(feetIndex >= 0 ? feetItems.get(feetIndex) : null);
                break;
        }
    }

    public void prevItem(SlotType slot) {
        setActiveLayer(slot);
        switch (slot) {
            case HEAD:
                if (headItems.isEmpty())
                    return;
                // Cycle: -1 (empty) <- 0 <- 1 <- ... <- size-1 <- -1 (empty)
                headIndex = headIndex - 1;
                if (headIndex < -1)
                    headIndex = headItems.size() - 1;
                selectedHead.setValue(headIndex >= 0 ? headItems.get(headIndex) : null);
                break;
            case CHEST:
                if (chestItems.isEmpty())
                    return;
                chestIndex = chestIndex - 1;
                if (chestIndex < -1)
                    chestIndex = chestItems.size() - 1;
                selectedTop.setValue(chestIndex >= 0 ? chestItems.get(chestIndex) : null);
                break;
            case LEGS:
                if (legsItems.isEmpty())
                    return;
                legsIndex = legsIndex - 1;
                if (legsIndex < -1)
                    legsIndex = legsItems.size() - 1;
                selectedBottom.setValue(legsIndex >= 0 ? legsItems.get(legsIndex) : null);
                break;
            case FEET:
                if (feetItems.isEmpty())
                    return;
                feetIndex = feetIndex - 1;
                if (feetIndex < -1)
                    feetIndex = feetItems.size() - 1;
                selectedShoes.setValue(feetIndex >= 0 ? feetItems.get(feetIndex) : null);
                break;
        }
    }

    public void resetCanvas() {
        selectedHead.setValue(null);
        selectedTop.setValue(null);
        selectedBottom.setValue(null);
        selectedShoes.setValue(null);
        recommendationAlert.setValue("");
        activeLayerName.setValue("None");
        headIndex = -1;
        chestIndex = -1;
        legsIndex = -1;
        feetIndex = -1;
    }

    /**
     * Checks if at least one item has been selected on the canvas.
     */
    public boolean hasSelections() {
        return selectedHead.getValue() != null
                || selectedTop.getValue() != null
                || selectedBottom.getValue() != null
                || selectedShoes.getValue() != null;
    }

    /**
     * Fetch wardrobe items belonging to the category group associated with a
     * SlotType.
     * Room's Flowable ensures real-time updates to the Bottom Sheet list if
     * database changes.
     */
    public Flowable<List<WardrobeItem>> getClothingOptions(SlotType slot) {
        List<String> targetCategories;
        switch (slot) {
            case HEAD:
                targetCategories = Arrays.asList("Accessories");
                break;
            case CHEST:
                targetCategories = CATEGORIES_TOP;
                break;
            case LEGS:
                targetCategories = CATEGORIES_BOTTOM;
                break;
            case FEET:
                targetCategories = CATEGORIES_SHOES;
                break;
            default:
                targetCategories = new ArrayList<>();
        }
        return repository.getItemsByCategories(targetCategories);
    }

    /**
     * Logs wear events for all items currently active on the canvas.
     * Merges individual Completable insertions into a single reactive stream.
     *
     * @return Completable signaling when all events are saved.
     */
    public Completable logWearEvent() {
        List<Completable> completables = new ArrayList<>();

        if (selectedHead.getValue() != null) {
            completables.add(repository.logWearEvent(selectedHead.getValue().getItemId()));
        }
        if (selectedTop.getValue() != null) {
            completables.add(repository.logWearEvent(selectedTop.getValue().getItemId()));
        }
        if (selectedBottom.getValue() != null) {
            completables.add(repository.logWearEvent(selectedBottom.getValue().getItemId()));
        }
        if (selectedShoes.getValue() != null) {
            completables.add(repository.logWearEvent(selectedShoes.getValue().getItemId()));
        }

        if (completables.isEmpty()) {
            return Completable.complete();
        }

        // Run insertions concurrently on Schedulers.io()
        return Completable.merge(completables)
                .subscribeOn(Schedulers.io());
    }

    // =========================================================================
    // MODULE 3: AUTOMATED SMART STYLIST METHODS
    // =========================================================================

    /**
     * Fetch current temperature for a city from OpenWeatherMap API using Retrofit.
     *
     * @param city   The name of the city (e.g. "Kuala Lumpur")
     * @param apiKey The OpenWeatherMap API key
     * @return Single emitting the temperature in Celsius
     */
    public Single<Double> fetchTemperature(String city, String apiKey) {
        isWeatherLoading.postValue(true);
        return RetrofitClient.getInstance().getWeatherApiService()
                .getCurrentWeather(city, "metric", apiKey)
                .subscribeOn(Schedulers.io())
                .doOnSuccess(response -> {
                    isWeatherLoading.postValue(false);
                    if (response.getWeather() != null && !response.getWeather().isEmpty()) {
                        weatherDescription.postValue(response.getWeather().get(0).getDescription());
                    } else {
                        weatherDescription.postValue("Clear");
                    }
                })
                .doOnError(throwable -> isWeatherLoading.postValue(false))
                .map(response -> response.getMain().getTemp());
    }

    /**
     * Executes the rule-based outfit generation algorithm.
     * 1. Fetches StylingOntology rules for temperature threshold.
     * 2. Resolves list of weather-appropriate categories.
     * 3. Queries matching database wardrobe items.
     * 4. Excludes items worn in the last 3 days.
     * 5. Partitions options and selects one random item per slot, falling back as
     * needed.
     * 6. Posts results to selected item LiveDatas.
     *
     * @param temperature The temperature in Celsius to filter rules by
     */
    public void generateSmartOutfit(double temperature) {
        // Collect all categories that are candidates for Tops, Bottoms, and Shoes
        List<String> allPossibleCategories = new ArrayList<>();
        allPossibleCategories.addAll(CATEGORIES_TOP);
        allPossibleCategories.addAll(CATEGORIES_BOTTOM);
        allPossibleCategories.addAll(CATEGORIES_SHOES);

        Disposable disposable = Single.zip(
                repository.getRulesForTemperature(temperature),
                repository.getRecentlyWornItemIds(3),
                repository.getItemsByCategoriesSingle(allPossibleCategories),
                (rules, recentlyWornIds, dbItems) -> {
                    // 1. Filter matching rules to find the most specific ones for this temperature
                    // range
                    // to prevent overlapping categories (e.g. Winter rule and Casual Hot rule
                    // unioning at 10°C).
                    List<com.aiman.smartwardrobe.data.entity.StylingOntology> activeRules = new ArrayList<>();
                    com.aiman.smartwardrobe.data.entity.StylingOntology bestCasualRule = null;
                    com.aiman.smartwardrobe.data.entity.StylingOntology bestFormalRule = null;

                    for (com.aiman.smartwardrobe.data.entity.StylingOntology rule : rules) {
                        String code = rule.getDressCode().toLowerCase();
                        if (code.contains("casual") || code.contains("winter") || code.contains("cool")
                                || code.contains("warm") || code.contains("hot")) {
                            if (bestCasualRule == null
                                    || rule.getMaxTemperature() < bestCasualRule.getMaxTemperature()) {
                                bestCasualRule = rule;
                            }
                        } else if (code.contains("formal")) {
                            if (bestFormalRule == null
                                    || rule.getMaxTemperature() < bestFormalRule.getMaxTemperature()) {
                                bestFormalRule = rule;
                            }
                        }
                    }
                    if (bestCasualRule != null) {
                        activeRules.add(bestCasualRule);
                    }
                    if (bestFormalRule != null) {
                        activeRules.add(bestFormalRule);
                    }

                    // Parse union of allowed categories from the filtered active rules
                    List<String> allowedCats = new ArrayList<>();
                    for (com.aiman.smartwardrobe.data.entity.StylingOntology rule : activeRules) {
                        if (rule.getAllowedCategories() != null) {
                            String[] parts = rule.getAllowedCategories().split(",");
                            for (String part : parts) {
                                String trimmed = part.trim();
                                if (!trimmed.isEmpty() && !allowedCats.contains(trimmed)) {
                                    allowedCats.add(trimmed);
                                }
                            }
                        }
                    }

                    // 2. Separate all DB items into Category Groups (Tops, Bottoms, Shoes)
                    List<WardrobeItem> dbTops = new ArrayList<>();
                    List<WardrobeItem> dbBottoms = new ArrayList<>();
                    List<WardrobeItem> dbShoes = new ArrayList<>();

                    for (WardrobeItem item : dbItems) {
                        if (CATEGORIES_TOP.contains(item.getCategory())) {
                            dbTops.add(item);
                        } else if (CATEGORIES_BOTTOM.contains(item.getCategory())) {
                            dbBottoms.add(item);
                        } else if (CATEGORIES_SHOES.contains(item.getCategory())) {
                            dbShoes.add(item);
                        }
                    }

                    // 3. Separate allowed items matching ontology categories
                    List<WardrobeItem> allowedTops = new ArrayList<>();
                    List<WardrobeItem> allowedBottoms = new ArrayList<>();
                    List<WardrobeItem> allowedShoes = new ArrayList<>();

                    for (WardrobeItem item : dbItems) {
                        if (allowedCats.contains(item.getCategory())) {
                            if (CATEGORIES_TOP.contains(item.getCategory())) {
                                allowedTops.add(item);
                            } else if (CATEGORIES_BOTTOM.contains(item.getCategory())) {
                                allowedBottoms.add(item);
                            } else if (CATEGORIES_SHOES.contains(item.getCategory())) {
                                allowedShoes.add(item);
                            }
                        }
                    }

                    // 4. Apply 3-day recency filter to allowed categories
                    List<WardrobeItem> freshAllowedTops = new ArrayList<>();
                    for (WardrobeItem item : allowedTops) {
                        if (!recentlyWornIds.contains(item.getItemId())) {
                            freshAllowedTops.add(item);
                        }
                    }

                    List<WardrobeItem> freshAllowedBottoms = new ArrayList<>();
                    for (WardrobeItem item : allowedBottoms) {
                        if (!recentlyWornIds.contains(item.getItemId())) {
                            freshAllowedBottoms.add(item);
                        }
                    }

                    List<WardrobeItem> freshAllowedShoes = new ArrayList<>();
                    for (WardrobeItem item : allowedShoes) {
                        if (!recentlyWornIds.contains(item.getItemId())) {
                            freshAllowedShoes.add(item);
                        }
                    }

                    // 5. Build Outfit Recommendation
                    java.util.Random random = new java.util.Random();
                    WardrobeItem top = selectRandomItem(freshAllowedTops, allowedTops, dbTops, random);
                    WardrobeItem bottom = selectRandomItem(freshAllowedBottoms, allowedBottoms, dbBottoms, random);
                    WardrobeItem shoes = selectRandomItem(freshAllowedShoes, allowedShoes, dbShoes, random);

                    // Track fallback details for the user notification/alert text
                    StringBuilder sb = new StringBuilder();
                    sb.append("Outfit generated for ").append(String.format(java.util.Locale.US, "%.1f", temperature))
                            .append("°C.\n");
                    sb.append("Allowed Categories: ").append(String.join(", ", allowedCats)).append(".\n\n");

                    appendStatus(sb, "Top", freshAllowedTops, allowedTops, dbTops, top);
                    appendStatus(sb, "Bottom", freshAllowedBottoms, allowedBottoms, dbBottoms, bottom);
                    appendStatus(sb, "Shoes", freshAllowedShoes, allowedShoes, dbShoes, shoes);

                    return new RecommendationResult(top, bottom, shoes, sb.toString());
                })
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            selectedTop.setValue(result.top);
                            selectedBottom.setValue(result.bottom);
                            selectedShoes.setValue(result.shoes);
                            recommendationAlert.setValue(result.alertText);
                        },
                        throwable -> {
                            throwable.printStackTrace();
                            recommendationAlert
                                    .setValue("Failed to generate recommendation: " + throwable.getMessage());
                        });
        compositeDisposable.add(disposable);
    }

    private WardrobeItem selectRandomItem(List<WardrobeItem> fresh, List<WardrobeItem> allowed,
            List<WardrobeItem> allDb, java.util.Random random) {
        if (!fresh.isEmpty()) {
            return fresh.get(random.nextInt(fresh.size()));
        } else if (!allowed.isEmpty()) {
            return allowed.get(random.nextInt(allowed.size()));
        } else if (!allDb.isEmpty()) {
            return allDb.get(random.nextInt(allDb.size()));
        }
        return null;
    }

    private void appendStatus(StringBuilder sb, String slotName, List<WardrobeItem> fresh, List<WardrobeItem> allowed,
            List<WardrobeItem> allDb, WardrobeItem selected) {
        sb.append(slotName).append(": ");
        if (selected == null) {
            sb.append("None available in DB");
        } else {
            String itemName = selected.getFabricType() + " " + selected.getCategory();
            if (fresh.contains(selected)) {
                sb.append(itemName).append(" (Weather-appropriate & Fresh)");
            } else if (allowed.contains(selected)) {
                sb.append(itemName).append(" (Weather-appropriate; recently worn fallback)");
            } else {
                sb.append(itemName).append(" (General database fallback)");
            }
        }
        sb.append(".\n");
    }

    private static class RecommendationResult {
        final WardrobeItem top;
        final WardrobeItem bottom;
        final WardrobeItem shoes;
        final String alertText;

        RecommendationResult(WardrobeItem top, WardrobeItem bottom, WardrobeItem shoes, String alertText) {
            this.top = top;
            this.bottom = bottom;
            this.shoes = shoes;
            this.alertText = alertText;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

    // --- VIEWMODEL FACTORY ---

    public static class Factory implements ViewModelProvider.Factory {
        private final Application application;

        public Factory(Application application) {
            this.application = application;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(StylistViewModel.class)) {
                return (T) new StylistViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
