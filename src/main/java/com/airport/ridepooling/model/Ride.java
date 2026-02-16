package com.airport.ridepooling.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a ride that can pool multiple passengers
 * Uses optimistic locking to handle concurrent seat bookings
 */
@Entity
@Table(name = "rides", indexes = {
    @Index(name = "idx_ride_status", columnList = "status"),
    @Index(name = "idx_ride_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status = RideStatus.PENDING;
    
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Passenger> passengers = new ArrayList<>();
    
    // Capacity constraints
    @Column(nullable = false)
    private Integer maxSeats = 4;
    
    @Column(nullable = false)
    private Integer maxLuggage = 6;
    
    @Column(nullable = false)
    private Integer availableSeats = 4;
    
    @Column(nullable = false)
    private Integer availableLuggage = 6;
    
    // Pricing and distance
    private Double totalDistance; // Total route distance in km
    private Double baseFare; // Base fare before surge/discounts
    private Double finalFare; // Final fare after calculations
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime scheduledTime; // When the ride should start
    
    /**
     * Version field for optimistic locking
     * Critical for preventing race conditions when multiple passengers
     * try to book the last available seat simultaneously
     */
    @Version
    private Long version;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * Check if this ride can accommodate a new passenger
     * @param passenger The passenger to check
     * @return true if there's enough space for seats and luggage
     */
    public boolean canAccommodate(Passenger passenger) {
        return availableSeats >= 1 && 
               availableLuggage >= passenger.getLuggageCount();
    }
    
    /**
     * Add a passenger to this ride
     * Updates available capacity
     * @param passenger The passenger to add
     */
    public void addPassenger(Passenger passenger) {
        if (!canAccommodate(passenger)) {
            throw new IllegalStateException("Cannot accommodate passenger - insufficient capacity");
        }
        
        passengers.add(passenger);
        passenger.setRide(this);
        availableSeats--;
        availableLuggage -= passenger.getLuggageCount();
    }
    
    /**
     * Remove a passenger from this ride
     * Frees up capacity
     * @param passenger The passenger to remove
     */
    public void removePassenger(Passenger passenger) {
        if (passengers.remove(passenger)) {
            passenger.setRide(null);
            availableSeats++;
            availableLuggage += passenger.getLuggageCount();
        }
    }
    
    /**
     * Check if ride is full
     */
    public boolean isFull() {
        return availableSeats == 0;
    }
    
    /**
     * Get current occupancy percentage
     */
    public double getOccupancyRate() {
        return ((maxSeats - availableSeats) / (double) maxSeats) * 100;
    }
    
    /**
     * Get the primary passenger (first to book)
     */
    public Passenger getPrimaryPassenger() {
        return passengers.isEmpty() ? null : passengers.get(0);
    }
}
