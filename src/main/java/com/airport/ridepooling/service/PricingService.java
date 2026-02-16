package com.airport.ridepooling.service;

import com.airport.ridepooling.model.Ride;
import com.airport.ridepooling.model.RideStatus;
import com.airport.ridepooling.repository.RideRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for calculating dynamic ride pricing
 * 
 * PRICING FORMULA:
 * base_price = BASE_FARE + (distance × RATE_PER_KM)
 * surge_multiplier = 1 + (active_requests / SURGE_THRESHOLD) × MAX_SURGE_FACTOR
 * sharing_discount = SHARING_DISCOUNT_RATE × (passenger_count - 1)
 * final_price = base_price × surge_multiplier × (1 - sharing_discount)
 */
@Service
@Slf4j
public class PricingService {
    
    @Autowired
    private RideRepository rideRepository;
    
    // Pricing constants
    private static final double BASE_FARE = 5.0; // Base booking fee in $
    private static final double RATE_PER_KM = 2.0; // $ per kilometer
    private static final double SHARING_DISCOUNT_RATE = 0.25; // 25% discount per additional passenger
    private static final double MAX_SURGE_FACTOR = 0.5; // Maximum 1.5x surge
    private static final int SURGE_THRESHOLD = 100; // Number of active rides to trigger max surge
    
    /**
     * Calculate fare for a ride
     * @param distance Distance in kilometers
     * @param passengerCount Number of passengers in the ride
     * @return Calculated fare in dollars
     */
    public double calculateFare(double distance, int passengerCount) {
        // Step 1: Calculate base price
        double basePrice = BASE_FARE + (distance * RATE_PER_KM);
        log.debug("Base price for {} km: ${}", distance, basePrice);
        
        // Step 2: Calculate surge multiplier based on demand
        double surgeMultiplier = calculateSurgeMultiplier();
        log.debug("Surge multiplier: {}", surgeMultiplier);
        
        // Step 3: Calculate sharing discount
        double sharingDiscount = calculateSharingDiscount(passengerCount);
        log.debug("Sharing discount for {} passengers: {}%", 
                 passengerCount, sharingDiscount * 100);
        
        // Step 4: Apply surge and discount
        double finalPrice = basePrice * surgeMultiplier * (1 - sharingDiscount);
        
        // Round to 2 decimal places
        finalPrice = Math.round(finalPrice * 100.0) / 100.0;
        
        log.info("Final price: ${} (base: ${}, surge: {}, discount: {}%)",
                finalPrice, basePrice, surgeMultiplier, sharingDiscount * 100);
        
        return finalPrice;
    }
    
    /**
     * Calculate surge multiplier based on current demand
     * Formula: 1 + (active_rides / threshold) × max_surge_factor
     */
    private double calculateSurgeMultiplier() {
        long activeRides = rideRepository.countByStatus(RideStatus.PENDING);
        
        // Surge increases linearly with demand
        double surgeFactor = Math.min(
            (double) activeRides / SURGE_THRESHOLD, 
            1.0
        ) * MAX_SURGE_FACTOR;
        
        return 1.0 + surgeFactor;
    }
    
    /**
     * Calculate sharing discount based on number of passengers
     * More passengers = higher discount
     */
    private double calculateSharingDiscount(int passengerCount) {
        if (passengerCount <= 1) {
            return 0.0; // No discount for solo rides
        }
        
        // Discount increases with each additional passenger
        // Max discount is 75% (for 4 passengers)
        double discount = SHARING_DISCOUNT_RATE * (passengerCount - 1);
        return Math.min(discount, 0.75); // Cap at 75% discount
    }
    
    /**
     * Calculate savings percentage from pooling
     */
    public double calculateSavingsPercentage(double soloPrice, double pooledPrice) {
        if (soloPrice == 0) {
            return 0.0;
        }
        return ((soloPrice - pooledPrice) / soloPrice) * 100.0;
    }
    
    /**
     * Get current surge information for transparency
     */
    public SurgeInfo getCurrentSurgeInfo() {
        long activeRides = rideRepository.countByStatus(RideStatus.PENDING);
        double surgeMultiplier = calculateSurgeMultiplier();
        boolean isSurging = surgeMultiplier > 1.0;
        
        return new SurgeInfo(activeRides, surgeMultiplier, isSurging);
    }
    
    /**
     * Inner class for surge information
     */
    public record SurgeInfo(long activeRides, double multiplier, boolean isSurging) {}
}
