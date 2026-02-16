package com.airport.ridepooling.service;

import com.airport.ridepooling.model.Location;
import com.airport.ridepooling.model.Passenger;
import com.airport.ridepooling.model.Ride;
import com.airport.ridepooling.model.RideStatus;
import com.airport.ridepooling.repository.RideRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for matching passengers to rides
 * Implements greedy matching algorithm with constraint checking
 * 
 * ALGORITHM COMPLEXITY ANALYSIS:
 * - Time Complexity: O(n × m) where n = active rides, m = passengers per ride
 * - Space Complexity: O(1) - no additional data structures
 * - Approach: Greedy algorithm - finds ride with minimum detour cost
 */
@Service
@Slf4j
public class MatchingService {
    
    @Autowired
    private RideRepository rideRepository;
    
    // Configuration constants
    private static final double MAX_SEARCH_RADIUS_KM = 5.0;
    private static final double DETOUR_TOLERANCE_PERCENTAGE = 0.20; // 20% max detour
    private static final double AVERAGE_CITY_SPEED_KMH = 30.0;
    
    /**
     * Find the best matching ride for a new passenger
     * Uses greedy algorithm to minimize total detour cost
     * 
     * @param newPassenger The passenger requesting a ride
     * @return The best matching Ride, or null if no suitable match found
     */
    public Ride findBestMatch(Passenger newPassenger) {
        log.info("Finding match for passenger: {} from {} to {}", 
                 newPassenger.getName(),
                 newPassenger.getPickupLocation(), 
                 newPassenger.getDropoffLocation());
        
        // Step 1: Get all available rides (rides with available seats)
        List<Ride> activeRides = rideRepository.findAvailableRidesByStatus(RideStatus.PENDING);
        log.debug("Found {} active rides to evaluate", activeRides.size());
        
        Ride bestMatch = null;
        double minDetourCost = Double.MAX_VALUE;
        
        // Step 2: Evaluate each ride
        for (Ride ride : activeRides) {
            // Constraint 1: Check capacity (seats and luggage)
            if (!ride.canAccommodate(newPassenger)) {
                log.debug("Ride {} cannot accommodate - insufficient capacity", ride.getId());
                continue;
            }
            
            // Constraint 2: Check if pickup locations are within acceptable radius
            if (!isWithinSearchRadius(ride, newPassenger)) {
                log.debug("Ride {} rejected - outside search radius", ride.getId());
                continue;
            }
            
            // Constraint 3: Calculate detour cost
            double detourCost = calculateDetourCost(ride, newPassenger);
            log.debug("Ride {} detour cost: {} km", ride.getId(), detourCost);
            
            // Constraint 4: Check if detour exceeds tolerance for any passenger
            if (exceedsDetourTolerance(ride, newPassenger, detourCost)) {
                log.debug("Ride {} rejected - exceeds detour tolerance", ride.getId());
                continue;
            }
            
            // Step 3: Select ride with minimum detour (greedy choice)
            if (detourCost < minDetourCost) {
                minDetourCost = detourCost;
                bestMatch = ride;
                log.debug("New best match: Ride {} with detour cost {}", ride.getId(), detourCost);
            }
        }
        
        if (bestMatch != null) {
            log.info("Best match found: Ride {} with detour cost {} km", 
                     bestMatch.getId(), minDetourCost);
        } else {
            log.info("No suitable match found - will create new ride");
        }
        
        return bestMatch;
    }
    
    /**
     * Check if new passenger's pickup is within acceptable radius
     * from existing passengers in the ride
     */
    private boolean isWithinSearchRadius(Ride ride, Passenger newPassenger) {
        if (ride.getPassengers().isEmpty()) {
            return true;
        }
        
        // Check distance from first passenger's pickup location
        Passenger primaryPassenger = ride.getPrimaryPassenger();
        double distance = primaryPassenger.getPickupLocation()
                .distanceTo(newPassenger.getPickupLocation());
        
        return distance <= MAX_SEARCH_RADIUS_KM;
    }
    
    /**
     * Calculate the additional distance added by including new passenger
     * Simplified model: Assumes optimal route ordering
     * 
     * For production: Use actual route optimization (e.g., Google Maps Directions API)
     */
    private double calculateDetourCost(Ride ride, Passenger newPassenger) {
        if (ride.getPassengers().isEmpty()) {
            return 0.0;
        }
        
        Passenger existingPassenger = ride.getPrimaryPassenger();
        
        // Calculate direct distances
        double existingDirectDistance = existingPassenger.getDirectDistance();
        double newDirectDistance = newPassenger.getDirectDistance();
        
        // Simplified pooled route calculation
        // Route: Pickup1 -> Pickup2 -> Dropoff1 -> Dropoff2
        double pooledDistance = calculateSimplifiedPooledRoute(
                existingPassenger.getPickupLocation(),
                existingPassenger.getDropoffLocation(),
                newPassenger.getPickupLocation(),
                newPassenger.getDropoffLocation()
        );
        
        // Detour cost = Additional distance compared to direct routes
        return pooledDistance - existingDirectDistance - newDirectDistance;
    }
    
    /**
     * Simplified pooled route calculation
     * In production, use proper TSP solver or route optimization API
     */
    private double calculateSimplifiedPooledRoute(
            Location pickup1, Location dropoff1,
            Location pickup2, Location dropoff2) {
        
        // Try different route orderings and pick shortest
        // Order 1: P1 -> P2 -> D1 -> D2
        double route1 = pickup1.distanceTo(pickup2) +
                       pickup2.distanceTo(dropoff1) +
                       dropoff1.distanceTo(dropoff2);
        
        // Order 2: P1 -> P2 -> D2 -> D1
        double route2 = pickup1.distanceTo(pickup2) +
                       pickup2.distanceTo(dropoff2) +
                       dropoff2.distanceTo(dropoff1);
        
        return Math.min(route1, route2);
    }
    
    /**
     * Check if adding new passenger exceeds detour tolerance
     * for any existing passenger in the ride
     */
    private boolean exceedsDetourTolerance(Ride ride, Passenger newPassenger, 
                                          double additionalDistance) {
        // Check tolerance for existing passengers
        for (Passenger existingPassenger : ride.getPassengers()) {
            double directDistance = existingPassenger.getDirectDistance();
            
            // Calculate detour time in minutes
            double detourTimeMinutes = (additionalDistance / AVERAGE_CITY_SPEED_KMH) * 60;
            
            if (detourTimeMinutes > existingPassenger.getMaxDetourMinutes()) {
                log.debug("Detour of {} minutes exceeds passenger {}'s tolerance of {} minutes",
                         detourTimeMinutes, existingPassenger.getName(), 
                         existingPassenger.getMaxDetourMinutes());
                return true;
            }
        }
        
        // Check tolerance for new passenger
        double newDirectTime = newPassenger.getDirectTravelTime();
        double detourPercentage = (additionalDistance / newPassenger.getDirectDistance());
        
        double newPassengerDetourMinutes = newDirectTime * detourPercentage;
        if (newPassengerDetourMinutes > newPassenger.getMaxDetourMinutes()) {
            log.debug("Detour of {} minutes exceeds new passenger's tolerance of {} minutes",
                     newPassengerDetourMinutes, newPassenger.getMaxDetourMinutes());
            return true;
        }
        
        return false;
    }
}
