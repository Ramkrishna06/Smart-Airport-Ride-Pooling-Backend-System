package com.airport.ridepooling.service;

import com.airport.ridepooling.dto.RideDetailsDTO;
import com.airport.ridepooling.dto.RideRequestDTO;
import com.airport.ridepooling.dto.RideResponseDTO;
import com.airport.ridepooling.exception.ResourceNotFoundException;
import com.airport.ridepooling.model.Passenger;
import com.airport.ridepooling.model.Ride;
import com.airport.ridepooling.model.RideStatus;
import com.airport.ridepooling.repository.PassengerRepository;
import com.airport.ridepooling.repository.RideRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Main service for ride operations
 * Handles concurrent bookings using optimistic locking with retry mechanism
 */
@Service
@Slf4j
public class RideService {
    
    @Autowired
    private PassengerRepository passengerRepository;
    
    @Autowired
    private RideRepository rideRepository;
    
    @Autowired
    private MatchingService matchingService;
    
    @Autowired
    private PricingService pricingService;
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * Request a new ride
     * Handles concurrent bookings with optimistic locking and retry logic
     * 
     * CONCURRENCY STRATEGY:
     * - Uses @Transactional for ACID properties
     * - Optimistic locking prevents double-booking
     * - Automatic retry on lock conflicts
     * 
     * @param request Ride request details
     * @return Response with ride and pricing information
     */
    @Transactional
    public RideResponseDTO requestRide(RideRequestDTO request) {
        return requestRideWithRetry(request, 0);
    }
    
    /**
     * Internal method with retry logic for optimistic lock conflicts
     */
    private RideResponseDTO requestRideWithRetry(RideRequestDTO request, int attemptNumber) {
        try {
            log.info("Processing ride request (attempt {}) for passenger: {}", 
                     attemptNumber + 1, request.getName());
            
            // Step 1: Create passenger entity
            Passenger passenger = createPassengerFromRequest(request);
            
            // Step 2: Try to find a matching ride
            Ride matchedRide = matchingService.findBestMatch(passenger);
            
            if (matchedRide != null) {
                // Step 3a: Add passenger to existing ride (pooling scenario)
                return addPassengerToExistingRide(matchedRide, passenger);
            } else {
                // Step 3b: Create new ride (solo or no match found)
                return createNewRide(passenger);
            }
            
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            // Race condition detected - someone else modified the ride
            log.warn("Optimistic lock conflict on attempt {}: {}", 
                     attemptNumber + 1, e.getMessage());
            
            if (attemptNumber < MAX_RETRY_ATTEMPTS - 1) {
                // Retry the operation
                log.info("Retrying ride request...");
                return requestRideWithRetry(request, attemptNumber + 1);
            } else {
                log.error("Max retry attempts reached. Creating new ride instead.");
                // After max retries, create a new ride
                Passenger passenger = createPassengerFromRequest(request);
                return createNewRide(passenger);
            }
        }
    }
    
    /**
     * Add passenger to an existing ride
     */
    private RideResponseDTO addPassengerToExistingRide(Ride ride, Passenger passenger) {
        log.info("Adding passenger {} to existing ride {}", 
                 passenger.getName(), ride.getId());
        
        // Add passenger to ride (this updates version for optimistic locking)
        ride.addPassenger(passenger);
        ride.setStatus(RideStatus.MATCHED);
        
        // Recalculate pricing with pooling discount
        int passengerCount = ride.getPassengers().size();
        double pooledFare = pricingService.calculateFare(
            passenger.getDirectDistance(), 
            passengerCount
        );
        
        ride.setFinalFare(pooledFare);
        
        // Save (optimistic lock version is automatically checked)
        rideRepository.save(ride);
        passengerRepository.save(passenger);
        
        // Calculate savings
        double soloFare = pricingService.calculateFare(passenger.getDirectDistance(), 1);
        double savings = pricingService.calculateSavingsPercentage(soloFare, pooledFare);
        
        log.info("Successfully added passenger to ride {}. Fare: ${}, Savings: {}%",
                ride.getId(), pooledFare, savings);
        
        return RideResponseDTO.builder()
                .rideId(ride.getId())
                .passengerId(passenger.getId())
                .status(RideStatus.MATCHED)
                .estimatedFare(pooledFare)
                .distance(passenger.getDirectDistance())
                .totalPassengers(passengerCount)
                .availableSeats(ride.getAvailableSeats())
                .isPooled(true)
                .savingsPercentage(savings)
                .message(String.format("Ride matched! You're saving %.1f%% by pooling with %d other passenger(s)",
                        savings, passengerCount - 1))
                .build();
    }
    
    /**
     * Create a new ride for the passenger
     */
    private RideResponseDTO createNewRide(Passenger passenger) {
        log.info("Creating new ride for passenger {}", passenger.getName());
        
        Ride newRide = new Ride();
        newRide.setStatus(RideStatus.PENDING);
        newRide.addPassenger(passenger);
        
        // Calculate pricing
        double distance = passenger.getDirectDistance();
        double fare = pricingService.calculateFare(distance, 1);
        
        newRide.setTotalDistance(distance);
        newRide.setBaseFare(fare);
        newRide.setFinalFare(fare);
        
        rideRepository.save(newRide);
        
        log.info("Created new ride {} with fare ${}", newRide.getId(), fare);
        
        return RideResponseDTO.builder()
                .rideId(newRide.getId())
                .passengerId(passenger.getId())
                .status(RideStatus.PENDING)
                .estimatedFare(fare)
                .distance(distance)
                .totalPassengers(1)
                .availableSeats(newRide.getAvailableSeats())
                .isPooled(false)
                .savingsPercentage(0.0)
                .message("Ride created! Waiting for potential matches to reduce your fare.")
                .build();
    }
    
    /**
     * Get ride details by ID
     */
    @Transactional(readOnly = true)
    public RideDetailsDTO getRideDetails(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));
        
        return RideDetailsDTO.builder()
                .rideId(ride.getId())
                .status(ride.getStatus())
                .passengers(ride.getPassengers().stream()
                        .map(p -> RideDetailsDTO.PassengerSummary.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .pickupLocation(p.getPickupLocation())
                                .dropoffLocation(p.getDropoffLocation())
                                .luggageCount(p.getLuggageCount())
                                .build())
                        .collect(Collectors.toList()))
                .availableSeats(ride.getAvailableSeats())
                .availableLuggage(ride.getAvailableLuggage())
                .totalDistance(ride.getTotalDistance())
                .finalFare(ride.getFinalFare())
                .createdAt(ride.getCreatedAt())
                .build();
    }
    
    /**
     * Cancel a passenger's ride
     * Handles real-time cancellations and rebalances the ride
     */
    @Transactional
    public void cancelRide(Long passengerId) {
        log.info("Processing cancellation for passenger {}", passengerId);
        
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));
        
        Ride ride = passenger.getRide();
        if (ride == null) {
            throw new IllegalStateException("Passenger is not associated with any ride");
        }
        
        // Remove passenger from ride
        ride.removePassenger(passenger);
        
        // If no passengers left, cancel the entire ride
        if (ride.getPassengers().isEmpty()) {
            ride.setStatus(RideStatus.CANCELLED);
            log.info("Ride {} cancelled - no passengers remaining", ride.getId());
        } else {
            // Recalculate pricing for remaining passengers
            int remainingPassengers = ride.getPassengers().size();
            double newFare = pricingService.calculateFare(
                ride.getTotalDistance(), 
                remainingPassengers
            );
            ride.setFinalFare(newFare);
            log.info("Ride {} updated - {} passengers remaining, new fare: ${}",
                    ride.getId(), remainingPassengers, newFare);
        }
        
        passenger.setRide(null);
        passengerRepository.save(passenger);
        rideRepository.save(ride);
        
        log.info("Successfully cancelled passenger {} from ride {}", passengerId, ride.getId());
    }
    
    /**
     * Helper method to create Passenger entity from DTO
     */
    private Passenger createPassengerFromRequest(RideRequestDTO request) {
        Passenger passenger = new Passenger();
        passenger.setName(request.getName());
        passenger.setPhone(request.getPhone());
        passenger.setPickupLocation(request.getPickupLocation());
        passenger.setDropoffLocation(request.getDropoffLocation());
        passenger.setLuggageCount(request.getLuggageCount());
        passenger.setMaxDetourMinutes(request.getMaxDetourMinutes());
        return passenger;
    }
}
