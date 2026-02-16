package com.airport.ridepooling.controller;

import com.airport.ridepooling.dto.RideDetailsDTO;
import com.airport.ridepooling.dto.RideRequestDTO;
import com.airport.ridepooling.dto.RideResponseDTO;
import com.airport.ridepooling.service.PricingService;
import com.airport.ridepooling.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for ride pooling operations
 * Provides APIs for requesting, viewing, and canceling rides
 */
@RestController
@RequestMapping("/api/rides")
@Tag(name = "Ride Management", description = "APIs for managing ride requests and pooling")
@Slf4j
public class RideController {
    
    @Autowired
    private RideService rideService;
    
    @Autowired
    private PricingService pricingService;
    
    /**
     * Request a new ride
     * Automatically matches with existing rides if suitable match found
     * 
     * POST /api/rides/request
     */
    @PostMapping("/request")
    @Operation(summary = "Request a ride", 
               description = "Create a new ride request. System will automatically match with existing rides if possible.")
    public ResponseEntity<RideResponseDTO> requestRide(
            @Valid @RequestBody RideRequestDTO request) {
        
        log.info("Received ride request from {}", request.getName());
        
        RideResponseDTO response = rideService.requestRide(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get ride details by ID
     * 
     * GET /api/rides/{rideId}
     */
    @GetMapping("/{rideId}")
    @Operation(summary = "Get ride details", 
               description = "Retrieve detailed information about a specific ride including all passengers")
    public ResponseEntity<RideDetailsDTO> getRide(
            @Parameter(description = "ID of the ride to retrieve")
            @PathVariable Long rideId) {
        
        log.info("Fetching details for ride {}", rideId);
        
        RideDetailsDTO details = rideService.getRideDetails(rideId);
        
        return ResponseEntity.ok(details);
    }
    
    /**
     * Cancel a passenger's ride
     * 
     * DELETE /api/rides/passenger/{passengerId}
     */
    @DeleteMapping("/passenger/{passengerId}")
    @Operation(summary = "Cancel a ride", 
               description = "Cancel a passenger's ride. If this was the last passenger, the entire ride is cancelled.")
    public ResponseEntity<Map<String, String>> cancelRide(
            @Parameter(description = "ID of the passenger to cancel")
            @PathVariable Long passengerId) {
        
        log.info("Cancellation request for passenger {}", passengerId);
        
        rideService.cancelRide(passengerId);
        
        Map<String, String> response = Map.of(
            "message", "Ride cancelled successfully",
            "passengerId", passengerId.toString()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current surge pricing information
     * 
     * GET /api/rides/pricing/surge
     */
    @GetMapping("/pricing/surge")
    @Operation(summary = "Get surge pricing info", 
               description = "Get current surge multiplier and active ride count")
    public ResponseEntity<PricingService.SurgeInfo> getSurgeInfo() {
        
        PricingService.SurgeInfo surgeInfo = pricingService.getCurrentSurgeInfo();
        
        return ResponseEntity.ok(surgeInfo);
    }
    
    /**
     * Health check endpoint
     * 
     * GET /api/rides/health
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is running")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Ride Pooling Backend",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
