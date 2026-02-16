package com.airport.ridepooling.repository;

import com.airport.ridepooling.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Passenger entity
 */
@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    
    /**
     * Find passengers by ride ID
     */
    List<Passenger> findByRideId(Long rideId);
    
    /**
     * Find passengers by phone number (for user history)
     */
    List<Passenger> findByPhone(String phone);
}
