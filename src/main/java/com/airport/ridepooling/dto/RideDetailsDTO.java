package com.airport.ridepooling.dto;

import com.airport.ridepooling.model.Location;
import com.airport.ridepooling.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed ride information DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideDetailsDTO {
    
    private Long rideId;
    private RideStatus status;
    private List<PassengerSummary> passengers;
    private Integer availableSeats;
    private Integer availableLuggage;
    private Double totalDistance;
    private Double finalFare;
    private LocalDateTime createdAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PassengerSummary {
        private Long id;
        private String name;
        private Location pickupLocation;
        private Location dropoffLocation;
        private Integer luggageCount;
    }
}
