package com.airport.ridepooling;

import com.airport.ridepooling.model.Location;
import com.airport.ridepooling.model.Passenger;
import com.airport.ridepooling.model.Ride;
import com.airport.ridepooling.model.RideStatus;
import com.airport.ridepooling.repository.RideRepository;
import com.airport.ridepooling.service.MatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchingService
 */
@ExtendWith(MockitoExtension.class)
public class MatchingServiceTest {
    
    @Mock
    private RideRepository rideRepository;
    
    @InjectMocks
    private MatchingService matchingService;
    
    private Location airportLocation;
    private Location downtownLocation;
    private Location gurgaonLocation;
    
    @BeforeEach
    public void setup() {
        airportLocation = new Location(28.5562, 77.1000);
        downtownLocation = new Location(28.7041, 77.1025);
        gurgaonLocation = new Location(28.4595, 77.0266);
    }
    
    /**
     * Test that matching finds a suitable ride
     */
    @Test
    public void testFindBestMatch_SuccessfulMatch() {
        // Create existing ride
        Ride existingRide = createRide(airportLocation, downtownLocation);
        
        // Create new passenger with similar route
        Passenger newPassenger = createPassenger("Bob", 
            new Location(28.5570, 77.1010), // Near airport
            new Location(28.7050, 77.1030)  // Near downtown
        );
        
        when(rideRepository.findAvailableRidesByStatus(RideStatus.PENDING))
            .thenReturn(List.of(existingRide));
        
        Ride match = matchingService.findBestMatch(newPassenger);
        
        assertNotNull(match);
        assertEquals(existingRide.getId(), match.getId());
    }
    
    /**
     * Test that matching returns null when no suitable ride found
     */
    @Test
    public void testFindBestMatch_NoMatch_DifferentDestination() {
        // Create existing ride going to downtown
        Ride existingRide = createRide(airportLocation, downtownLocation);
        
        // Create new passenger going to completely different location
        Passenger newPassenger = createPassenger("Charlie", 
            airportLocation,
            gurgaonLocation // Different destination
        );
        
        when(rideRepository.findAvailableRidesByStatus(RideStatus.PENDING))
            .thenReturn(List.of(existingRide));
        
        Ride match = matchingService.findBestMatch(newPassenger);
        
        // Should not match due to different destination
        assertNull(match);
    }
    
    /**
     * Test that matching respects capacity constraints
     */
    @Test
    public void testFindBestMatch_NoMatch_InsufficientCapacity() {
        // Create ride with no available seats
        Ride fullRide = createRide(airportLocation, downtownLocation);
        fullRide.setAvailableSeats(0);
        
        Passenger newPassenger = createPassenger("David", 
            new Location(28.5570, 77.1010),
            new Location(28.7050, 77.1030)
        );
        
        when(rideRepository.findAvailableRidesByStatus(RideStatus.PENDING))
            .thenReturn(List.of(fullRide));
        
        Ride match = matchingService.findBestMatch(newPassenger);
        
        assertNull(match);
    }
    
    /**
     * Test that matching respects luggage constraints
     */
    @Test
    public void testFindBestMatch_NoMatch_InsufficientLuggage() {
        Ride ride = createRide(airportLocation, downtownLocation);
        ride.setAvailableLuggage(1); // Only 1 space left
        
        Passenger heavyLuggage = createPassenger("Eve", 
            new Location(28.5570, 77.1010),
            new Location(28.7050, 77.1030)
        );
        heavyLuggage.setLuggageCount(3); // Needs 3 spaces
        
        when(rideRepository.findAvailableRidesByStatus(RideStatus.PENDING))
            .thenReturn(List.of(ride));
        
        Ride match = matchingService.findBestMatch(heavyLuggage);
        
        assertNull(match);
    }
    
    /**
     * Test empty ride list returns null
     */
    @Test
    public void testFindBestMatch_NoActiveRides() {
        Passenger passenger = createPassenger("Frank", airportLocation, downtownLocation);
        
        when(rideRepository.findAvailableRidesByStatus(RideStatus.PENDING))
            .thenReturn(new ArrayList<>());
        
        Ride match = matchingService.findBestMatch(passenger);
        
        assertNull(match);
    }
    
    // Helper methods
    
    private Ride createRide(Location pickup, Location dropoff) {
        Ride ride = new Ride();
        ride.setId(1L);
        ride.setStatus(RideStatus.PENDING);
        ride.setAvailableSeats(3);
        ride.setAvailableLuggage(5);
        
        Passenger passenger = createPassenger("Alice", pickup, dropoff);
        ride.addPassenger(passenger);
        
        return ride;
    }
    
    private Passenger createPassenger(String name, Location pickup, Location dropoff) {
        Passenger passenger = new Passenger();
        passenger.setName(name);
        passenger.setPhone("1234567890");
        passenger.setPickupLocation(pickup);
        passenger.setDropoffLocation(dropoff);
        passenger.setLuggageCount(1);
        passenger.setMaxDetourMinutes(15);
        return passenger;
    }
}
