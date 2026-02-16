package com.airport.ridepooling.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a passenger requesting a ride
 * Uses optimistic locking (@Version) to handle concurrent bookings
 */
@Entity
@Table(name = "passengers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Passenger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(nullable = false)
    private String name;
    
    @NotNull
    @Column(nullable = false)
    private String phone;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "pickup_lat")),
        @AttributeOverride(name = "longitude", column = @Column(name = "pickup_lon"))
    })
    private Location pickupLocation;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "dropoff_lat")),
        @AttributeOverride(name = "longitude", column = @Column(name = "dropoff_lon"))
    })
    private Location dropoffLocation;
    
    @Column(nullable = false)
    private Integer luggageCount = 1;
    
    @Column(nullable = false)
    private Integer maxDetourMinutes = 15; // Maximum acceptable detour time
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    @JsonBackReference
    private Ride ride;
    
    @Column(nullable = false)
    private LocalDateTime requestedAt;
    
    /**
     * Version field for optimistic locking
     * Prevents concurrent modifications (e.g., two passengers booking the last seat)
     */
    @Version
    private Long version;
    
    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }
    
    /**
     * Calculate the direct distance for this passenger's trip
     */
    public double getDirectDistance() {
        if (pickupLocation == null || dropoffLocation == null) {
            return 0.0;
        }
        return pickupLocation.distanceTo(dropoffLocation);
    }
    
    /**
     * Calculate direct travel time in minutes
     */
    public double getDirectTravelTime() {
        if (pickupLocation == null || dropoffLocation == null) {
            return 0.0;
        }
        return pickupLocation.travelTimeTo(dropoffLocation);
    }
}
