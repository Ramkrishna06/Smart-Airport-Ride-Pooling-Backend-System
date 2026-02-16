package com.airport.ridepooling.dto;

import com.airport.ridepooling.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing ride booking details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideResponseDTO {
    
    private Long rideId;
    private Long passengerId;
    private RideStatus status;
    private Double estimatedFare;
    private Double distance;
    private Integer totalPassengers;
    private Integer availableSeats;
    private String message;
    private Boolean isPooled; // Whether this is a shared ride
    private Double savingsPercentage; // Discount from pooling
}
