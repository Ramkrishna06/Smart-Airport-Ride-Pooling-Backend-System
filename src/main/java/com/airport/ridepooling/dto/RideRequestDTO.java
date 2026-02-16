package com.airport.ridepooling.dto;

import com.airport.ridepooling.model.Location;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new ride request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestDTO {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Phone number is required")
    private String phone;
    
    @NotNull(message = "Pickup location is required")
    private Location pickupLocation;
    
    @NotNull(message = "Dropoff location is required")
    private Location dropoffLocation;
    
    @Min(value = 0, message = "Luggage count cannot be negative")
    private Integer luggageCount = 1;
    
    @Min(value = 1, message = "Max detour must be at least 1 minute")
    private Integer maxDetourMinutes = 15;
}
