package com.aiman.smartwardrobe.data.entity;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ============================================================================
 * ItemWearStatsTest — Unit Tests for Cost-Per-Wear (CPW) Computations
 * ============================================================================
 *
 * <p>Validates the Cost-Per-Wear calculation logic under various scenarios
 * including normal wears, zero wears, and fractional purchase prices.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class ItemWearStatsTest {

    @Test
    public void testCostPerWearNormalCalculation() {
        ItemWearStats stats = new ItemWearStats();
        stats.setPurchasePrice(150.0);
        stats.setWearCount(5);

        // CPW = 150.0 / 5 = 30.0
        assertEquals(30.0, stats.getCostPerWear(), 0.001);
    }

    @Test
    public void testCostPerWearZeroWears() {
        ItemWearStats stats = new ItemWearStats();
        stats.setPurchasePrice(150.0);
        stats.setWearCount(0);

        // If never worn, fallback is the purchase price (150.0)
        assertEquals(150.0, stats.getCostPerWear(), 0.001);
    }

    @Test
    public void testCostPerWearNegativeWears() {
        ItemWearStats stats = new ItemWearStats();
        stats.setPurchasePrice(150.0);
        stats.setWearCount(-3);

        // If wear count is invalid/negative, fallback is the purchase price (150.0)
        assertEquals(150.0, stats.getCostPerWear(), 0.001);
    }

    @Test
    public void testCostPerWearFractionalPrice() {
        ItemWearStats stats = new ItemWearStats();
        stats.setPurchasePrice(45.50);
        stats.setWearCount(3);

        // CPW = 45.50 / 3 = 15.1666...
        assertEquals(15.1666, stats.getCostPerWear(), 0.001);
    }
}
