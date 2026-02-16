package com.airport.ridepooling.repository;

import com.airport.ridepooling.model.Ride;
import com.airport.ridepooling.model.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Ride entity
 * Provides database access methods with custom queries
 */
@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    
    /**
     * Find all rides with a specific status
     * Used for matching algorithm to find available rides
     * Time Complexity: O(n) where n = number of rides with status
     */
    List<Ride> findByStatus(RideStatus status);
    
    /**
     * Find rides with status and available seats
     * More efficient query for matching
     */
    @Query("SELECT r FROM Ride r WHERE r.status = :status AND r.availableSeats > 0")
    List<Ride> findAvailableRidesByStatus(RideStatus status);
    
    /**
     * Count rides by status
     * Used for surge pricing calculation
     */
    long countByStatus(RideStatus status);
    
    /**
     * Find recent rides (for analytics/monitoring)
     */
    @Query("SELECT r FROM Ride r WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<Ride> findRecentRidesByStatus(RideStatus status);
}
