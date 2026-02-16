package com.airport.ridepooling.model;

/**
 * Represents the lifecycle status of a ride
 */
public enum RideStatus {
    PENDING,      // Waiting for more passengers to match
    MATCHED,      // Successfully pooled with other passengers
    IN_PROGRESS,  // Driver assigned and ride in progress
    COMPLETED,    // Ride finished successfully
    CANCELLED     // Ride was cancelled
}
